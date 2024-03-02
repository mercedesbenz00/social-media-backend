package iq.earthlink.social.notificationservice.service.notification;

import iq.earthlink.social.classes.data.event.NotificationEvent;
import iq.earthlink.social.classes.enumeration.NotificationState;
import iq.earthlink.social.classes.enumeration.NotificationType;
import iq.earthlink.social.common.util.DateUtil;
import iq.earthlink.social.exception.NotFoundException;
import iq.earthlink.social.exception.RestApiException;
import iq.earthlink.social.notificationservice.data.model.Notification;
import iq.earthlink.social.notificationservice.data.repository.NotificationRepository;
import iq.earthlink.social.util.LocalizationUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.Nonnull;
import javax.transaction.Transactional;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class DefaultNotificationManager implements NotificationManager {

    private final NotificationRepository notificationRepository;
    private final NotificationProperties properties;

    private final LocalizationUtil localizationUtil;
    private final Random rnd = new Random();

    public DefaultNotificationManager(NotificationRepository notificationRepository,
                                      NotificationProperties properties,
                                      LocalizationUtil localizationUtil) {
        this.notificationRepository = notificationRepository;
        this.properties = properties;
        this.localizationUtil = localizationUtil;
    }

    @Transactional
    @Override
    public void createNotifications(NotificationEvent event) {
        try {
            int batchId = this.rnd.nextInt(999999);

            if (NotificationType.USER_INVITED_TO_GROUP.equals(event.getType())) {
                String groupId = event.getMetadata().get("groupId");
                List<Long> receiverIds = event.getReceiverIds();
                notificationRepository.deletePreviousGroupInvitations(receiverIds, groupId);
            }
            List<Notification> notifications = new ArrayList<>();
            event.getReceiverIds().forEach(id -> notifications.add(Notification.builder()
                    .batchId(batchId)
                    .authorId(event.getEventAuthor() != null ? event.getEventAuthor().getId() : null)
                    .receiverId(id)
                    .topic(event.getType())
                    .body("push.notification." + event.getType().getMessageId() + ".message")
                    .createdDate(new Date())
                    .state(NotificationState.NEW)
                    .metadata(event.getMetadata())
                    .build()));

            notificationRepository.saveAll(notifications);
        } catch (Exception ex) {
            throw new RestApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Error storing notifications: ", ex.getMessage());
        }
    }

    @Override
    public Page<Notification> findLatestNotifications(Long receiverPersonId, boolean showDeleted, NotificationState state, Pageable pageable) {
        // Calculate date using interval from configuration property:
        Page<Notification> notifications;
        Date afterDate = DateUtil.getDateBefore(Integer.parseInt(properties.getLastNotificationsIntervalDays()));
        if (state != null) {
            // Return all events by state after specified date:
            notifications = notificationRepository.findByReceiverIdAndStateAndCreatedDateAfterOrderByCreatedDateDesc(receiverPersonId, state, afterDate, pageable);
        } else if (showDeleted) {
            // Return all events regardless of state after specified date:
            notifications = notificationRepository.findByReceiverIdAndCreatedDateAfterOrderByCreatedDateDesc(receiverPersonId, afterDate, pageable);
        } else {
            // Return all events after specified date excluding deleted events:
            notifications = notificationRepository.findActiveNotifications(receiverPersonId, afterDate, pageable);
        }

        notifications.forEach(notification ->
        {
            List<String> placeholders = notification.getTopic().getMessagePlaceholders();
            notification
                    .setBody(localizationUtil.getLocalizedMessage(notification.getBody(), placeholders.stream().map(placeholder -> "#" + placeholder).toArray(Object[]::new)));
        });
        return notifications;
    }

    @Transactional
    @Override
    public List<Notification> updateNotificationState(@Nonnull List<Long> notificationIds,
                                                      @Nonnull NotificationState state) {

        // Find notifications by ids:
        List<Notification> notifications = notificationRepository.findByIdIn(notificationIds);

        // Find all notifications within the same batches:
        Set<Integer> batchIds = notifications.stream().map(Notification::getBatchId).collect(Collectors.toSet());
        Set<Notification> batchNotifications = notificationRepository.findByBatchIdIn(batchIds);

        // Update notifications state and date:
        batchNotifications.forEach(n -> {
            n.setState(state);
            n.setUpdatedDate(new Date());
        });

        if (CollectionUtils.isEmpty(batchNotifications)) {
            throw new NotFoundException("error.not.found.notifications");
        }

        notificationRepository.saveAll(batchNotifications);

        return notifications;
    }

    @Override
    public int deleteNotifications() {
        // Calculate date using interval from configuration property:
        Date date = DateUtil.getDateBefore(Integer.parseInt(properties.getLastNotificationsIntervalDays()));
        return notificationRepository.removeByCreatedDateBefore(date);
    }
}
