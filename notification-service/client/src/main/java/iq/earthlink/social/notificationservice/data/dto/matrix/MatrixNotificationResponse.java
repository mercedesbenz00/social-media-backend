package iq.earthlink.social.notificationservice.data.dto.matrix;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MatrixNotificationResponse {
    private String[] rejected;
}
