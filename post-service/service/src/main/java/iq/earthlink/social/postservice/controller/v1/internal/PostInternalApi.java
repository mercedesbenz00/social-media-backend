package iq.earthlink.social.postservice.controller.v1.internal;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import iq.earthlink.social.classes.data.dto.PostResponse;
import iq.earthlink.social.postservice.post.BFFPostManager;
import iq.earthlink.social.postservice.post.PostManager;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Api(value = "PostInternalApi", tags = "Post Internal Api")
@RestController
@RequestMapping(value = "/internal/v1", produces = MediaType.APPLICATION_JSON_VALUE)
public class PostInternalApi {

    private final PostManager postManager;
    private final BFFPostManager bffPostManager;

    public PostInternalApi(PostManager postManager, BFFPostManager bffPostManager) {
        this.postManager = postManager;
        this.bffPostManager = bffPostManager;
    }

    @ApiOperation("Returns top 10 groups based on user's frequent posts in the last 30 days, sorted by post count in descending order")
    @GetMapping("/frequently-posts")
    public List<Long> getFrequentlyPostsGroups(@RequestParam("personId") Long personId) {
        return postManager.getFrequentlyPostsGroups(personId);
    }

    @ApiOperation("Returns list of posts by postIds, used by user feed aggregator")
    @PostMapping("/posts-with-details")
    public List<PostResponse> getPosts(@RequestHeader("Authorization") String authorizationHeader, @RequestBody List<UUID> postUuids) {
        return bffPostManager.getPostsWithDetailsInternal(authorizationHeader, postUuids);
    }
}
