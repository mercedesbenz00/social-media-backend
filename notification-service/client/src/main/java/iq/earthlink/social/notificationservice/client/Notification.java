package iq.earthlink.social.notificationservice.client;

import java.io.Serializable;

public interface Notification extends Serializable {
    NotificationType getType();
}
