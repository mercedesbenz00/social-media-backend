package iq.earthlink.social.classes.data.event;

import iq.earthlink.social.classes.data.dto.PersonData;
import iq.earthlink.social.classes.enumeration.NotificationState;
import iq.earthlink.social.classes.enumeration.NotificationType;
import lombok.*;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class NotificationEvent implements Serializable {
    private List<Long> receiverIds;
    private NotificationType type;
    private NotificationState state;
    private PersonData eventAuthor;
    private Map<String, String> metadata;
}