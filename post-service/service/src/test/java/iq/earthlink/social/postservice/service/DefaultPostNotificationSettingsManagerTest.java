package iq.earthlink.social.postservice.service;

import iq.earthlink.social.exception.NotFoundException;
import iq.earthlink.social.exception.RestApiException;
import iq.earthlink.social.postservice.post.PostManager;
import iq.earthlink.social.postservice.post.notificationsettings.DefaultPostNotificationSettingsManager;
import iq.earthlink.social.postservice.post.notificationsettings.PostNotificationSettings;
import iq.earthlink.social.postservice.post.notificationsettings.PostNotificationSettingsRepository;
import iq.earthlink.social.postservice.post.rest.JsonPostNotificationSettings;
import iq.earthlink.social.postservice.post.rest.PostNotificationSettingsDTO;
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
import org.springframework.data.domain.PageRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

class DefaultPostNotificationSettingsManagerTest {
    @InjectMocks
    private DefaultPostNotificationSettingsManager settingsManager;

    @Mock
    private PostNotificationSettingsRepository postNotificationSettingsRepository;
    @Mock
    private PostManager postManager;

    @Spy
    private Mapper mapper = DozerBeanMapperBuilder.create().build();

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    void findPostNotificationSettings_byPersonId_returnPaginatedResult() {
        List<Long> postIds = List.of(1L, 2L, 3L);
        List<PostNotificationSettings> expectedByPostIdsAndPersonId = getNotificationSettings().stream()
                .filter(s -> s.getPersonId().equals(1L) && postIds.contains(s.getPostId())).collect(Collectors.toList());
        Page<PostNotificationSettings> page1 = new PageImpl<>(expectedByPostIdsAndPersonId, PageRequest.of(0, 3), expectedByPostIdsAndPersonId.size());

        // given
        given(postNotificationSettingsRepository.findByPersonIdAndPostIdIn(1L, postIds, page1.getPageable())).willReturn(page1);

        Page<PostNotificationSettingsDTO> actualByPostIdsAndPersonId = settingsManager.findPostNotificationSettings(1L, postIds, page1.getPageable());
        assertTrue(actualByPostIdsAndPersonId.isFirst());
        assertEquals(1, actualByPostIdsAndPersonId.getTotalPages());
        assertEquals(expectedByPostIdsAndPersonId.size(), actualByPostIdsAndPersonId.getContent().size());
        assertEquals(expectedByPostIdsAndPersonId.size(), actualByPostIdsAndPersonId.getTotalElements());
    }

    @Test
    void findPostNotificationSettings_byPostIds_returnPaginatedResult() {
        List<PostNotificationSettings> expectedByPersonId = getNotificationSettings().stream()
                .filter(s -> s.getPersonId().equals(1L)).collect(Collectors.toList());
        Page<PostNotificationSettings> page1 = new PageImpl<>(expectedByPersonId, PageRequest.of(0, 5), expectedByPersonId.size());

        // given
        given(postNotificationSettingsRepository.findByPersonId(1L, page1.getPageable())).willReturn(page1);

        Page<PostNotificationSettingsDTO> actualByPersonId = settingsManager.findPostNotificationSettings(1L, null, page1.getPageable());
        assertTrue(actualByPersonId.isFirst());
        assertEquals(2, actualByPersonId.getTotalPages());
        assertEquals(expectedByPersonId.size(), actualByPersonId.getContent().size());
        assertEquals(expectedByPersonId.size(), actualByPersonId.getTotalElements());
    }

    @Test
    void findPostNotificationSettingsByPostId_notFound_throwException() {

        // given
        given(postNotificationSettingsRepository.findByPersonIdAndPostId(any(), any())).willReturn(Optional.empty());

        assertThatThrownBy(() -> settingsManager.findPostNotificationSettingsByPostId(1L, 11L))
                .isInstanceOf(RestApiException.class)
                .hasMessageContaining("error.not.found.post.notification.settings");
    }

    @Test
    void findPostNotificationSettingsByPostId_found_returnSetting() {

        // given
        given(postNotificationSettingsRepository.findByPersonIdAndPostId(any(), any())).willReturn(Optional.of(getNotificationSettings().get(0)));

        PostNotificationSettingsDTO setting = settingsManager.findPostNotificationSettingsByPostId(1L, 1L);
        assertEquals(1L, setting.getPostId().longValue());
        assertTrue(setting.isMuted());
    }

    @Test
    void setPostNotificationSettings_postNotFound_throwError() {
        JsonPostNotificationSettings settingRequest = new JsonPostNotificationSettings();
        settingRequest.setIsMuted(true);

        // given
        given(postManager.getPost(any())).willThrow(new NotFoundException("error.not.found.post", any()));

        assertThatThrownBy(() -> settingsManager.setPostNotificationSettings(1L, 11L, settingRequest))
                .isInstanceOf(RestApiException.class)
                .hasMessageContaining("error.not.found.post");
    }

    @Test
    void setPostNotificationSettings_postFound_returnSetting() {
        JsonPostNotificationSettings settingRequest = new JsonPostNotificationSettings();
        settingRequest.setIsMuted(true);
        // given
        given(postNotificationSettingsRepository.findByPersonIdAndPostId(any(), any())).willReturn(Optional.empty());

        PostNotificationSettingsDTO setting = settingsManager.setPostNotificationSettings(1L, 10L, settingRequest);
        assertEquals(settingRequest.getIsMuted(), setting.isMuted());

        given(postNotificationSettingsRepository.findByPersonIdAndPostId(any(), any())).willReturn(Optional.of(getNotificationSettings().get(6)));

        PostNotificationSettingsDTO setting1 = settingsManager.setPostNotificationSettings(1L, 10L, settingRequest);
        assertEquals(settingRequest.getIsMuted(), setting1.isMuted());
    }

    private List<PostNotificationSettings> getNotificationSettings() {
        PostNotificationSettings setting1 = PostNotificationSettings.builder()
                .personId(1L)
                .postId(1L)
                .isMuted(true)
                .build();
        PostNotificationSettings setting2 = PostNotificationSettings.builder()
                .personId(1L)
                .postId(2L)
                .isMuted(true)
                .build();
        PostNotificationSettings setting3 = PostNotificationSettings.builder()
                .personId(1L)
                .postId(3L)
                .isMuted(true)
                .build();
        PostNotificationSettings setting4 = PostNotificationSettings.builder()
                .personId(1L)
                .postId(4L)
                .isMuted(false)
                .build();
        PostNotificationSettings setting5 = PostNotificationSettings.builder()
                .personId(1L)
                .postId(5L)
                .isMuted(true)
                .build();
        PostNotificationSettings setting6 = PostNotificationSettings.builder()
                .personId(1L)
                .postId(6L)
                .isMuted(true)
                .build();
        PostNotificationSettings setting7 = PostNotificationSettings.builder()
                .personId(1L)
                .postId(7L)
                .isMuted(false)
                .build();
        PostNotificationSettings setting8 = PostNotificationSettings.builder()
                .personId(1L)
                .postId(8L)
                .isMuted(true)
                .build();
        PostNotificationSettings setting9 = PostNotificationSettings.builder()
                .personId(2L)
                .postId(1L)
                .isMuted(false)
                .build();
        PostNotificationSettings setting10 = PostNotificationSettings.builder()
                .personId(3L)
                .postId(1L)
                .isMuted(false)
                .build();

        List<PostNotificationSettings> settings = new ArrayList<>();
        settings.add(setting1);
        settings.add(setting2);
        settings.add(setting3);
        settings.add(setting4);
        settings.add(setting5);
        settings.add(setting6);
        settings.add(setting7);
        settings.add(setting8);
        settings.add(setting9);
        settings.add(setting10);
        return settings;
    }
}
