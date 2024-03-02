package iq.earthlink.social.notificationservice.data.dto.matrix;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotEmpty;
import java.util.Map;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class NotificationPayload {
    private Map<String, Object> content;
    @NotEmpty
    private Device[] devices;
    private String eventId;
    private String prio;
    private String roomId;
    private String roomName;
    private String sender;
    private String senderDisplayName;
    private String type;
}
