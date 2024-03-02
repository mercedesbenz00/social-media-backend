package iq.earthlink.social.postservice.controller.v1;

import io.micrometer.core.annotation.Timed;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import iq.earthlink.social.classes.enumeration.PostState;
import iq.earthlink.social.classes.enumeration.SortType;
import iq.earthlink.social.common.file.MediaFile;
import iq.earthlink.social.exception.NotFoundException;
import iq.earthlink.social.postservice.person.CurrentUser;
import iq.earthlink.social.postservice.person.dto.PersonDTO;
import iq.earthlink.social.postservice.post.PostComplaintState;
import iq.earthlink.social.postservice.post.PostManager;
import iq.earthlink.social.postservice.post.PostMediaService;
import iq.earthlink.social.postservice.post.PostSearchCriteria;
import iq.earthlink.social.postservice.post.rest.*;
import iq.earthlink.social.postservice.post.statistics.PostStatisticsManager;
import iq.earthlink.social.postservice.util.RoleUtil;
import iq.earthlink.social.security.SecurityProvider;
import org.dozer.Mapper;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Objects;

@Api(value = "PostApi", tags = "Post Api")
@RestController
@RequestMapping(value = "/api/v1/posts", produces = MediaType.APPLICATION_JSON_VALUE)
public class PostApi {

    private final PostManager postManager;
    private final PostStatisticsManager postStatisticsManager;
    private final Mapper mapper;
    private final PostMediaService mediaService;
    private final SecurityProvider securityProvider;
    private final RoleUtil roleUtil;

    public PostApi(
            Mapper mapper,
            PostManager postManager,
            PostStatisticsManager postStatisticsManager,
            PostMediaService postMediaService, SecurityProvider securityProvider, RoleUtil roleUtil) {
        this.postManager = postManager;
        this.postStatisticsManager = postStatisticsManager;
        this.mapper = mapper;
        this.mediaService = postMediaService;
        this.securityProvider = securityProvider;
        this.roleUtil = roleUtil;
    }

    @ApiOperation("Creates new post owned by the person or the group")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Timed("post.create.api")
    public JsonPost createPost(
            @RequestHeader("Authorization") String authorizationHeader,

            //TODO: this param is not shown on the Swagger UI due to a bug: https://github.com/springfox/springfox/issues/3464
            @ApiParam("The data contains post specific information")
            @RequestPart(value = "data") JsonPostData data,
            @ApiParam("The files contains media files that should be attached to the post. Optional.")
            @RequestPart(value = "files", required = false) MultipartFile[] files,
            @CurrentUser PersonDTO personDTO) {

        return mapper.map(postManager.createPostDeprecated(authorizationHeader, personDTO, data, files), JsonPost.class);
    }

    @ApiOperation("Returns the posts list found by the provided criteria")
    @GetMapping
    public Page<JsonPost> findPosts(
            @RequestHeader("Authorization") String authorizationHeader,
            @ApiParam("The search term query that filters posts by title, content")
            @RequestParam(required = false) String query,
            @ApiParam("The group ids filters posts by the matched groups")
            @RequestParam(required = false) List<Long> groupIds,
            @ApiParam("Filters the posts by the posts ids")
            @RequestParam(required = false) List<Long> postIds,
            @ApiParam("Filters the posts by the provided states")
            @RequestParam(required = false) List<PostState> states,
            @ApiParam("Filters the posts by the pinned state")
            @RequestParam(required = false) Boolean pinned,
            @ApiParam("Filters the posts by the author ids")
            @RequestParam(required = false) List<Long> authorIds,
            @ApiParam(value = "Sort the posts by the sort type", example = "NEWEST | POPULAR | TRENDING | ALL")
            @RequestParam(required = false) SortType sortType,
            Pageable page) {
        Long personId = securityProvider.getPersonIdFromAuthorization(authorizationHeader);

        PostSearchCriteria criteria = PostSearchCriteria.builder()
                .query(query)
                .groupIds(groupIds)
                .postIds(postIds)
                .states(states)
                .pinned(pinned)
                .authorIds(authorIds)
                .sortType(Objects.nonNull(sortType) ? sortType : SortType.ALL)
                .build();

        return postManager.findPostsDeprecated(personId, criteria, page);
    }

    @ApiOperation("Returns the post found by provided id")
    @GetMapping("/{postId}")
    public JsonPost getPost(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable Long postId) {
        Long personId = securityProvider.getPersonIdFromAuthorization(authorizationHeader);
        String[] personRoles = securityProvider.getRolesFromAuthorization(authorizationHeader);
        boolean isAdmin = roleUtil.isAdmin(personRoles);

        return postManager.getPost(personId, isAdmin, postId);
    }

    @ApiOperation("Updates existing post")
    @PutMapping(path = "/{postId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public JsonPost updatePost(
            @RequestHeader("Authorization") String authorizationHeader,

            @PathVariable Long postId,

            @ApiParam("The data contains post specific information")
            @RequestPart(value = "data", required = false) JsonUpdatePostData data,

            @ApiParam("The files contains media files that should be attached to the post. Should be provided only for VIDEO, IMAGE posts.")
            @RequestPart(value = "files", required = false) MultipartFile[] files,
            @CurrentUser PersonDTO person) {

        return postManager.updatePostDeprecated(authorizationHeader, person, postId, data, files);
    }

    @ApiOperation("Removes the post. Post can be removed by the author, admin or group moderator/admin")
    @DeleteMapping("/{postId}")
    public void deletePost(@PathVariable Long postId,
                           @CurrentUser PersonDTO person) {

        postManager.removePost(person, postId);
    }

    @ApiOperation("Returns the list of posts with complaints")
    @GetMapping("/search/with-complaints")
    public Page<JsonPost> getPostsWithComplaints(
            @RequestHeader("Authorization") String authorizationHeader,
            @RequestParam(required = false) Long groupId,
            @RequestParam(required = false, defaultValue = "PENDING") PostComplaintState complaintState,
            @RequestParam(required = false, defaultValue = "PUBLISHED") PostState postState,
            @CurrentUser PersonDTO person,
            Pageable page) {

        return postManager.findPostsWithComplaintsDeprecated(authorizationHeader, person, groupId, complaintState, postState, page);
    }

    @ApiOperation("Returns the media post file content")
    @GetMapping("/{postId}/files/{fileId}/download")
    public ResponseEntity<Resource> downloadPostFile(
            @RequestHeader(value = "Range", required = false) String rangeHeader,
            @PathVariable Long postId,
            @PathVariable Long fileId) {

        MediaFile file = mediaService.findPostFile(postId, fileId)
                .orElseThrow(() -> new NotFoundException("error.not.found.file.for.post", fileId, postId));

        return mediaService.downloadPostFile(file, rangeHeader);
    }

    @ApiOperation("Delete media file of the post")
    @DeleteMapping("/{postId}/files/{fileId}")
    public void deletePostMediaFile(
            @PathVariable Long postId,
            @PathVariable Long fileId,
            @CurrentUser PersonDTO person) {
        postManager.removePostFile(person, postId, fileId);
    }

    @ApiOperation("Rejects post by complaint")
    @PatchMapping("/complaints/{complaintId}/reject")
    public JsonPost rejectPostByComplaint(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable Long complaintId,
            @RequestParam("reason") @ApiParam("Reject Reason (text)") String reason,
            @CurrentUser PersonDTO person) {

        return mapper.map(postManager.rejectPostByComplaintDeprecated(authorizationHeader, person, reason, complaintId), JsonPost.class);
    }

    @ApiOperation("Synchronize post statistics")
    @PatchMapping("/stats/sync")
    public void synchronizePostStatistics(@CurrentUser PersonDTO person) {

        postStatisticsManager.synchronizePostStatistics(person);
    }

    @ApiOperation("Compare post statistics with actual data")
    @GetMapping("/stats/compare")
    public List<PostStatisticsGap> comparePostStatistics(@CurrentUser PersonDTO person) {
        return postStatisticsManager.comparePostStatistics(person);
    }

    @ApiOperation("Get post/comment complaint stats for the person")
    @GetMapping("/complaint-stats")
    public ComplaintStatsDTO getPersonComplaintStats(
            @RequestParam("personId") Long personId,
            @CurrentUser PersonDTO currentUser) {
        return postStatisticsManager.getComplaintStatsByPerson(currentUser, personId);
    }

    @ApiOperation("Get group statistics info")
    @GetMapping("/group-stats")
    public GroupStatsDTO getGroupStats(
            @RequestParam("groupId") Long groupId,
            @CurrentUser PersonDTO currentUser) {
        return postStatisticsManager.getGroupStats(currentUser, groupId);
    }

    @ApiOperation("Removes the post's LinkMeta. The LinkMeta of a post can be removed by the author, admin or group moderator/admin")
    @DeleteMapping("/linkMeta/{postId}")
    public void deleteLinkMeta(@PathVariable Long postId,
                               @CurrentUser PersonDTO person) {
        postManager.removeLinkMeta(person, postId);
    }
}
