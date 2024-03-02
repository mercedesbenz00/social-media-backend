package iq.earthlink.social.postservice.controller.v1;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import iq.earthlink.social.postservice.post.notificationsettings.PostNotificationSettingsManager;
import iq.earthlink.social.postservice.post.rest.JsonPostNotificationSettings;
import iq.earthlink.social.postservice.post.rest.PostNotificationSettingsDTO;
import iq.earthlink.social.security.SecurityProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@Api(value = "PostNotificationSettingsApi", tags = "Post Notification Settings Api")
@RestController
@RequestMapping(value = "/api/v1/posts/notification-settings", produces = MediaType.APPLICATION_JSON_VALUE)
public class PostNotificationSettingsApi {

    private static final Logger LOGGER = LoggerFactory.getLogger(PostNotificationSettingsApi.class);

    private final PostNotificationSettingsManager postNotificationSettingsManager;
    private final SecurityProvider securityProvider;

    public PostNotificationSettingsApi(PostNotificationSettingsManager postNotificationSettingsManager,
                                       SecurityProvider securityProvider) {
        this.postNotificationSettingsManager = postNotificationSettingsManager;
        this.securityProvider = securityProvider;
    }

    @ApiOperation("Returns the post notification settings list")
    @GetMapping
    public Page<PostNotificationSettingsDTO> findPostNotificationSettings(
            @RequestHeader("Authorization") String authorizationHeader,
            @ApiParam("Filters notification settings by the list of Post IDs")
            @RequestParam(required = false) List<Long> postIds,
            Pageable page
    ) {
        LOGGER.debug("Requested find post notification settings with page: {}", page);
        Long personId = securityProvider.getPersonIdFromAuthorization(authorizationHeader);
        return postNotificationSettingsManager.findPostNotificationSettings(personId, postIds, page);
    }

    @ApiOperation("Returns the post notification settings by postId")
    @GetMapping("/{postId}")
    public PostNotificationSettingsDTO findPostNotificationSettingsByPostId(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable Long postId
    ) {
        LOGGER.debug("Requested find post notification settings with postId: {}", postId);
        Long personId = securityProvider.getPersonIdFromAuthorization(authorizationHeader);
        return postNotificationSettingsManager.findPostNotificationSettingsByPostId(personId, postId);
    }

    @ApiOperation("Set post notification settings by postId")
    @PutMapping("/{postId}")
    public PostNotificationSettingsDTO setPostNotificationSettings(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable Long postId,
            @RequestBody @Valid JsonPostNotificationSettings request
    ) {
        Long personId = securityProvider.getPersonIdFromAuthorization(authorizationHeader);
        return postNotificationSettingsManager.setPostNotificationSettings(personId, postId, request);
    }
}

