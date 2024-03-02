package iq.earthlink.social.postservice.util;

import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
public class RoleUtil {
    public boolean isAdmin(String[] roles) {
        List<String> rolesList = Arrays.asList(roles);
        return rolesList.contains("ADMIN");
    }
}
