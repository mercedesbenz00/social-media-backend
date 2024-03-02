package iq.earthlink.social.postservice.util;

import iq.earthlink.social.exception.ForbiddenException;
import iq.earthlink.social.postservice.group.member.GroupMemberManager;
import iq.earthlink.social.postservice.group.member.dto.GroupMemberDTO;
import iq.earthlink.social.postservice.group.member.enumeration.Permission;
import iq.earthlink.social.postservice.person.dto.PersonDTO;
import lombok.NoArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.List;
import java.util.Objects;

import static iq.earthlink.social.common.util.Preconditions.checkNotNull;

@Component
@NoArgsConstructor
public class PermissionUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(PermissionUtil.class);
    private GroupMemberManager groupMemberManager;

    @Autowired
    public PermissionUtil(GroupMemberManager groupMemberManager) {
        this.groupMemberManager = groupMemberManager;
    }

    public List<Long> getModeratedGroups(Long personId) {
        checkNotNull(personId, "error.check.not.null", "personId");
        var groupMemberShips = groupMemberManager
                .getUserGroupMembershipsByPermissions(personId, List.of(Permission.ADMIN, Permission.MODERATOR));
        return groupMemberShips.stream().map(GroupMemberDTO::getGroupId).toList();
    }

    public void checkGroupPermissions(PersonDTO person, Long groupId) {
        if (!hasGroupPermissions(person, groupId)) {
            throw new ForbiddenException("error.operation.not.permitted");
        }
    }

    public boolean hasGroupPermissions(PersonDTO person, Long groupId) {
        if (Objects.nonNull(groupId) && !person.isAdmin()) {
            return isGroupAdminOrModerator(person.getPersonId(), groupId);
        }
        return person.isAdmin();
    }

    public boolean isGroupAdminOrModerator(Long personId, Long groupId) {
        try {
            return isGroupAdminOrModerator(groupMemberManager.getGroupMember(groupId, personId));
        } catch (Exception ex) {
            LOGGER.warn("Failed get membership info for person: {} and group: {}",
                    personId, groupId, ex);
            return false;
        }
    }

    public boolean isGroupAdminOrModerator(GroupMemberDTO groupMemberDTO) {
        EnumSet<Permission> permissions = EnumSet.of(Permission.ADMIN, Permission.MODERATOR);
        return groupMemberDTO.getPermissions().stream().anyMatch(permissions::contains);
    }

    public boolean isGroupMember(PersonDTO personInfo, Long groupId) {
        try {
            return groupMemberManager.getGroupMember(groupId, personInfo.getPersonId()) != null;
        } catch (Exception ex) {
            LOGGER.error("Failed retrieve membership status for person: {} in group: {}",
                    personInfo.getPersonId(), groupId, ex);
            return false;
        }
    }
}
