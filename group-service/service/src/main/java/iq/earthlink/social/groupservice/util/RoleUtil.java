package iq.earthlink.social.groupservice.util;

import iq.earthlink.social.groupservice.group.GroupMemberStatus;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
public class RoleUtil {
    public boolean isAdmin(String[] roles) {
        List<String> rolesList = Arrays.asList(roles);
        return rolesList.contains(GroupMemberStatus.ADMIN.name());
    }

    public boolean isModerator(String[] roles) {
        List<String> rolesList = Arrays.asList(roles);
        return rolesList.contains(GroupMemberStatus.MODERATOR.name());
    }
}
