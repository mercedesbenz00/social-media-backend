package iq.earthlink.social.commentservice.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import iq.earthlink.social.classes.data.validation.NewEntityGroup;
import iq.earthlink.social.classes.enumeration.CommentComplaintState;
import iq.earthlink.social.commentservice.dto.JsonComment;
import iq.earthlink.social.commentservice.dto.JsonCommentData;
import iq.earthlink.social.commentservice.service.CommentService;
import iq.earthlink.social.common.data.model.CommentEntity;
import iq.earthlink.social.personservice.person.PersonInfo;
import iq.earthlink.social.personservice.rest.PersonRestService;
import org.dozer.Mapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Api(value = "CommentsApi", tags = "Comments Api")
@RestController
@RequestMapping(value = "/api/v1/comment", produces = MediaType.APPLICATION_JSON_VALUE)
public class CommentApi {

    private final CommentService commentService;
    private final Mapper mapper;
    private final PersonRestService personRestService;

    public CommentApi(CommentService commentService, Mapper mapper, PersonRestService personRestService) {
        this.commentService = commentService;
        this.mapper = mapper;
        this.personRestService = personRestService;
    }

    @ApiOperation("Creates new comment for the post, short video etc.")
    @PostMapping()
    public JsonComment createComment(
            @RequestHeader("Authorization") String authorizationHeader,
            @Validated(NewEntityGroup.class)
            @RequestBody JsonCommentData payload) {

        PersonInfo person = personRestService.getPersonProfile(authorizationHeader);

        return mapper.map(commentService.createComment(person, payload), JsonComment.class);
    }

    @ApiOperation("Creates new comment for the post, short video etc.")
    @PutMapping("/{commentId}")
    public JsonComment updateComment(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable(value = "commentId") Long commentId,
            @Validated(NewEntityGroup.class)
            @RequestBody JsonCommentData payload) {
        PersonInfo person = personRestService.getPersonProfile(authorizationHeader);

        return mapper.map(commentService.updateComment(person, commentId, payload), JsonComment.class);
    }

    @ApiOperation("Returns the list of the root comments for the object")
    @GetMapping
    public Page<JsonComment> getComments(
            @RequestHeader("Authorization") String authorizationHeader,
            @ApiParam("The object unique identifier for filtering returned comments list")
            @RequestParam UUID objectId,
            @RequestParam(required = false) Boolean showAll,
            Pageable page) {
        PersonInfo person = personRestService.getPersonProfile(authorizationHeader);

        Page<CommentEntity> comments = commentService.findComments(person, objectId, showAll, page);
        return comments.map(c -> mapper.map(c, JsonComment.class));
    }

    @ApiOperation("Returns the list of the comments which has complaints")
    @GetMapping("/search/with-complaints")
    public Page<JsonComment> getCommentsWithComplaints(
            @RequestHeader("Authorization") String authorizationHeader,
            @RequestParam(required = false) Long groupId,
            @RequestParam(required = false, defaultValue = "PENDING") CommentComplaintState complainState,
            @RequestParam(required = false, defaultValue = "false") boolean deleted,
            Pageable page) {
        PersonInfo person = personRestService.getPersonProfile(authorizationHeader);

        Page<CommentEntity> comments = commentService.findCommentsWithComplaints(person, groupId, complainState, deleted, page);
        return comments.map(c -> mapper.map(c, JsonComment.class));
    }

    @ApiOperation("Returns comment by id")
    @GetMapping("/{commentId}")
    public JsonComment getComment(@PathVariable Long commentId,
                                  @ApiParam("UUID of commented object")
                                  @RequestParam("objectId") UUID objectId) {
        CommentEntity comment = commentService.getCommentEntity(commentId, objectId);
        return mapper.map(comment, JsonComment.class);
    }

    @ApiOperation("Reject comment by complaint")
    @PatchMapping("/complaints/{complaintId}/reject")
    public void rejectCommentByComplaint(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable Long complaintId,
            @RequestParam("reason") @ApiParam("Reject Reason (text)") String reason) {
        PersonInfo person = personRestService.getPersonProfile(authorizationHeader);
        commentService.rejectCommentByComplaint(person, reason, complaintId);
    }

    @ApiOperation("Remove comment by id")
    @DeleteMapping("/{commentId}")
    public void removeComment(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable Long commentId,
            @ApiParam("UUID of commented object")
            @RequestParam("objectId") UUID objectId) {
        PersonInfo person = personRestService.getPersonProfile(authorizationHeader);

        commentService.removeComment(person, commentId, objectId);
    }

    @ApiOperation("Saves reply to the comment")
    @PostMapping("/{commentId}/reply")
    public JsonComment reply(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable Long commentId,
            @RequestBody JsonCommentData data) {
        PersonInfo person = personRestService.getPersonProfile(authorizationHeader);
        return mapper.map(commentService.reply(person, commentId, data), JsonComment.class);
    }

    @ApiOperation("Returns list of replies for the comment")
    @GetMapping("/{commentId}/reply")
    public Page<JsonComment> getReplies(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable Long commentId,
            @ApiParam("UUID of commented object")
            @RequestParam("objectId") UUID objectId,
            @RequestParam(required = false) Boolean showAll,
            Pageable page) {
        PersonInfo person = personRestService.getPersonProfile(authorizationHeader);

        Page<CommentEntity> replies = commentService.getReplies(person, commentId, objectId, showAll, page);
        return replies.map(r -> mapper.map(r, JsonComment.class));
    }
}

