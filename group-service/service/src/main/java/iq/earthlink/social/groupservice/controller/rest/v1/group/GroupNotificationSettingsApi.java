package iq.earthlink.social.groupservice.controller.rest.v1.group;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import iq.earthlink.social.groupservice.group.notificationsettings.GroupNotificationSettingsManager;
import iq.earthlink.social.groupservice.group.rest.JsonGroupNotificationSettings;
import iq.earthlink.social.groupservice.group.rest.UserGroupNotificationSettingsDTO;
import iq.earthlink.social.security.DefaultSecurityProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@Api(value = "GroupNotificationSettingsApi", tags = "Group Notification Settings Api")
@RestController
@RequestMapping(value = "/api/v1/groups/notification-settings", produces = MediaType.APPLICATION_JSON_VALUE)
public class GroupNotificationSettingsApi {

    private static final Logger LOGGER = LoggerFactory.getLogger(GroupNotificationSettingsApi.class);

    private final GroupNotificationSettingsManager groupNotificationSettingsManager;
    private final DefaultSecurityProvider securityProvider;

    public GroupNotificationSettingsApi(GroupNotificationSettingsManager groupNotificationSettingsManager, DefaultSecurityProvider securityProvider) {
        this.groupNotificationSettingsManager = groupNotificationSettingsManager;
        this.securityProvider = securityProvider;
    }

    @ApiOperation("Returns the group notification settings list")
    @GetMapping
    public Page<UserGroupNotificationSettingsDTO> findGroupNotificationSettings(
            @RequestHeader("Authorization") String authorizationHeader,
            @ApiParam("Filters notification settings by the list of Group IDs")
            @RequestParam(required = false) List<Long> groupIds,
            Pageable page
    ) {
        LOGGER.debug("Requested find group notification settings with page: {}", page);
        Long personId = securityProvider.getPersonIdFromAuthorization(authorizationHeader);
        return groupNotificationSettingsManager.findGroupNotificationSettings(personId, groupIds, page);
    }

    @ApiOperation("Returns the group notification settings by groupId")
    @GetMapping("/{groupId}")
    public UserGroupNotificationSettingsDTO findGroupNotificationSettingsByGroupId(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable Long groupId
    ) {
        LOGGER.debug("Requested find group notification settings with groupId: {}", groupId);
        Long personId = securityProvider.getPersonIdFromAuthorization(authorizationHeader);
        return groupNotificationSettingsManager.findGroupNotificationSettingsByGroupId(personId, groupId);
    }

    @ApiOperation("Set group notification settings by groupId")
    @PutMapping("/{groupId}")
    public UserGroupNotificationSettingsDTO setGroupNotificationSettings(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable Long groupId,
            @RequestBody @Valid JsonGroupNotificationSettings request
    ) {
        Long personId = securityProvider.getPersonIdFromAuthorization(authorizationHeader);
        return groupNotificationSettingsManager.setGroupNotificationSettings(personId, groupId, request);
    }
}
