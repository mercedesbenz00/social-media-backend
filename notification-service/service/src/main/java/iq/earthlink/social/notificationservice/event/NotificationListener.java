package iq.earthlink.social.notificationservice.event;

import iq.earthlink.social.classes.data.event.NotificationEvent;
import iq.earthlink.social.notificationservice.service.firebase.FirebaseNotificationService;
import iq.earthlink.social.notificationservice.service.notification.NotificationManager;
import iq.earthlink.social.util.LocalizationUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Slf4j
@Component
public class NotificationListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(NotificationListener.class);
    private final FirebaseNotificationService firebaseNotificationService;
    private final NotificationManager notificationManager;
    private final LocalizationUtil localizationUtil;

    public NotificationListener(FirebaseNotificationService firebaseNotificationService,
                                NotificationManager notificationManager,
                                LocalizationUtil localizationUtil) {
        this.firebaseNotificationService = firebaseNotificationService;
        this.notificationManager = notificationManager;
        this.localizationUtil = localizationUtil;
    }

    @KafkaListener(topics = "PUSH_NOTIFICATION", groupId = "notification-service",
            containerFactory = "notificationListenerContainerFactory")
    public void pushNotificationEvent(@Payload NotificationEvent event) {
        try {
            String eventName = event.getType().getMessageId();
            LOGGER.info("Received Push notification with type {}", eventName);

            if (CollectionUtils.isNotEmpty(event.getReceiverIds())) {
                notificationManager.createNotifications(event);

                String title = localizationUtil.getLocalizedMessage("push.notification." + eventName + ".title");
                List<String> placeholders = new ArrayList<>(event.getType().getMessagePlaceholders());

                for (int i = 0; i < placeholders.size(); i++) {
                    if (Objects.equals(placeholders.get(i), "authorName")) {
                        placeholders.set(i, event.getEventAuthor().getDisplayName());
                    } else {
                        placeholders.set(i, event.getMetadata().get(placeholders.get(i)) != null ? event.getMetadata().get(placeholders.get(i)) : placeholders.get(i));
                    }
                }

                String message = localizationUtil.getLocalizedMessage("push.notification." + eventName + ".message", placeholders.toArray(Object[]::new));
                // Push notifications to receivers:
                event.getReceiverIds().forEach(receiveId ->
                        firebaseNotificationService.sendPushNotificationToPerson(receiveId, title, message, eventName, event.getMetadata()));
            }

        } catch (Exception ex) {
            LOGGER.error("Failed to handle a notification:", ex);
        }
    }
}
