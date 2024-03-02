package iq.earthlink.social.notificationservice.service.matrix;

import iq.earthlink.social.notificationservice.data.dto.matrix.MatrixNotification;
import iq.earthlink.social.notificationservice.data.dto.matrix.MatrixNotificationResponse;

public interface MatrixNotificationManager {

    MatrixNotificationResponse pushNotification(MatrixNotification notification);
}
