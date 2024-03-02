package iq.earthlink.social.postservice.group.notificationsettings;

import iq.earthlink.social.groupservice.group.rest.JsonGroupMemberWithNotificationSettings;
import iq.earthlink.social.postservice.group.notificationsettings.dto.UserGroupNotificationSettingsEventDTO;

import java.util.List;

public interface GroupNotificationSettingsManager {
    void setGroupNotificationSettings(UserGroupNotificationSettingsEventDTO userGroupNotificationSettingsEventDTO);

    List<JsonGroupMemberWithNotificationSettings> getGroupMembersWithSettings(Long userGroupId);
}
