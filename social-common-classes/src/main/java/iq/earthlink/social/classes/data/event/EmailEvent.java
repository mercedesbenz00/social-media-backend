package iq.earthlink.social.classes.data.event;

import iq.earthlink.social.classes.enumeration.EmailType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailEvent {
    Long eventId;
    String recipientEmail;
    EmailType emailType;
    Map<String, Object> templateModel;
}
