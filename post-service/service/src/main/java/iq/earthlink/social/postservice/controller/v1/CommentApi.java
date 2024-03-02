package iq.earthlink.social.postservice.controller.v1;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import iq.earthlink.social.classes.data.validation.NewEntityGroup;
import iq.earthlink.social.classes.enumeration.CommentComplaintState;
import iq.earthlink.social.postservice.person.CurrentUser;
import iq.earthlink.social.postservice.person.dto.PersonDTO;
import iq.earthlink.social.postservice.post.comment.CommentService;
import iq.earthlink.social.postservice.post.rest.JsonComment;
import iq.earthlink.social.postservice.post.rest.JsonCommentData;
import iq.earthlink.social.postservice.util.RoleUtil;
import iq.earthlink.social.security.DefaultSecurityProvider;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Api(value = "CommentApi", tags = "Comment Api")
@RestController
@RequestMapping(value = "/api/v1/comments", produces = MediaType.APPLICATION_JSON_VALUE)
public class CommentApi {

    private final CommentService commentService;
    private final DefaultSecurityProvider securityProvider;
    private final RoleUtil roleUtil;

    public CommentApi(CommentService commentService, DefaultSecurityProvider securityProvider, RoleUtil roleUtil) {
        this.commentService = commentService;
        this.securityProvider = securityProvider;
        this.roleUtil = roleUtil;
    }

    @ApiOperation("Creates new comment for the post")
    @PostMapping
    public JsonComment createComment(
            @RequestHeader("Authorization") String authorizationHeader,
            @Validated(NewEntityGroup.class)
            @RequestBody JsonCommentData payload,
            @CurrentUser PersonDTO person) {
        payload.setAllowEmptyContent(false);

        return commentService.createComment(authorizationHeader, person, payload);
    }

    @ApiOperation("Returns the list of the root comments for the post")
    @GetMapping
    public Page<JsonComment> getComments(
            @RequestHeader("Authorization") String authorizationHeader,
            @ApiParam("The post id for filtering returned comments list")
            @RequestParam UUID postUuid,
            @RequestParam(required = false) Boolean showAll,
            Pageable page) {
        Long personId = securityProvider.getPersonIdFromAuthorization(authorizationHeader);
        String[] personRoles = securityProvider.getRolesFromAuthorization(authorizationHeader);
        boolean isAdmin = roleUtil.isAdmin(personRoles);

        return commentService.findComments(personId, isAdmin, postUuid, showAll, page);
    }

    @ApiOperation("Returns the list of the comments which has complaints")
    @GetMapping("/search/with-complaints")
    public Page<JsonComment> getCommentsWithComplaints(
            @RequestParam(required = false) Long groupId,
            @RequestParam(required = false, defaultValue = "PENDING") CommentComplaintState complainState,
            @RequestParam(required = false, defaultValue = "false") boolean deleted,
            @CurrentUser PersonDTO person,
            Pageable page) {

        return commentService.findCommentsWithComplaints(person, groupId, complainState, deleted, page);
    }

    @ApiOperation("Returns comment by id")
    @GetMapping("/{commentUuid}")
    public JsonComment getComment(
            @PathVariable UUID commentUuid,
            @CurrentUser PersonDTO person
    ) {
        return commentService.getCommentWithReplies(person.getPersonId(), commentUuid);
    }

    @ApiOperation("Updates comment content")
    @PutMapping("/{commentUuid}")
    public JsonComment editComment(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable UUID commentUuid,
            @RequestBody JsonCommentData data,
            @CurrentUser PersonDTO person
    ) {
        data.setAllowEmptyContent(false);

        return commentService.edit(authorizationHeader, person, commentUuid, data, null);
    }

    @ApiOperation("Reject comment by complaint")
    @PatchMapping("/complaints/{complaintUuid}/reject")
    public void rejectCommentByComplaint(
            @PathVariable UUID complaintUuid,
            @RequestParam("reason") @ApiParam("Reject Reason (text)") String reason,
            @CurrentUser PersonDTO person
    ) {

        commentService.rejectCommentByComplaint(person, reason, complaintUuid);
    }

    @ApiOperation("Remove comment by id")
    @DeleteMapping("/{commentUuid}")
    public void removeComment(
            @PathVariable UUID commentUuid,
            @CurrentUser PersonDTO person
    ) {

        commentService.removeComment(person, commentUuid);
    }

    @ApiOperation("Saves reply to the comment")
    @PostMapping("/{commentUuid}/reply")
    public JsonComment reply(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable UUID commentUuid,
            @RequestBody JsonCommentData data,
            @CurrentUser PersonDTO person) {
        data.setAllowEmptyContent(false);

        return commentService.reply(authorizationHeader, person, commentUuid, data, null);
    }

    @ApiOperation("Returns list of replies for the comment")
    @GetMapping("/{commentUuid}/replies")
    public Page<JsonComment> getReplies(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable UUID commentUuid,
            @RequestParam(required = false) Boolean showAll,
            Pageable page) {
        Long personId = securityProvider.getPersonIdFromAuthorization(authorizationHeader);
        String[] personRoles = securityProvider.getRolesFromAuthorization(authorizationHeader);
        boolean isAdmin = roleUtil.isAdmin(personRoles);

        return commentService.getReplies(personId, isAdmin, commentUuid, showAll, page);
    }
}
