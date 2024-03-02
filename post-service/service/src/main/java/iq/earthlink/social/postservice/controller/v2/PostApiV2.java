package iq.earthlink.social.postservice.controller.v2;

import io.micrometer.core.annotation.Timed;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import iq.earthlink.social.classes.data.dto.PostResponse;
import iq.earthlink.social.classes.enumeration.PostState;
import iq.earthlink.social.classes.enumeration.SortType;
import iq.earthlink.social.postservice.person.CurrentUser;
import iq.earthlink.social.postservice.person.PersonManager;
import iq.earthlink.social.postservice.person.dto.PersonDTO;
import iq.earthlink.social.postservice.post.BFFPostManager;
import iq.earthlink.social.postservice.post.PostComplaintState;
import iq.earthlink.social.postservice.post.PostSearchCriteria;
import iq.earthlink.social.postservice.post.rest.JsonPostData;
import iq.earthlink.social.postservice.post.rest.JsonUpdatePostData;
import iq.earthlink.social.security.SecurityProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Objects;

@Api(value = "PostApi", tags = "Post Api")
@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/api/v2/posts", produces = MediaType.APPLICATION_JSON_VALUE)
public class PostApiV2 {

    private final BFFPostManager postManager;
    private final SecurityProvider securityProvider;
    private final PersonManager personManager;
    private final RedisTemplate<String, Long> migrationFlag;

    @ApiOperation("Creates new post owned by the person or the group")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Timed("post.create.api")
    public PostResponse createPost(@RequestHeader("Authorization") String authorizationHeader,
                                   @ApiParam("The data contains post specific information")
                                   @RequestPart(value = "data") JsonPostData data,
                                   @ApiParam("The files contains media files that should be attached to the post. Optional.")
                                   @RequestPart(value = "files", required = false) MultipartFile[] files,
                                   @CurrentUser PersonDTO personDTO) {
        return postManager.createPost(authorizationHeader, personDTO, data, files);
    }

    @ApiOperation("Returns the posts list found by the provided criteria")
    @GetMapping
    public Page<PostResponse> findPosts(@ApiParam("The search term query that filters posts by title, content")
                                        @RequestParam(required = false) String query,
                                        @ApiParam("The group ids filters posts by the matched groups")
                                        @RequestParam(required = false) List<Long> groupIds,
                                        @ApiParam("Filters the posts by the provided states")
                                        @RequestParam(required = false) List<PostState> states,
                                        @ApiParam("Filters the posts by the pinned state")
                                        @RequestParam(required = false) Boolean pinned,
                                        @ApiParam("Filters the posts by the author ids")
                                        @RequestParam(required = false) List<Long> authorIds,
                                        @ApiParam(value = "Sort the posts by the sort type", example = "NEWEST | POPULAR | TRENDING | ALL")
                                        @RequestParam(required = false) SortType sortType,
                                        @CurrentUser PersonDTO currentUser,
                                        Pageable page) {

        PostSearchCriteria criteria = PostSearchCriteria.builder()
                .query(query)
                .groupIds(groupIds)
                .states(CollectionUtils.isEmpty(states) ? List.of(PostState.PUBLISHED) : states)
                .pinned(pinned)
                .authorIds(authorIds)
                .sortType(Objects.nonNull(sortType) ? sortType : SortType.ALL)
                .build();

        return postManager.findPosts(currentUser, criteria, page);
    }

    @ApiOperation("Returns the post found by provided id")
    @GetMapping("/{postId}")
    public PostResponse getPost(@PathVariable Long postId, @CurrentUser PersonDTO person) {

        return postManager.getPostById(person, postId);
    }

    @ApiOperation("Updates existing post")
    @PutMapping(path = "/{postId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public PostResponse updatePost(@RequestHeader("Authorization") String authorizationHeader,
                                   @PathVariable Long postId,
                                   @ApiParam("The data contains post specific information")
                                   @RequestPart(value = "data", required = false) JsonUpdatePostData data,
                                   @ApiParam("The files contains media files that should be attached to the post. Should be provided only for VIDEO, IMAGE posts.")
                                   @RequestPart(value = "files", required = false) MultipartFile[] files,
                                   @CurrentUser PersonDTO person) {

        return postManager.updatePost(authorizationHeader, person, postId, data, files);
    }

    @ApiOperation("Returns the list of posts with complaints")
    @GetMapping("/search/with-complaints")
    public Page<PostResponse> getPostsWithComplaints(@RequestHeader("Authorization") String authorizationHeader,
                                                     @RequestParam(required = false) Long groupId,
                                                     @RequestParam(required = false, defaultValue = "PENDING") PostComplaintState complaintState,
                                                     @RequestParam(required = false, defaultValue = "PUBLISHED") PostState postState,
                                                     @CurrentUser PersonDTO person,
                                                     Pageable page) {

        return postManager.findPostsWithComplaints(authorizationHeader, person, groupId, complaintState, postState, page);
    }


    @ApiOperation("Rejects post by complaint")
    @PatchMapping("/complaints/{complaintId}/reject")
    public PostResponse rejectPostByComplaint(@RequestHeader("Authorization") String authorizationHeader,
                                              @PathVariable Long complaintId,
                                              @RequestParam("reason") @ApiParam("Reject Reason (text)") String reason,
                                              @CurrentUser PersonDTO person) {

        return postManager.rejectPostByComplaint(authorizationHeader, person, reason, complaintId);
    }


    //todo: remove after deployment to the prod
    @ApiOperation("Sync post author uuid")
    @GetMapping("/sync/post-author")
    public void syncPostAuthorUUid() {
        personManager.updateAllPostAuthorUUID();
    }

    @ApiOperation("Trigger to push all published posts to kafka")
    @GetMapping("/sync/posts-published")
    public void triggerPostPublishedEvents(@CurrentUser PersonDTO personDTO) {
        if(personDTO.isAdmin()) {
            migrationFlag.opsForValue().set("postsPublishedMigrationFlag", 0L);
        }
    }
}
