package iq.earthlink.social.postservice.group.member;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import iq.earthlink.social.exception.NotFoundException;
import iq.earthlink.social.postservice.group.member.dto.GroupMemberDTO;
import iq.earthlink.social.postservice.group.member.dto.GroupMemberEventDTO;
import iq.earthlink.social.postservice.group.member.enumeration.Permission;
import iq.earthlink.social.postservice.group.member.model.GroupMember;
import iq.earthlink.social.postservice.group.member.repository.GroupMemberRepository;
import iq.earthlink.social.postservice.group.model.UserGroup;
import iq.earthlink.social.postservice.group.repository.GroupRepository;
import iq.earthlink.social.postservice.person.model.Person;
import iq.earthlink.social.postservice.person.repository.PersonRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.*;

@Service
@RequiredArgsConstructor
public class DefaultGroupMemberManager implements GroupMemberManager {
    private final GroupMemberRepository repository;
    private final GroupRepository groupRepository;
    private final PersonRepository personRepository;

    @Override
    public void saveGroupMember(GroupMemberEventDTO groupMemberEventDTO) {
        var groupMember = repository.getByGroupIdAndPersonId(groupMemberEventDTO.getGroupId(), groupMemberEventDTO.getPersonId());
        if (groupMember.isEmpty()) {
            var group = groupRepository.getByGroupId(groupMemberEventDTO.getGroupId());
            var person = personRepository.getPersonByPersonId(groupMemberEventDTO.getPersonId());
            if (group.isPresent() && person.isPresent()) {
                var newGroupMember = GroupMember
                        .builder()
                        .userGroup(group.get())
                        .person(person.get())
                        .permissions(groupMemberEventDTO.getPermissions() == null ? List.of() : groupMemberEventDTO.getPermissions())
                        .createdAt(groupMemberEventDTO.getCreatedAt())
                        .build();
                repository.save(newGroupMember);
            } else {
                throw new NotFoundException("Couldn't add user group member as group not found for group id: {}", groupMemberEventDTO.getGroupId());
            }
        } else if (!CollectionUtils.isEmpty(groupMemberEventDTO.getPermissions())) {
            var entity = groupMember.get();
            entity.setPermissions(groupMemberEventDTO.getPermissions());
            repository.save(entity);
        }
    }

    @Override
    public GroupMemberDTO getGroupMember(Long groupId, Long personId) {
        var groupMember = repository.getByGroupIdAndPersonId(groupId, personId);
        if (groupMember.isPresent()) {
            var groupMemberEntity = groupMember.get();
            var userGroup = groupMemberEntity.getUserGroup();
            return GroupMemberDTO
                    .builder()
                    .groupId(userGroup.getGroupId())
                    .personId(personId)
                    .createdAt(groupMemberEntity.getCreatedAt())
                    .permissions(groupMemberEntity.getPermissions())
                    .build();
        }
        return null;
    }

    @Override
    public List<GroupMemberDTO> getGroupMembersByPermissions(Long groupId, List<Permission> permissions) {
        JsonArray permissionJson = new JsonArray();
        permissions.forEach(permission -> permissionJson.add(permission.name()));
        var groupMembers = repository.findByGroupIdAndPermissions(groupId, permissionJson.toString());
        return groupMembers.stream()
                .map(groupMember -> GroupMemberDTO
                        .builder()
                        .personId(groupMember.getPerson().getPersonId())
                        .groupId(groupMember.getUserGroup().getGroupId())
                        .permissions(groupMember.getPermissions())
                        .createdAt(groupMember.getCreatedAt())
                        .build())
                .toList();
    }

    @Override
    public List<GroupMemberDTO> getUserGroupMembershipsByPermissions(Long personId, List<Permission> permissions) {
        JsonArray permissionJson = new JsonArray();
        permissions.forEach(permission -> permissionJson.add(permission.name()));
        var groupMemberShips = repository.findByPersonIdAndPermissions(personId, permissionJson.toString());
        if (CollectionUtils.isEmpty(groupMemberShips)) {
            return Collections.emptyList();
        }
        return groupMemberShips.stream()
                .map(groupMember -> GroupMemberDTO
                        .builder()
                        .personId(personId)
                        .groupId(groupMember.getUserGroup().getGroupId())
                        .permissions(groupMember.getPermissions())
                        .createdAt(groupMember.getCreatedAt())
                        .build())
                .toList();
    }

    @Override
    @Transactional
    public void addPermission(GroupMemberEventDTO groupMemberEventDTO) {
        var groupMember = repository.getByGroupIdAndPersonId(groupMemberEventDTO.getGroupId(), groupMemberEventDTO.getPersonId());
        if (groupMember.isPresent()) {
            var groupMemberEntity = groupMember.get();
            var permissions = groupMemberEntity.getPermissions();
            if (!CollectionUtils.isEmpty(permissions)) {
                if (!permissions.contains(groupMemberEventDTO.getPermissions().get(0))) {
                    permissions.add(groupMemberEventDTO.getPermissions().get(0));
                }
            } else {
                permissions = groupMemberEventDTO.getPermissions();
            }
            Gson gson = new Gson();
            repository.setGroupMemberPermissions(gson.toJson(permissions), groupMemberEntity.getId());
        }
    }

    @Override
    @Transactional
    public void deletePermissions(GroupMemberEventDTO groupMemberEventDTO) {
        var groupMember = repository.getByGroupIdAndPersonId(groupMemberEventDTO.getGroupId(), groupMemberEventDTO.getPersonId());
        if (groupMember.isPresent()) {
            var groupMemberEntity = groupMember.get();
            var permissions = groupMemberEntity.getPermissions();
            if (!CollectionUtils.isEmpty(permissions) && permissions.containsAll(groupMemberEventDTO.getPermissions())) {
                permissions.removeAll(groupMemberEventDTO.getPermissions());
                Gson gson = new Gson();
                repository.setGroupMemberPermissions(gson.toJson(permissions), groupMemberEntity.getId());
            }
        }
    }

    public void deleteGroupMember(GroupMemberEventDTO groupMemberEventDTO) {
        var groupMember = repository.getByGroupIdAndPersonId(groupMemberEventDTO.getGroupId(), groupMemberEventDTO.getPersonId());
        groupMember.ifPresent(repository::delete);
    }

    @Override
    public Long getCount() {
        return repository.count();
    }

    @Override
    public void saveAllGroupMembers(List<GroupMemberEventDTO> groupMemberEventDTOS) {
        List<Long> groupIds = new ArrayList<>();
        List<Long> personIds = new ArrayList<>();
        List<GroupMember> groupMembers = new ArrayList<>();
        groupMemberEventDTOS.forEach(groupMemberEventDTO -> {
            groupIds.add(groupMemberEventDTO.getGroupId());
            personIds.add(groupMemberEventDTO.getPersonId());

        });
        List<UserGroup> userGroupList = groupRepository.getByGroupIdIn(groupIds);
        List<Person> personList = personRepository.getPersonByPersonIdIn(personIds);

        groupMemberEventDTOS.forEach(groupMemberEventDTO -> {
            var userGroup = userGroupList.stream().filter(group -> Objects.equals(group.getGroupId(), groupMemberEventDTO.getGroupId())).findFirst();
            var groupMember = personList.stream().filter(person -> Objects.equals(person.getPersonId(), groupMemberEventDTO.getPersonId())).findFirst();
            if (userGroup.isPresent() && groupMember.isPresent()) {
                groupMembers.add(GroupMember
                        .builder()
                        .userGroup(userGroup.get())
                        .person(groupMember.get())
                        .permissions(groupMemberEventDTO.getPermissions())
                        .build());
            }
        });

        if (!CollectionUtils.isEmpty(groupMembers)) {
            repository.saveAll(groupMembers);
        }
    }

    @Override
    public Set<Long> getAllMemberIdsByGroupId(Long groupId) {
        return repository.getAllMemberIdsByGroupId(groupId);
    }
}
