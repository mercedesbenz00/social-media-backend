package iq.earthlink.social.postservice.rest;

import iq.earthlink.social.classes.data.dto.PostResponse;
import iq.earthlink.social.postservice.post.rest.ComplaintStatsDTO;
import iq.earthlink.social.postservice.post.rest.GroupStatsDTO;
import iq.earthlink.social.postservice.post.rest.JsonReasonWithLocalization;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(name = "post-service", url = "${post.service.url}")
public interface PostRestService {
    @GetMapping(value = "/api/v1/posts/complaints/reasons/{reasonId}")
    JsonReasonWithLocalization getComplaintReason(
            @RequestHeader(value = "Authorization") String authorizationHeader,
            @PathVariable(value = "reasonId") Long reasonId);


    @GetMapping(value = "/api/v1/posts/complaint-stats")
    ComplaintStatsDTO getPersonComplaintStats(
            @RequestHeader(value = "Authorization") String authorizationHeader,
            @RequestParam(value = "personId") Long personId);

    @GetMapping(value = "/internal/v1/frequently-posts")
    List<Long> getFrequentlyPostsGroups(@RequestParam(value = "personId") Long personId);

    @GetMapping(value = "/api/v1/posts/group-stats")
    GroupStatsDTO getGroupStats(
            @RequestHeader(value = "Authorization") String authorizationHeader,
            @RequestParam(value = "groupId") Long groupId);

    @PostMapping(path = "/internal/v1/posts-with-details")
    List<PostResponse> getPosts(
            @RequestHeader(value = "Authorization") String authorizationHeader,
            @RequestBody List<String> postUuids);
}
