package iq.earthlink.social.notificationservice.data.dto.matrix;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MatrixNotification {
    @NotNull
    NotificationPayload notification;
}
