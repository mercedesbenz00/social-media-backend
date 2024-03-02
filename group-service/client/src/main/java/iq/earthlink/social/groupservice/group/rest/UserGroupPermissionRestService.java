package iq.earthlink.social.groupservice.group.rest;

import iq.earthlink.social.groupservice.group.GroupMemberStatus;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "group-service", url = "${group.service.url}")
public interface UserGroupPermissionRestService {

    @GetMapping(value = "/internal/v1/groups/permissions")
    List<JsonGroupPermission> findGroupPermissions(
            @RequestHeader("Authorization") String authorizationHeader,
            @RequestParam(value = "groupIds", required = false) List<Long> groupIds,
            @RequestParam(value = "personId", required = false) Long personId,
            @RequestParam(value = "statuses", required = false) List<GroupMemberStatus> statuses);

    @GetMapping(value = "/api/v1/groups/{groupId}")
    UserGroupDto getGroup(@RequestHeader("Authorization") String authorizationHeader,
                          @PathVariable("groupId") Long groupId);

    @GetMapping(value = "/api/v1/groups/permissions/access")
    List<Long> getAccessibleGroups(@RequestHeader("Authorization") String authorizationHeader,
                                   @RequestParam(value = "groupIds", required = false) List<Long> groupIds);
}
