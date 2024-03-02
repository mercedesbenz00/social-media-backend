package iq.earthlink.social.groupservice.controller.rest.v1.group;

import io.swagger.annotations.Api;
import iq.earthlink.social.exception.BadRequestException;
import iq.earthlink.social.groupservice.group.GroupManagerUtils;
import iq.earthlink.social.groupservice.group.GroupMemberStatus;
import iq.earthlink.social.groupservice.group.permission.GroupPermissionManager;
import iq.earthlink.social.groupservice.group.permission.PermissionSearchCriteria;
import iq.earthlink.social.groupservice.group.rest.JsonGroupPermission;
import iq.earthlink.social.groupservice.group.rest.JsonGroupPermissionData;
import iq.earthlink.social.groupservice.util.RoleUtil;
import iq.earthlink.social.security.DefaultSecurityProvider;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.annotations.ApiIgnore;

import java.util.List;
import java.util.Objects;

/**
 * This API may be not required due to GroupMember has attribute status that responsible
 * for handling group member permission based on that status.
 */
@Api(value = "The Group Permission API allows set up the permission for the group members", hidden = true)
@ApiIgnore
@RestController
@RequestMapping("/api/v1/groups")
public class GroupPermissionApi {

    private static final Logger LOGGER = LoggerFactory.getLogger(GroupPermissionApi.class);

    private final GroupManagerUtils groupManagerUtils;
    private final GroupPermissionManager permissionManager;
    private final DefaultSecurityProvider securityProvider;
    private final RoleUtil roleUtil;

    public GroupPermissionApi(
            GroupManagerUtils groupManagerUtils,
            GroupPermissionManager permissionManager,
            DefaultSecurityProvider securityProvider, RoleUtil roleUtil) {
        this.groupManagerUtils = groupManagerUtils;
        this.permissionManager = permissionManager;
        this.securityProvider = securityProvider;
        this.roleUtil = roleUtil;
    }

    @PostMapping("/{groupId}/permissions")
    public JsonGroupPermission addPermission(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable Long groupId,
            @RequestBody JsonGroupPermissionData data
    ) {
        Long personId = securityProvider.getPersonIdFromAuthorization(authorizationHeader);
        String[] personRoles = securityProvider.getRolesFromAuthorization(authorizationHeader);
        boolean isAdmin = roleUtil.isAdmin(personRoles);
        LOGGER.debug("Requested add permission: {} to group: {} by {}",
                data, groupId, personId);

        return permissionManager
                .setPermission(personId, isAdmin, groupManagerUtils.getGroup(groupId), data);
    }

    @GetMapping("/permissions")
    public Page<JsonGroupPermission> findPermissions(
            @RequestHeader("Authorization") String authorizationHeader,
            @RequestParam(required = false) List<Long> groupIds,
            @RequestParam(required = false) Long personId,
            @RequestParam(required = false) List<GroupMemberStatus> statuses,
            @RequestParam(required = false) String query,
            Pageable page
    ) {
        Long callerPersonId = securityProvider.getPersonIdFromAuthorization(authorizationHeader);
        String[] callerRoles = securityProvider.getRolesFromAuthorization(authorizationHeader);
        boolean isCallerAdmin = roleUtil.isAdmin(callerRoles);

        if (CollectionUtils.isEmpty(groupIds) && Objects.isNull(personId) && StringUtils.isEmpty(query))
            throw new BadRequestException("error.specify.group.person.or.query");

        PermissionSearchCriteria criteria = PermissionSearchCriteria.builder()
                .groupIds(groupIds)
                .personId(personId)
                .statuses(statuses)
                .query(query)
                .build();

        LOGGER.debug("Requested find group: {} permissions with criteria: {}", groupIds, criteria);

        return permissionManager.findPermissions(criteria, callerPersonId, isCallerAdmin, authorizationHeader, page);
    }

    @DeleteMapping("/{groupId}/permissions/{permissionId}")
    public void removePermission(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable Long groupId,
            @PathVariable Long permissionId
    ) {
        Long personId = securityProvider.getPersonIdFromAuthorization(authorizationHeader);
        String[] personRoles = securityProvider.getRolesFromAuthorization(authorizationHeader);
        boolean isAdmin = roleUtil.isAdmin(personRoles);
        LOGGER.debug("Requested remove group: {} permission: {} by {}",
                groupId, permissionId, personId);

        permissionManager.removePermission(personId, isAdmin, permissionId);
    }

    @GetMapping(value = "/permissions/access")
    List<Long> getAccessibleGroups(
            @RequestHeader("Authorization") String authorizationHeader,
            @RequestParam(value = "groupIds", required = false) List<Long> groupIds) {
        Long personId = securityProvider.getPersonIdFromAuthorization(authorizationHeader);
        String[] personRoles = securityProvider.getRolesFromAuthorization(authorizationHeader);
        boolean isAdmin = roleUtil.isAdmin(personRoles);
        return permissionManager.getAccessibleGroups(personId, isAdmin, groupIds);
    }

}
