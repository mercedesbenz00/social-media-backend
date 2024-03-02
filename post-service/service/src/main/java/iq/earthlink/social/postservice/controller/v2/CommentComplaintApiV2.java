package iq.earthlink.social.postservice.controller.v2;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import iq.earthlink.social.classes.data.dto.ComplaintRequest;
import iq.earthlink.social.postservice.post.comment.complaint.CommentComplaint;
import iq.earthlink.social.postservice.post.comment.complaint.CommentComplaintManager;
import iq.earthlink.social.postservice.post.rest.JsonCommentComplaint;
import iq.earthlink.social.security.SecurityProvider;
import org.dozer.Mapper;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Api(value = "CommentComplaintApi", tags = "Comment Complaint Api")
@RestController
@RequestMapping("/api/v2/comments/{commentUuid}")
public class CommentComplaintApiV2 {

    private final CommentComplaintManager manager;
    private final Mapper mapper;
    private final SecurityProvider securityProvider;

    public CommentComplaintApiV2(
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
            @RequestBody ComplaintRequest request
    ) {
        Long personId = securityProvider.getPersonIdFromAuthorization(authorizationHeader);

        return manager.createComplaint(personId, commentUuid, request);
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
            @RequestBody ComplaintRequest request
    ) {
        Long personId = securityProvider.getPersonIdFromAuthorization(authorizationHeader);
        CommentComplaint commentComplaint = manager
                .updateComplaint(personId, commentUuid, complaintUuid, request);

        return mapper.map(commentComplaint, JsonCommentComplaint.class);
    }
}
