package iq.earthlink.social.groupservice.group;

import iq.earthlink.social.exception.NotFoundException;
import iq.earthlink.social.exception.RestApiException;
import iq.earthlink.social.groupservice.event.KafkaProducerService;
import iq.earthlink.social.groupservice.group.notificationsettings.DefaultGroupNotificationSettingsManager;
import iq.earthlink.social.groupservice.group.notificationsettings.UserGroupNotificationSettings;
import iq.earthlink.social.groupservice.group.notificationsettings.UserGroupNotificationSettingsRepository;
import iq.earthlink.social.groupservice.group.rest.JsonGroupNotificationSettings;
import iq.earthlink.social.groupservice.group.rest.UserGroupNotificationSettingsDTO;
import org.dozer.DozerBeanMapperBuilder;
import org.dozer.Mapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

class DefaultGroupNotificationSettingsManagerTest {

    @InjectMocks
    private DefaultGroupNotificationSettingsManager groupNotificationSettingsManager;

    @Mock
    private UserGroupNotificationSettingsRepository repository;
    @Mock
    private GroupManagerUtils groupManagerUtils;
    @Mock
    private KafkaProducerService kafkaProducerService;
    @Spy
    private Mapper mapper = DozerBeanMapperBuilder.create().build();

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    void findGroupNotificationSettings_emptyGroupId_returnPaginatedResult() {

        List<UserGroupNotificationSettings> settingsExpected = List.of(
                UserGroupNotificationSettings.builder().id(1L).groupId(1L).personId(1L).isMuted(true).build(),
                UserGroupNotificationSettings.builder().groupId(2L).personId(1L).build()
        );
        //given
        given(repository.findByPersonId(any(), any())).willReturn(new PageImpl<>(settingsExpected));

        //when
        Page<UserGroupNotificationSettingsDTO> settings = groupNotificationSettingsManager.findGroupNotificationSettings(1L, Collections.emptyList(), Pageable.unpaged());
        //then
        assertTrue(settings.isFirst());
        assertEquals(settings.getContent().size(), settingsExpected.size());
    }

    @Test
    void findGroupNotificationSettings_validInput_returnPaginatedResult() {
        List<UserGroupNotificationSettings> settingsExpected = List.of(
                UserGroupNotificationSettings.builder().id(1L).groupId(1L).personId(1L).isMuted(true).build(),
                UserGroupNotificationSettings.builder().groupId(1L).personId(2L).build()
        );
        //given
        given(repository.findByPersonIdAndGroupIdIn(any(), any(), any()))
                .willReturn(new PageImpl<>(settingsExpected.stream().filter(s -> s.getGroupId().equals(1L) && s.getPersonId().equals(1L)).collect(Collectors.toList())));

        //when
        Page<UserGroupNotificationSettingsDTO> settings = groupNotificationSettingsManager.findGroupNotificationSettings(1L, List.of(1L), Pageable.unpaged());
        //then
        assertTrue(settings.isFirst());
        assertEquals(1, settings.getContent().size());
        assertTrue(settings.getContent().get(0).isMuted());
    }

    @Test
    void findGroupNotificationSettingsByGroupId_invalidGroup_throwException() {
        //given
        given(repository.findByPersonIdAndGroupId(any(), any()))
                .willThrow(new NotFoundException("error.not.found.group.notification.settings", any(), any()));

        //when
        //then
        assertThatThrownBy(() -> groupNotificationSettingsManager.findGroupNotificationSettingsByGroupId(1L, 1234L))
                .isInstanceOf(RestApiException.class)
                .hasMessageContaining("error.not.found.group.notification.settings");
    }

    @Test
    void findGroupNotificationSettingsByGroupId_found_returnSettingsDto() {
        UserGroupNotificationSettings expectedSettings = UserGroupNotificationSettings.builder()
                .personId(1L)
                .groupId(1L)
                .isMuted(true)
                .build();

        //given
        given(repository.findByPersonIdAndGroupId(any(), any())).willReturn(Optional.of(expectedSettings));

        //when
        UserGroupNotificationSettingsDTO actualSettings = groupNotificationSettingsManager.findGroupNotificationSettingsByGroupId(1L, 1L);

        //then
        assertTrue(actualSettings.isMuted());
        assertEquals(actualSettings.getGroupId(), expectedSettings.getGroupId());
    }

    @Test
    void setGroupNotificationSettings_invalidGroup_throwException() {
        JsonGroupNotificationSettings request = new JsonGroupNotificationSettings();
        request.setIsMuted(true);

        //given
        given(groupManagerUtils.getGroup(any())).willThrow(new NotFoundException("error.group.not.found", any()));

        //when
        //then
        assertThatThrownBy(() -> groupNotificationSettingsManager.setGroupNotificationSettings(1L, 1234L, request))
                .isInstanceOf(RestApiException.class)
                .hasMessageContaining("error.group.not.found");
    }

    @Test
    void setGroupNotificationSettings_settingExists_returnUpdatedSetting() {
        JsonGroupNotificationSettings request = new JsonGroupNotificationSettings();
        request.setIsMuted(true);
        UserGroup group = UserGroup.builder()
                .id(1L)
                .name("Test group")
                .build();
        UserGroupNotificationSettings existingSetting = UserGroupNotificationSettings.builder().isMuted(false).groupId(1L).personId(1L).build();
        //given
        given(groupManagerUtils.getGroup(any())).willReturn(group);
        given(repository.findByPersonIdAndGroupId(any(), any())).willReturn(Optional.of(existingSetting));

        //when
        UserGroupNotificationSettingsDTO actualSettings = groupNotificationSettingsManager.setGroupNotificationSettings(1L, 1L, request);

        //then
        assertTrue(actualSettings.isMuted());
        assertEquals(actualSettings.getGroupId(), existingSetting.getGroupId());

    }
}
