package iq.earthlink.social.notificationservice.controller.rest.v1.notification;

import io.swagger.annotations.*;
import iq.earthlink.social.classes.data.dto.PersonData;
import iq.earthlink.social.classes.data.event.NotificationEvent;
import iq.earthlink.social.classes.enumeration.NotificationState;
import iq.earthlink.social.exception.ForbiddenException;
import iq.earthlink.social.notificationservice.data.dto.JsonNotification;
import iq.earthlink.social.notificationservice.model.PushNotificationRequest;
import iq.earthlink.social.notificationservice.service.firebase.FirebaseNotificationService;
import iq.earthlink.social.notificationservice.service.notification.NotificationManager;
import iq.earthlink.social.personservice.person.PersonInfo;
import iq.earthlink.social.personservice.person.rest.JsonPerson;
import iq.earthlink.social.personservice.rest.PersonRestService;
import iq.earthlink.social.security.DefaultSecurityProvider;
import org.dozer.Mapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Api(value = "NotificationApi", tags = "Notification Api")
@RestController
@RequestMapping(value = "/api/v1/notifications", produces = MediaType.APPLICATION_JSON_VALUE)
@Validated
public class NotificationApi {
    private static final Logger LOGGER = LoggerFactory.getLogger(NotificationApi.class);

    private final Mapper mapper;
    private final NotificationManager notificationManager;
    private final FirebaseNotificationService firebaseNotificationService;
    private final DefaultSecurityProvider securityProvider;
    private final PersonRestService personRestService;

    public NotificationApi(Mapper mapper, NotificationManager notificationManager,
                           FirebaseNotificationService firebaseNotificationService,
                           DefaultSecurityProvider securityProvider,
                           PersonRestService personRestService) {
        this.mapper = mapper;
        this.notificationManager = notificationManager;
        this.firebaseNotificationService = firebaseNotificationService;
        this.securityProvider = securityProvider;
        this.personRestService = personRestService;
    }

    @GetMapping
    @ApiOperation("Returns list of notifications for configurable period.")
    public Page<JsonNotification> findLatestNotifications(
            @RequestHeader("Authorization") String authorizationHeader,
            @RequestParam(value = "showDeleted", required = false, defaultValue = "false") boolean showDeleted,
            @RequestParam(value = "state", required = false) NotificationState state,
            Pageable pageable) {
        Long personId = securityProvider.getPersonIdFromAuthorization(authorizationHeader);

        return notificationManager.findLatestNotifications(personId, showDeleted, state, pageable)
                .map(n -> {
                    var notification = mapper.map(n, JsonNotification.class);
                    if (n.getAuthorId() != null) {
                        JsonPerson p = personRestService.getPersonById(authorizationHeader, n.getAuthorId());
                        notification.setEventAuthor(mapper.map(p, PersonData.class));
                    }
                    return notification;
                });
    }

    @ApiOperation("Updates state for notifications")
    @ApiResponses({
            @ApiResponse(code = 200, message = "If notification successfully updated"),
            @ApiResponse(code = 400, message = "If invalid data provided"),
            @ApiResponse(code = 401, message = "If user is not authenticated"),
            @ApiResponse(code = 404, message = "If target notification is not found"),
            @ApiResponse(code = 500, message = "If any unexpected error occurred"),
    })
    @PutMapping()
    public List<JsonNotification> updateNotificationState(
            @ApiParam("List of notification IDs, required")
            @RequestParam("notificationIds") List<Long> notificationIds,
            @ApiParam("New notification state")
            @RequestParam("state") NotificationState state) {

        return notificationManager.updateNotificationState(notificationIds, state).stream()
                .map(n -> mapper.map(n, JsonNotification.class)).toList();
    }

    @ApiOperation("Creates new notifications (endpoint for testing purposes)")
    @ApiResponses({
            @ApiResponse(code = 200, message = "If notification successfully created"),
            @ApiResponse(code = 400, message = "If invalid data provided"),
            @ApiResponse(code = 401, message = "If user is not authenticated"),
            @ApiResponse(code = 500, message = "If any unexpected error occurred"),
    })
    @PostMapping()
    public void createNotification(
            @RequestHeader("Authorization") String authorizationHeader,
            @RequestBody @Valid JsonNotification data) {
        PersonInfo person = personRestService.getPersonProfile(authorizationHeader);

        if (!person.isAdmin()) {
            throw new ForbiddenException("error.operation.not.permitted");
        }
        PersonData eventAuthor = PersonData
                .builder()
                .id(person.getId())
                .avatar(person.getAvatar())
                .displayName(person.getDisplayName())
                .build();

        NotificationEvent event = NotificationEvent
                .builder()
                .eventAuthor(eventAuthor)
                .type(data.getTopic())
                .metadata(data.getMetadata())
                .receiverIds(List.of(data.getReceiverId()))
                .build();

        notificationManager.createNotifications(event);
    }

    @PostMapping(value = "/push")
    public String sendPush(
            @RequestParam("sendToToken") String sendToToken,
            @RequestParam(value = "topic", required = false, defaultValue = "") String topic,
            @RequestParam(value = "title", defaultValue = "Title", required = false) String title,
            @RequestParam(value = "message", defaultValue = "Message", required = false) String message,
            @RequestParam(value = "param", required = false) String param) {

        PushNotificationRequest request = new PushNotificationRequest();
        request.setToken(sendToToken);
        request.setTitle(title);
        request.setMessage(message);
        request.setTopic(topic);

        Map<String, String> data = new HashMap<>();
        if (param != null) {
            data.put("param", param);
        }
        request.setData(data);
        return firebaseNotificationService.sendPushNotificationToToken(request);
    }

    @Scheduled(cron = "${social.notificationservice.notification.cleanup.cron}")
    @Transactional
    public void cleanupNotifications() {
        LOGGER.info("Running a scheduled job to remove expired notifications...");
        int deletedNotificationsCount = 0;
        try {
            deletedNotificationsCount = notificationManager.deleteNotifications();
        } catch (Exception ex) {
            LOGGER.error(ex.getMessage());
        } finally {
            LOGGER.info("Removed {} records from Notification table. ", deletedNotificationsCount);
        }
    }
}
