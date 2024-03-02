package iq.earthlink.social.postservice.controller.v2;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import iq.earthlink.social.classes.data.dto.ComplaintRequest;
import iq.earthlink.social.postservice.post.PostManager;
import iq.earthlink.social.postservice.post.complaint.PostComplaintManager;
import iq.earthlink.social.postservice.post.model.Post;
import iq.earthlink.social.postservice.post.rest.JsonPostComplaint;
import iq.earthlink.social.security.SecurityProvider;
import org.springframework.web.bind.annotation.*;

@Api(value = "PostComplaintApi", tags = "Post Complaint Api")
@RestController
@RequestMapping("/api/v2/posts/{postId}")
public class PostComplaintApiV2 {

  private final PostComplaintManager manager;
  private final PostManager postManager;
  private final SecurityProvider securityProvider;

  public PostComplaintApiV2(PostComplaintManager manager,
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
      @RequestBody ComplaintRequest request
  ) {
    Long personId = securityProvider.getPersonIdFromAuthorization(authorizationHeader);

    Post post = postManager.getPost(postId);

    return manager.createComplaint(personId, post, request);
  }

  @ApiOperation("Updates the existent complaint")
  @PutMapping("/complaints/{complaintId}")
  public JsonPostComplaint updateComplaint(
      @RequestHeader("Authorization") String authorizationHeader,
      @PathVariable Long postId,
      @PathVariable Long complaintId,
      @RequestBody ComplaintRequest request
  ) {
    Long personId = securityProvider.getPersonIdFromAuthorization(authorizationHeader);

    return manager.updateComplaint(personId, postManager.getPost(postId), complaintId, request);
  }
}
