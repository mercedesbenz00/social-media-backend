package iq.earthlink.social.notificationservice.service.notification;

import iq.earthlink.social.classes.data.event.NotificationEvent;
import iq.earthlink.social.classes.enumeration.NotificationState;
import iq.earthlink.social.notificationservice.data.model.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import javax.annotation.Nonnull;
import java.util.List;

public interface NotificationManager {

    void createNotifications(NotificationEvent event);

    Page<Notification> findLatestNotifications(Long receiverPersonId, boolean showDeleted, NotificationState state, Pageable pageable);

    List<Notification> updateNotificationState(@Nonnull List<Long> notificationIds, @Nonnull NotificationState state);

    int deleteNotifications();
}
