package iq.earthlink.social.notificationservice.client;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TextNotification implements Notification {
    private static final NotificationType type = NotificationType.TEXT;

    private final String title;
    private final String body;

    @Override
    public NotificationType getType() {
        return TextNotification.type;
    }
}
