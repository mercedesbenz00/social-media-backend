package iq.earthlink.social.personservice.service;

import iq.earthlink.social.classes.data.dto.JsonStoryConfiguration;
import iq.earthlink.social.classes.enumeration.LocalizationType;
import iq.earthlink.social.classes.enumeration.StoryAccessType;
import iq.earthlink.social.classes.enumeration.ThemeType;
import iq.earthlink.social.personservice.person.impl.repository.FollowRepository;
import iq.earthlink.social.personservice.person.impl.repository.PersonConfigurationRepository;
import iq.earthlink.social.personservice.person.model.PersonConfiguration;
import iq.earthlink.social.personservice.person.rest.JsonPersonConfiguration;
import iq.earthlink.social.postservice.story.rest.StoryRestService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class PersonConfigurationServiceTest {

    private static final String AUTHORIZATION_HEADER = "authorizationHeader";

    @InjectMocks
    private PersonConfigurationService personConfigurationService;
    @Mock
    private PersonConfigurationRepository personConfigurationRepo;
    @Mock
    private StoryRestService storyRestService;
    @Mock
    private FollowRepository followRepository;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    void getByPersonId_getNotExistingConfig_returnJsonPersonConfigurationOnlyWithPersonId() {
        //given
        Long personId = 1L;
        PersonConfiguration personConfiguration = PersonConfiguration.builder()
                .personId(personId)
                .notificationMute(false)
                .build();

        given(personConfigurationRepo.save(any())).willReturn(personConfiguration);

        //when
        JsonPersonConfiguration jsonPersonConfiguration = personConfigurationService.getByPersonId(AUTHORIZATION_HEADER, personId);

        //then
        assertEquals(personId, jsonPersonConfiguration.getPersonId());
        assertFalse(jsonPersonConfiguration.isNotificationMute());
        assertNull(jsonPersonConfiguration.getLocalization());
        assertNull(jsonPersonConfiguration.getStory());
        assertNull(jsonPersonConfiguration.getTheme());
    }

    @Test
    void getByPersonId_getExistingConfig_returnJsonPersonConfiguration() {
        //given
        Long personId = 1L;
        PersonConfiguration personConfiguration = PersonConfiguration.builder()
                .id(2L)
                .personId(personId)
                .notificationMute(true)
                .localization(LocalizationType.EN)
                .theme(ThemeType.DARK)
                .build();

        JsonStoryConfiguration storyConfiguration = new JsonStoryConfiguration();
        storyConfiguration.setAccessType(StoryAccessType.PUBLIC);
        storyConfiguration.setAllowedFollowersIds(new HashSet<>(List.of(3L)));


        given(personConfigurationRepo.findByPersonId(personId)).willReturn(Optional.ofNullable(personConfiguration));
        given(storyRestService.getStoryConfiguration(any())).willReturn(storyConfiguration);

        //when
        JsonPersonConfiguration jsonPersonConfiguration = personConfigurationService.getByPersonId(AUTHORIZATION_HEADER, personId);

        //then
        assertEquals(personId, jsonPersonConfiguration.getPersonId());
        assertEquals(storyConfiguration, jsonPersonConfiguration.getStory());
        assertEquals(LocalizationType.EN, jsonPersonConfiguration.getLocalization());
        assertEquals(ThemeType.DARK, jsonPersonConfiguration.getTheme());
        assertTrue(jsonPersonConfiguration.isNotificationMute());
    }

    @Test
    void updatePersonConfig_updateConfigWithValidData_peronConfigUpdated() {
        //given
        Long personId = 1L;

        JsonStoryConfiguration storyConfiguration = new JsonStoryConfiguration();
        storyConfiguration.setAccessType(StoryAccessType.PUBLIC);
        storyConfiguration.setAllowedFollowersIds(new HashSet<>(List.of(3L)));

        JsonPersonConfiguration jsonPersonConfiguration = JsonPersonConfiguration.builder()
                .personId(personId)
                .notificationMute(true)
                .story(storyConfiguration)
                .localization(LocalizationType.EN)
                .theme(ThemeType.DARK)
                .build();

        PersonConfiguration personConfiguration = PersonConfiguration.builder()
                .id(2L)
                .personId(personId)
                .notificationMute(false)
                .build();

        ArgumentCaptor<PersonConfiguration> personConfigurationCaptor = ArgumentCaptor.forClass(PersonConfiguration.class);

        given(personConfigurationRepo.findByPersonId(personId)).willReturn(Optional.of(personConfiguration));

        //when
        personConfigurationService.updatePersonConfig(AUTHORIZATION_HEADER, personId, jsonPersonConfiguration);

        //then
        then(personConfigurationRepo).should().save(personConfigurationCaptor.capture());
        verify(personConfigurationRepo, times(1)).save(personConfiguration);
        verify(storyRestService, times(1)).setStoryConfiguration(any(), any());
        assertEquals(personId, personConfigurationCaptor.getValue().getPersonId());
        assertEquals(LocalizationType.EN, personConfigurationCaptor.getValue().getLocalization());
        assertEquals(ThemeType.DARK, personConfigurationCaptor.getValue().getTheme());
        assertTrue(jsonPersonConfiguration.isNotificationMute());
    }
}