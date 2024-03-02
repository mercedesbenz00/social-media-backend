package iq.earthlink.social.notificationservice.data.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class NotificationBody {
    private String messageId;
    private String[] messageValues;
}
