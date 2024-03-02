package iq.earthlink.social.personservice.controller.rest.person.v1;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import iq.earthlink.social.personservice.controller.CurrentUser;
import iq.earthlink.social.personservice.dto.PersonDTO;
import iq.earthlink.social.personservice.person.NotificationsSettingsManager;
import iq.earthlink.social.personservice.person.rest.JsonFollowerNotificationSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@Api(tags = "Notifications Settings Api", value = "NotificationsSettingsApi")
@RestController
@RequestMapping(value = "/api/v1/persons/notification-settings", produces = MediaType.APPLICATION_JSON_VALUE)
public class NotificationsSettingsApi {

    private static final Logger LOGGER = LoggerFactory.getLogger(NotificationsSettingsApi.class);

    private final NotificationsSettingsManager notificationsSettingsManager;

    public NotificationsSettingsApi(NotificationsSettingsManager notificationsSettingsManager) {
        this.notificationsSettingsManager = notificationsSettingsManager;
    }

    /**
     * Finds person subscriptions.
     *
     * @param currentUser the current user
     * @param page        the pagination params for the result set
     */
    @ApiOperation("Returns the list of followers notification settings for the target person ")
    @GetMapping
    public Page<JsonFollowerNotificationSettings> getNotificationsSettings(
            @CurrentUser PersonDTO currentUser,
            @ApiParam("Filters notification settings by the list of Follower IDs")
            @RequestParam(required = false) List<Long> followerIds,
            Pageable page) {
        LOGGER.debug("Received 'get notifications settings' request for the person: {} with pagination: {}",
                currentUser.getId(), page);
        return notificationsSettingsManager.getNotificationsSettings(currentUser.getId(), followerIds, page);
    }

    @ApiOperation("Return follower notification setting for the target person")
    @GetMapping("/{followerId}")
    public JsonFollowerNotificationSettings getNotificationsSettings(@CurrentUser PersonDTO currentUser, @PathVariable Long followerId) {
        LOGGER.debug("Received 'get notifications settings' request for the person: {}",
                followerId);
        return notificationsSettingsManager.getNotificationSettingsByFollowerId(currentUser.getId(), followerId);
    }

    @ApiOperation("Set follower notification settings")
    @PutMapping("/{followerId}")
    public JsonFollowerNotificationSettings setNotificationsSettings(@CurrentUser PersonDTO currentUser, @PathVariable Long followerId, @RequestBody JsonFollowerNotificationSettings jsonFollowerNotificationSettings) {
        LOGGER.debug("Received 'set notifications settings' request for the person: {}",
                followerId);
        return notificationsSettingsManager.setNotificationsSettings(currentUser.getId(), followerId, jsonFollowerNotificationSettings);
    }

    @ApiOperation("Returns the list of personIds who muted followingId")
    @GetMapping("/muted-following")
    public List<Long> getPersonIdsWhoMutedFollowingId(
            @CurrentUser PersonDTO currentUser) {
        LOGGER.debug("Received 'get personIds who muted followingId' request for the person: {}", currentUser.getId());
        return notificationsSettingsManager.getPersonIdsWhoMutedFollowingId(currentUser.getId());
    }
}
