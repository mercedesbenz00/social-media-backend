package iq.earthlink.social.postservice.post.notificationsettings;

import iq.earthlink.social.postservice.post.rest.JsonPostNotificationSettings;
import iq.earthlink.social.postservice.post.rest.PostNotificationSettingsDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import javax.annotation.Nonnull;
import java.util.List;

public interface PostNotificationSettingsManager {
    @Nonnull
    Page<PostNotificationSettingsDTO> findPostNotificationSettings(@Nonnull Long personId, List<Long> postIds, @Nonnull Pageable page);

    @Nonnull
    PostNotificationSettingsDTO findPostNotificationSettingsByPostId(Long personId, Long postId);

    PostNotificationSettingsDTO setPostNotificationSettings(Long personId, Long postId, JsonPostNotificationSettings request);
}
