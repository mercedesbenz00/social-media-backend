package iq.earthlink.social.groupservice.group.notificationsettings;

import iq.earthlink.social.groupservice.group.rest.JsonGroupNotificationSettings;
import iq.earthlink.social.groupservice.group.rest.UserGroupNotificationSettingsDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import javax.annotation.Nonnull;
import java.util.List;

public interface GroupNotificationSettingsManager {
    @Nonnull
    Page<UserGroupNotificationSettingsDTO> findGroupNotificationSettings(@Nonnull Long personId, List<Long> groupIds, @Nonnull Pageable page);

    @Nonnull
    UserGroupNotificationSettingsDTO findGroupNotificationSettingsByGroupId(Long personId, Long groupId);

    UserGroupNotificationSettingsDTO setGroupNotificationSettings(Long personId, Long groupId, JsonGroupNotificationSettings request);

    List<UserGroupNotificationSettings> getAllGroupNotificationSettings();
}
