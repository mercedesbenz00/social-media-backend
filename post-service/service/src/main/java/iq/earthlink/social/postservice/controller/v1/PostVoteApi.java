package iq.earthlink.social.postservice.controller.v1;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import iq.earthlink.social.classes.enumeration.VoteType;
import iq.earthlink.social.postservice.person.CurrentUser;
import iq.earthlink.social.postservice.person.dto.PersonDTO;
import iq.earthlink.social.postservice.post.PostManager;
import iq.earthlink.social.postservice.post.rest.JsonVoteCount;
import iq.earthlink.social.postservice.post.vote.PostVoteManager;
import iq.earthlink.social.postservice.post.vote.VoteCount;
import iq.earthlink.social.security.SecurityProvider;
import org.dozer.Mapper;
import org.springframework.web.bind.annotation.*;

import java.util.Objects;

@Api(value = "PostVoteApi", tags = "Post Vote Api")
@RestController
@RequestMapping("/api/v1/posts/{postId}/votes")
public class PostVoteApi {

    private final PostVoteManager manager;
    private final PostManager postManager;
    private final Mapper mapper;
    private final SecurityProvider securityProvider;

    public PostVoteApi(PostVoteManager manager,
                       PostManager postManager,
                       Mapper mapper,
                       SecurityProvider securityProvider) {
        this.manager = manager;
        this.postManager = postManager;
        this.mapper = mapper;
        this.securityProvider = securityProvider;
    }

    @ApiOperation("Updates post vote type. Returns total values of post votes.")
    @PutMapping("/{voteType}")
    public JsonVoteCount submitPostVote(
            @ApiParam("Post ID")
            @PathVariable Long postId,
            @ApiParam("Type of vote")
            @PathVariable VoteType voteType,
            @CurrentUser PersonDTO person) {

        VoteCount count = manager.addPostVote(person, postManager.getPost(postId), voteType);

        return Objects.nonNull(count) ? mapper.map(count, JsonVoteCount.class)
                : new JsonVoteCount(postId, 0L, 0L, 0);
    }

    @ApiOperation("Removes vote for a post that has already been set. Returns total values of post upvotes and downvotes.")
    @DeleteMapping
    public JsonVoteCount removePostVote(
            @RequestHeader("Authorization") String authorizationHeader,
            @ApiParam("Id of the post")
            @PathVariable Long postId
    ) {
        Long personId = securityProvider.getPersonIdFromAuthorization(authorizationHeader);
        VoteCount count = manager.deletePostVote(personId, postManager.getPost(postId));
        return Objects.nonNull(count) ? mapper.map(count, JsonVoteCount.class) : new JsonVoteCount(postId, 0L, 0L, 0);
    }
}
