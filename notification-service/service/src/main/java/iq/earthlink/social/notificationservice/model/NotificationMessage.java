package iq.earthlink.social.notificationservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationMessage<T extends Serializable> implements Serializable {
    private MessageType type;
    private String notificationType;
    private String content;
    private String sender;
    private String url;
    private String uuid;
    private T body;

    public enum MessageType {
        NOTIFICATION,
        JOIN,
        LEAVE
    }
}
