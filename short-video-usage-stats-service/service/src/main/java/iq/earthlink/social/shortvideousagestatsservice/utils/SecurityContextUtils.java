package iq.earthlink.social.shortvideousagestatsservice.utils;

import iq.earthlink.social.personservice.person.PersonInfo;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.provider.authentication.OAuth2AuthenticationDetails;
import org.springframework.stereotype.Component;

@Component
public class SecurityContextUtils {

    public PersonInfo getCurrentPersonInfo() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return (PersonInfo) authentication.getPrincipal();
    }

    public String getAuthorizationToken() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return ((OAuth2AuthenticationDetails) authentication.getDetails()).getTokenValue();
    }
}
