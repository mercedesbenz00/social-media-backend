package iq.earthlink.social.groupservice.controller.rest.v1.group.internal;

import io.swagger.annotations.Api;
import iq.earthlink.social.exception.BadRequestException;
import iq.earthlink.social.groupservice.group.GroupMemberStatus;
import iq.earthlink.social.groupservice.group.permission.GroupPermissionManager;
import iq.earthlink.social.groupservice.group.permission.PermissionSearchCriteria;
import iq.earthlink.social.groupservice.group.rest.JsonGroupPermission;
import lombok.RequiredArgsConstructor;
import org.dozer.Mapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Objects;

@Api(value = "GroupPermissionInternalApi", tags = "Group Permission Internal Api")
@RestController
@RequestMapping(value = "/internal/v1/groups/permissions", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class GroupPermissionInternalApi {

    private static final Logger LOGGER = LoggerFactory.getLogger(GroupPermissionInternalApi.class);

    private final Mapper mapper;
    private final GroupPermissionManager permissionManager;

    @GetMapping
    public List<JsonGroupPermission> findPermissions(
            @RequestParam(required = false) List<Long> groupIds,
            @RequestParam(required = false) Long personId,
            @RequestParam(required = false) List<GroupMemberStatus> statuses
    ) {

        if (CollectionUtils.isEmpty(groupIds) && Objects.isNull(personId))
            throw new BadRequestException("error.specify.group.person.or.query");

        PermissionSearchCriteria criteria = PermissionSearchCriteria.builder()
                .groupIds(groupIds)
                .personId(personId)
                .statuses(statuses)
                .build();

        LOGGER.debug("Internal request to fetch permissions for group(s): {} with criteria: {}", groupIds, criteria);

        return permissionManager.findPermissionsInternal(criteria)
                .stream().map(groupPermission -> mapper.map(groupPermission, JsonGroupPermission.class)).toList();

    }
}
