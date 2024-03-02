package iq.earthlink.social.groupservice.group.permission;

import feign.FeignException;
import iq.earthlink.social.classes.data.dto.PersonData;
import iq.earthlink.social.common.audit.AuditMarker;
import iq.earthlink.social.common.audit.AuditMessage;
import iq.earthlink.social.common.audit.EventAction;
import iq.earthlink.social.common.util.CommonConstants;
import iq.earthlink.social.exception.BadRequestException;
import iq.earthlink.social.exception.ForbiddenException;
import iq.earthlink.social.exception.NotFoundException;
import iq.earthlink.social.exception.NotUniqueException;
import iq.earthlink.social.groupservice.event.KafkaProducerService;
import iq.earthlink.social.groupservice.group.*;
import iq.earthlink.social.groupservice.group.dto.GroupMemberEventDTO;
import iq.earthlink.social.groupservice.group.member.GroupMemberRepository;
import iq.earthlink.social.groupservice.group.permission.repository.GroupPermissionRepository;
import iq.earthlink.social.groupservice.group.rest.JsonGroupPermission;
import iq.earthlink.social.groupservice.group.rest.JsonUserGroupStats;
import iq.earthlink.social.groupservice.group.rest.UserGroupPermissionDto;
import iq.earthlink.social.groupservice.group.rest.enumeration.AccessType;
import iq.earthlink.social.groupservice.person.PersonManager;
import iq.earthlink.social.groupservice.person.dto.PersonDTO;
import iq.earthlink.social.postservice.post.rest.GroupStatsDTO;
import iq.earthlink.social.postservice.rest.PostRestService;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dozer.Mapper;
import org.jetbrains.annotations.NotNull;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import javax.annotation.Nonnull;
import java.util.*;

@Service
@RequiredArgsConstructor
public class DefaultGroupPermissionManager implements GroupPermissionManager {

    private static final Logger LOGGER = LogManager.getLogger(DefaultGroupPermissionManager.class);

    private final GroupPermissionRepository repository;
    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final KafkaProducerService kafkaProducerService;
    private final GroupManagerUtils groupManagerUtils;
    private final PostRestService postRestService;
    private final PersonManager personManager;
    private final Mapper mapper;

    @Override
    public JsonGroupPermission setPermission(
            @Nonnull Long personId,
            @Nonnull Boolean isAdmin,
            @Nonnull UserGroup group,
            @Nonnull GroupPermissionData data) {

        // only global admin or group admin allowed changing permissions
        if (!isAdmin(personId, isAdmin, group)) {
            throw new ForbiddenException("error.operation.not.permitted");
        }

        LOGGER.debug("Adding group permission in group: {} with data: {}. Initiator person: {}",
                group.getId(), data, personId);

        PersonDTO person = personManager.getPersonByPersonId(data.getPersonId());
        GroupPermission permission = setPermissionInternal(personId, group, data);
        JsonGroupPermission jsonGroupPermission = mapper.map(permission, JsonGroupPermission.class);
        jsonGroupPermission.setPerson(PersonData
                .builder()
                .id(person.getPersonId())
                .displayName(person.getDisplayName())
                .avatar(person.getAvatar())
                .build());

        return jsonGroupPermission;
    }

    @Override
    public GroupPermission setPermissionInternal(@Nonnull Long personId, @Nonnull UserGroup group, @Nonnull GroupPermissionData data) {
        if (isMember(data.getPersonId(), group.getId())) {
            if (hasPermission(data.getPersonId(), group.getId(), data.getPermission())) {
                throw new NotUniqueException("error.permission.already.exists", data.getPermission().name());
            }
            GroupPermission p = new GroupPermission();
            p.setAuthorId(personId);
            p.setUserGroup(group);
            p.setPersonId(data.getPersonId());
            p.setPermission(data.getPermission());

            try {
                p = repository.save(p);
                LOGGER.info("Created new group permission: {}", p);

                if (GroupMemberStatus.ADMIN.equals(data.getPermission())) {
                    String logMessage = String.format("User with id=\"%s\" has been granted admin permission to the group \"%s\"", data.getPersonId(), group.getName());
                    LOGGER.info(AuditMarker.getMarker(), AuditMessage.create(EventAction.ASSIGN_GROUP_ADMIN, logMessage, personId, p.getId()));
                }
                kafkaProducerService.sendMessageOnFailureThrowError(CommonConstants.GROUP_MEMBER_ADD_PERMISSION, GroupMemberEventDTO
                        .builder()
                        .permissions(List.of(data.getPermission()))
                        .groupId(group.getId())
                        .personId(data.getPersonId())
                        .build());
                return p;
            } catch (DataIntegrityViolationException ex) {
                throw new NotUniqueException("error.permission.already.exists", data.getPermission().name(), ex);
            }
        } else {
            throw new BadRequestException("error.not.member.of.group");
        }
    }

    @Override
    @Transactional
    public void removePermission(@Nonnull Long personId, @Nonnull Boolean isAdmin, @Nonnull Long permissionId) {
        GroupPermission permission = getPermission(permissionId);
        if (isAdmin(personId, isAdmin, permission.getUserGroup())) {
            repository.delete(permission);
            LOGGER.info("Group permission: {} deleted by: {}", permission.getId(), personId);
            String logMessage = String.format("\"Group permission: \"%s\" deleted by: \"%s\"", permission.getId(), personId);
            LOGGER.info(AuditMarker.getMarker(), AuditMessage.create(EventAction.REMOVE_USER_FROM_GROUP, logMessage, personId,
                    permission.getUserGroup().getId()));

            kafkaProducerService.sendMessageOnFailureThrowError(CommonConstants.GROUP_MEMBER_DELETE_PERMISSION, GroupMemberEventDTO
                    .builder()
                    .permissions(List.of(permission.getPermission()))
                    .groupId(permission.getUserGroup().getId())
                    .personId(permission.getPersonId())
                    .build());
        } else {
            throw new ForbiddenException("error.operation.not.permitted");
        }
    }

    @Override
    public void removeAllGroupPermissionsInternal(@Nonnull Long groupId) {
        repository.deleteByUserGroupId(groupId);
    }

    @Override
    @Transactional
    public void removeUserGroupPermissionsInternal(@Nonnull Long memberId, @Nonnull Long groupId) {
        List<GroupPermission> groupPermissions = findPermissionsInternal(memberId, groupId);
        if (!groupPermissions.isEmpty()) {
            repository.deleteAll(groupPermissions);
        }
    }

    @Override
    public boolean hasPermission(Long personId, Long groupId, GroupMemberStatus permission) {
        return repository.existsPermission(personId, groupId, permission);
    }

    @Override
    public Page<JsonGroupPermission> findPermissions(@NotNull PermissionSearchCriteria criteria, @NotNull Long personId,
                                                     boolean isAdmin, @NotNull String authorizationHeader, Pageable page) {
        List<GroupPermission> currentUserPermissions = repository.findByPersonIdAndGroupIds(personId, criteria.getGroupIds());
        var allPermissions = currentUserPermissions.stream().map(GroupPermission::getPermission).toList();

        if (!(Objects.equals(personId, criteria.getPersonId()) || isAdmin || (!allPermissions.isEmpty() && (allPermissions.contains(GroupMemberStatus.ADMIN) || allPermissions.contains(GroupMemberStatus.MODERATOR)))))
            throw new ForbiddenException("error.person.not.authorized");

        updateQuery(criteria);
        Page<GroupPermission> groupPermissions = page.getSort().isSorted() ? repository.findPermissions(criteria, page)
                : repository.findPermissionsOrderedBySimilarity(criteria, page);

        return groupPermissions
                .map(p -> {
                    JsonGroupPermission permission = mapper.map(p, JsonGroupPermission.class);
                    UserGroupPermissionDto group = permission.getUserGroup();
                    JsonUserGroupStats stats = group.getStats();
                    if (isAdmin) {
                        try {
                            GroupStatsDTO groupStats = postRestService.getGroupStats(authorizationHeader, group.getId());
                            stats.setPendingPostsCount(groupStats.getPendingPostsCount());
                        } catch (FeignException ex) {
                            LOGGER.error("Could not fetch pending posts from post service: {}", ex.getMessage());
                        }
                    }
                    stats.setPendingJoinRequests(groupManagerUtils.getPendingJoinRequests(group.getId()));
                    PersonDTO person = personManager.getPersonByPersonId(permission.getPersonId());
                    permission.setPerson(PersonData
                            .builder()
                            .id(person.getPersonId())
                            .displayName(person.getDisplayName())
                            .avatar(person.getAvatar())
                            .build());
                    return permission;
                });
    }

    @Override
    public List<GroupPermission> findPermissionsInternal(@NotNull PermissionSearchCriteria criteria) {
        return repository.findPermissionsInternal(criteria);
    }

    @Override
    public List<GroupPermission> findPermissionsInternal(Long personId, Long groupId) {
        return repository.findByPersonIdAndGroupId(personId, groupId);
    }

    @Override
    public List<Long> getAccessibleGroups(@Nonnull Long personId, @Nonnull Boolean isAdmin, List<Long> groupIds) {
        Set<Long> accessibleGroupIds = new HashSet<>();

        List<UserGroup> publicGroups = CollectionUtils.isEmpty(groupIds) ? groupRepository.findByAccessType(AccessType.PUBLIC)
                : groupRepository.findByAccessTypeAndIdIn(AccessType.PUBLIC, groupIds);

        List<UserGroup> privateGroups = CollectionUtils.isEmpty(groupIds) ? groupRepository.findByAccessType(AccessType.PRIVATE)
                : groupRepository.findByAccessTypeAndIdIn(AccessType.PRIVATE, groupIds);

        accessibleGroupIds.addAll(publicGroups.stream().map(UserGroup::getId).toList());
        accessibleGroupIds.addAll(getAllowedPrivateGroupIds(personId, isAdmin, privateGroups));

        return new ArrayList<>(accessibleGroupIds);
    }

    private List<Long> getAllowedPrivateGroupIds(@Nonnull Long personId, boolean isAdmin, List<UserGroup> privateGroups) {
        List<Long> privateGroupIds = privateGroups.stream().map(UserGroup::getId).toList();
        List<Long> allowedGroupIds = isAdmin ? privateGroupIds : new ArrayList<>();

        if (!isAdmin && !privateGroupIds.isEmpty()) {
            // Find if person is allowed to access private groups:
            List<GroupPermission> groupPermissions = repository.findByPersonIdAndGroupIds(personId, privateGroupIds);
            if (!groupPermissions.isEmpty()) {
                allowedGroupIds = groupPermissions.stream().map(gp -> gp.getUserGroup().getId()).toList();
            }
        }
        return allowedGroupIds;
    }

    private GroupPermission getPermission(Long permissionId) {
        return repository.findById(permissionId)
                .orElseThrow(() -> new NotFoundException("error.not.found.permission", permissionId));
    }

    private boolean isAdmin(Long personId, Boolean isAdmin, UserGroup group) {
        return isAdmin || isGroupAdmin(personId, group);
    }

    private boolean isGroupAdmin(Long personId, UserGroup group) {
        try {
            return this.hasPermission(personId, group.getId(), GroupMemberStatus.ADMIN);
        } catch (NotFoundException ex) {
            return false;
        }
    }

    private boolean isMember(Long personId, Long groupId) {
        return groupMemberRepository.findActiveMember(groupId, personId) != null;
    }

    private void updateQuery(@Nonnull PermissionSearchCriteria criteria) {
        String query = criteria.getQuery();
        if (Objects.nonNull(query)) {
            criteria.setQuery("%" + query.toLowerCase().trim().replaceAll("\\s+", "%") + "%");
        } else {
            criteria.setQuery("%");
        }
    }
}
