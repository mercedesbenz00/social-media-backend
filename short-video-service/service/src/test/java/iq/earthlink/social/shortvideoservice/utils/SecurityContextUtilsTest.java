package iq.earthlink.social.shortvideoservice.utils;

import iq.earthlink.social.personservice.person.PersonInfo;
import iq.earthlink.social.personservice.person.rest.JsonPersonProfile;
import iq.earthlink.social.personservice.rest.PersonRestService;
import iq.earthlink.social.security.CustomAuthenticationDetails;
import iq.earthlink.social.security.SecurityProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


class SecurityContextUtilsTest {

    @InjectMocks
    private SecurityContextUtils securityContextUtils;
    private final PersonInfo givenUser = JsonPersonProfile.builder().username("admin@creativeadvtech.com").build();
    private final String givenToken = "JWT_TOKEN";

    @Mock
    private SecurityProvider securityProvider;
    @Mock
    private PersonRestService personRestService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
        initPrincipal();
    }

    @Test
    void getCurrentPersonInfo() {
        //given
        when(securityProvider.getPersonIdFromAuthorization(any())).thenReturn(1L);
        when(personRestService.getPersonProfile(any())).thenReturn((JsonPersonProfile) givenUser);
        //when
        PersonInfo personInfo = securityContextUtils.getCurrentPersonInfo();
        //then
        assertThat(personInfo).isEqualTo(givenUser);
    }

    @Test
    void getCurrentPersonId() {
        //given
        when(securityProvider.getPersonIdFromAuthorization(any())).thenReturn(1L);
        //when
        Long personId = securityContextUtils.getCurrentPersonId();
        //then
        assertThat(personId).isEqualTo(1L);
    }

    @Test
    void getAuthorizationToken() {
        //given
        //when
        String token = securityContextUtils.getAuthorizationToken();
        //then
        assertThat(token).isEqualTo(givenToken);
    }

    private void initPrincipal() {
        Authentication authentication = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);
        CustomAuthenticationDetails authenticationDetails = mock(CustomAuthenticationDetails.class);
        when(authenticationDetails.getBearerToken()).thenReturn(givenToken);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
        when(authentication.getDetails()).thenReturn(authenticationDetails);
    }
}