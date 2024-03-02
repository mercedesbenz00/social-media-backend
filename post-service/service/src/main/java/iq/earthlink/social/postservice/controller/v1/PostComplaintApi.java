package iq.earthlink.social.postservice.controller.v1;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import iq.earthlink.social.postservice.person.CurrentUser;
import iq.earthlink.social.postservice.person.dto.PersonDTO;
import iq.earthlink.social.postservice.post.PostManager;
import iq.earthlink.social.postservice.post.complaint.PostComplaintManager;
import iq.earthlink.social.postservice.post.rest.JsonPostComplaint;
import iq.earthlink.social.postservice.post.rest.JsonPostComplaintData;
import iq.earthlink.social.security.SecurityProvider;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@Api(value = "PostComplaintApi", tags = "Post Complaint Api")
@RestController
@RequestMapping("/api/v1/posts/{postId}")
public class PostComplaintApi {

    private final PostComplaintManager manager;
    private final PostManager postManager;
    private final SecurityProvider securityProvider;

    public PostComplaintApi(
            PostComplaintManager manager,
            PostManager postManager,
            SecurityProvider securityProvider) {
        this.manager = manager;
        this.postManager = postManager;
        this.securityProvider = securityProvider;
    }

    @ApiOperation("Creates complaint for the post")
    @PostMapping("/complaints")
    public JsonPostComplaint createComplaint(
            @PathVariable Long postId,
            @RequestHeader("Authorization") String authorizationHeader,
            @RequestBody @Valid JsonPostComplaintData data
    ) {
        Long personId = securityProvider.getPersonIdFromAuthorization(authorizationHeader);

        return manager.createComplaint(personId, postManager.getPost(postId), data);
    }

    @ApiOperation("Returns complaint found by id")
    @GetMapping("/complaints/{complaintId}")
    public JsonPostComplaint getComplaint(
            @PathVariable Long postId,
            @PathVariable Long complaintId,
            @RequestHeader("Authorization") String authorizationHeader
    ) {
        Long personId = securityProvider.getPersonIdFromAuthorization(authorizationHeader);

        return manager.getJsonPostComplaint(personId, postManager.getPost(postId), complaintId);
    }

    @ApiOperation("Returns complaints list for the post")
    @GetMapping("/complaints")
    public Page<JsonPostComplaint> getComplaints(
            @PathVariable Long postId,
            @CurrentUser PersonDTO person,
            Pageable page
    ) {
        return manager.findComplaints(person, postManager.getPost(postId), page);
    }

    @ApiOperation("Updates the existent complaint")
    @PutMapping("/complaints/{complaintId}")
    public JsonPostComplaint updateComplaint(
            @PathVariable Long postId,
            @PathVariable Long complaintId,
            @RequestHeader("Authorization") String authorizationHeader,
            @RequestBody @Valid JsonPostComplaintData data
    ) {
        Long personId = securityProvider.getPersonIdFromAuthorization(authorizationHeader);

        return manager.updateComplaint(personId, postManager.getPost(postId), complaintId, data);
    }

    @ApiOperation("Removes complaint by the id")
    @DeleteMapping("/complaints/{complaintId}")
    public void deleteComplaint(
            @PathVariable Long postId,
            @PathVariable Long complaintId,
            @RequestHeader("Authorization") String authorizationHeader
    ) {
        Long personId = securityProvider.getPersonIdFromAuthorization(authorizationHeader);
        manager.removeComplaint(personId, postManager.getPost(postId), complaintId);
    }

    @ApiOperation("Applies moderation on the complaint")
    @PatchMapping("/reject-complaints")
    public void rejectAllComplaints(
            @PathVariable Long postId,
            @RequestParam("reason") @ApiParam("Reject Reason (text)") String reason,
            @CurrentUser PersonDTO person
    ) {
        manager.rejectAllComplaints(person, reason, postId);
    }
}
