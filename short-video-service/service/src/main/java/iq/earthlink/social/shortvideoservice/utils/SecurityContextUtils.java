package iq.earthlink.social.shortvideoservice.utils;

import iq.earthlink.social.personservice.person.PersonInfo;
import iq.earthlink.social.personservice.rest.PersonRestService;
import iq.earthlink.social.security.CustomAuthenticationDetails;
import iq.earthlink.social.security.SecurityProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class SecurityContextUtils {
    private final PersonRestService personRestService;
    private final SecurityProvider securityProvider;

    public SecurityContextUtils(PersonRestService personRestService, SecurityProvider securityProvider) {
        this.personRestService = personRestService;
        this.securityProvider = securityProvider;
    }

    public PersonInfo getCurrentPersonInfo() {
        String authorizationToken = getAuthorizationToken();
        return personRestService.getPersonProfile(authorizationToken);
    }

    public Long getCurrentPersonId() {
        String authorizationToken = getAuthorizationToken();
        return securityProvider.getPersonIdFromAuthorization(authorizationToken);
    }

    public String getAuthorizationToken() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return ((CustomAuthenticationDetails) authentication.getDetails()).getBearerToken();
    }
}
