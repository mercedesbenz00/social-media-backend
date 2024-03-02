package iq.earthlink.social.postservice.group.member;

import iq.earthlink.social.postservice.group.member.dto.GroupMemberDTO;
import iq.earthlink.social.postservice.group.member.dto.GroupMemberEventDTO;
import iq.earthlink.social.postservice.group.member.enumeration.Permission;

import java.util.List;
import java.util.Set;

public interface GroupMemberManager {
    void saveGroupMember(GroupMemberEventDTO groupMemberEventDTO);
    GroupMemberDTO getGroupMember(Long groupId, Long personId);
    List<GroupMemberDTO> getGroupMembersByPermissions(Long groupId, List<Permission> permissions);
    List<GroupMemberDTO> getUserGroupMembershipsByPermissions(Long personId, List<Permission> permissions);
    void addPermission(GroupMemberEventDTO groupMemberEventDTO);
    void deletePermissions(GroupMemberEventDTO groupMemberEventDTO);
    void deleteGroupMember(GroupMemberEventDTO groupMemberEventDTO);
    Long getCount();
    void saveAllGroupMembers(List<GroupMemberEventDTO> groupMemberEventDTOS);
    Set<Long> getAllMemberIdsByGroupId(Long groupId);
}
