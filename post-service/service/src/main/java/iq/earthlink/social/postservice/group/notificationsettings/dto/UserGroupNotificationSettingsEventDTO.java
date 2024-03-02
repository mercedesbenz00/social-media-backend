package iq.earthlink.social.postservice.group.notificationsettings.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserGroupNotificationSettingsEventDTO {
    private Long id;
    private Long personId;
    private Long groupId;
    private boolean isMuted;
}
