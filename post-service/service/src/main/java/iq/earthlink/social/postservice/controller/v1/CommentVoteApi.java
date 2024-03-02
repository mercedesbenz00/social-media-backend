package iq.earthlink.social.postservice.controller.v1;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import iq.earthlink.social.classes.enumeration.VoteType;
import iq.earthlink.social.postservice.person.CurrentUser;
import iq.earthlink.social.postservice.person.dto.PersonDTO;
import iq.earthlink.social.postservice.post.rest.JsonVoteCount;
import iq.earthlink.social.postservice.post.vote.CommentVoteManager;
import iq.earthlink.social.postservice.post.vote.VoteCount;
import iq.earthlink.social.security.SecurityProvider;
import org.dozer.Mapper;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Api(value = "CommentVoteApi", tags = "Comment Vote Api")
@RestController
@RequestMapping("/api/v1/comments/{commentUuid}/votes")
public class CommentVoteApi {

    private final CommentVoteManager manager;
    private final Mapper mapper;
    private final SecurityProvider securityProvider;

    public CommentVoteApi(CommentVoteManager manager,
                          Mapper mapper,
                          SecurityProvider securityProvider) {
        this.manager = manager;
        this.mapper = mapper;
        this.securityProvider = securityProvider;
    }

    @ApiOperation("Updates post comment vote type. Returns total values of comment votes.")
    @PutMapping("/{voteType}")
    public JsonVoteCount submitCommentVote(
            @ApiParam("Comment ID")
            @PathVariable UUID commentUuid,
            @ApiParam("Type of vote")
            @PathVariable VoteType voteType,
            @CurrentUser PersonDTO person) {

        VoteCount count = manager.addCommentVote(person, commentUuid, voteType);
        return mapper.map(count, JsonVoteCount.class);
    }

    @ApiOperation("Removes vote for a post comment that has already been set. Returns total values of post comment upvotes and downvotes.")
    @DeleteMapping
    public JsonVoteCount removeCommentVote(
            @RequestHeader("Authorization") String authorizationHeader,
            @ApiParam("Id of the post comment")
            @PathVariable UUID commentUuid
    ) {
        Long personId = securityProvider.getPersonIdFromAuthorization(authorizationHeader);
        return manager.deleteCommentVote(personId, commentUuid);
    }
}
