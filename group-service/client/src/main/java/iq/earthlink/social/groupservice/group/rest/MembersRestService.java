package iq.earthlink.social.groupservice.group.rest;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Set;

@FeignClient(value = "group-service", url = "${group.service.url}")
public interface MembersRestService {

    @GetMapping("/api/v1/groups/{groupId}/members/{memberId}")
    JsonMemberPermission getMember(
        @PathVariable("groupId") Long groupId,
        @PathVariable("memberId") Long memberId
    );

    @GetMapping(value = "/api/v1/groups/subscribed")
    Set<Long> getMyGroups(@RequestHeader("Authorization") String authorizationHeader,
                                   @RequestParam(value = "groupIds", required = false) List<Long> groupIds);

    @GetMapping("/api/v1/groups/members")
    Set<Long> getAllMembersInGroups(@RequestHeader("Authorization") String authorizationHeader,
                                    @RequestParam(value = "groupIds", required = false) List<Long> groupIds);

    @GetMapping("/api/v1/groups/{groupId}/members-with-settings")
    List<JsonGroupMemberWithNotificationSettings> getGroupMembersWithSettings(@RequestHeader("Authorization") String authorizationHeader,
                                    @PathVariable(value = "groupId") Long groupId);

}
