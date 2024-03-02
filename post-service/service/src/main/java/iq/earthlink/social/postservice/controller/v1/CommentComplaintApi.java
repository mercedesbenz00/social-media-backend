package iq.earthlink.social.postservice.controller.v1;

import io.swagger.annotations.*;
import iq.earthlink.social.postservice.person.CurrentUser;
import iq.earthlink.social.postservice.person.dto.PersonDTO;
import iq.earthlink.social.postservice.post.comment.complaint.CommentComplaint;
import iq.earthlink.social.postservice.post.comment.complaint.CommentComplaintManager;
import iq.earthlink.social.postservice.post.rest.JsonCommentComplaint;
import iq.earthlink.social.postservice.post.rest.JsonCommentComplaintData;
import iq.earthlink.social.security.SecurityProvider;
import org.dozer.Mapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.UUID;

@Api(value = "CommentComplaintApi", tags = "Comment Complaint Api")
@RestController
@RequestMapping("/api/v1/comments/{commentUuid}")
public class CommentComplaintApi {

    private final CommentComplaintManager manager;
    private final Mapper mapper;
    private final SecurityProvider securityProvider;

    public CommentComplaintApi(
            CommentComplaintManager manager,
            Mapper mapper,
            SecurityProvider securityProvider) {
        this.manager = manager;
        this.mapper = mapper;
        this.securityProvider = securityProvider;
    }

    @ApiOperation("Creates complaint for the comment")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Complaint successfully created"),
    })
    @PostMapping("/complaints")
    public JsonCommentComplaint createComplaint(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable UUID commentUuid,
            @RequestBody @Valid JsonCommentComplaintData data
    ) {
        Long personId = securityProvider.getPersonIdFromAuthorization(authorizationHeader);

        return manager.createComplaint(personId, commentUuid, data);
    }

    @ApiOperation("Returns comment's complaint")
    @GetMapping("/complaints/{complaintUuid}")
    public JsonCommentComplaint getComplaint(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable UUID commentUuid,
            @PathVariable UUID complaintUuid
    ) {
        Long personId = securityProvider.getPersonIdFromAuthorization(authorizationHeader);
        CommentComplaint complaint = manager
                .getComplaint(personId, commentUuid, complaintUuid);
        return mapper.map(complaint, JsonCommentComplaint.class);
    }

    @ApiOperation("Returns the list of complaints for the provided comment")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Returns the list of found complaints")
    })
    @GetMapping("/complaints")
    public Page<JsonCommentComplaint> getComplaints(
            @PathVariable UUID commentUuid,
            @CurrentUser PersonDTO person,
            Pageable page
    ) {
        return manager.findComplaints(person, commentUuid, page)
                .map(c -> mapper.map(c, JsonCommentComplaint.class));
    }

    @ApiOperation("Updates the existing comment complaint")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Complaint successfully updated")
    })
    @PutMapping("/complaints/{complaintUuid}")
    public JsonCommentComplaint updateComplaint(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable UUID commentUuid,
            @PathVariable UUID complaintUuid,
            @RequestBody @Valid JsonCommentComplaintData data
    ) {
        Long personId = securityProvider.getPersonIdFromAuthorization(authorizationHeader);
        CommentComplaint commentComplaint = manager
                .updateComplaint(personId, commentUuid, complaintUuid, data);

        return mapper.map(commentComplaint, JsonCommentComplaint.class);
    }

    @ApiOperation("Removes the comment's complaint")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Complaint successfully removed")
    })
    @DeleteMapping("/complaints/{complaintUuid}")
    public void deleteComplaint(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable UUID commentUuid,
            @PathVariable UUID complaintUuid
    ) {
        Long personId = securityProvider.getPersonIdFromAuthorization(authorizationHeader);
        manager.removeComplaint(personId, commentUuid, complaintUuid);
    }

    @ApiOperation("Rejects all comment complaints")
    @PatchMapping("/reject-complaints")
    public void rejectAllComplaints(
            @PathVariable UUID commentUuid,
            @RequestParam("reason") @ApiParam("Reject Reason (text)") String reason,
            @CurrentUser PersonDTO person) {

        manager.rejectAllComplaints(person, reason, commentUuid);
    }
}
