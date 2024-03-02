package iq.earthlink.social.postservice.controller.v1;

import io.swagger.annotations.*;
import iq.earthlink.social.classes.data.validation.NewEntityGroup;
import iq.earthlink.social.postservice.person.CurrentUser;
import iq.earthlink.social.postservice.person.dto.PersonDTO;
import iq.earthlink.social.postservice.post.CollectionPostsSearchCriteria;
import iq.earthlink.social.postservice.post.PostManager;
import iq.earthlink.social.postservice.post.collection.GroupPostCollection;
import iq.earthlink.social.postservice.post.collection.GroupPostCollectionSearchCriteria;
import iq.earthlink.social.postservice.post.collection.GroupPostCollectionService;
import iq.earthlink.social.postservice.post.rest.JsonGroupPostCollection;
import iq.earthlink.social.postservice.post.rest.JsonGroupPostCollectionData;
import iq.earthlink.social.postservice.post.rest.JsonPost;
import org.dozer.Mapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Api(value = "GroupPostCollectionApi", tags = "Group Post Collection Api")
@RestController
@RequestMapping("/api/v1/group-post-collections")
public class GroupPostCollectionApi {

    private final GroupPostCollectionService groupPostCollectionService;
    private final PostManager postManager;
    private final Mapper mapper;

    public GroupPostCollectionApi(
            GroupPostCollectionService groupPostCollectionService,
            PostManager postManager, Mapper mapper) {
        this.groupPostCollectionService = groupPostCollectionService;
        this.postManager = postManager;
        this.mapper = mapper;
    }

    @ApiOperation("Creates new group post collection")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Group post collection successfully created"),
            @ApiResponse(code = 403, message = "User does not have permissions. Only group moderator/admin allowed create collections.")
    })
    @PostMapping
    public JsonGroupPostCollection create(
            @Validated(NewEntityGroup.class) @RequestBody JsonGroupPostCollectionData data,
            @CurrentUser PersonDTO person) {

        return mapper.map(groupPostCollectionService.createGroupCollection(person, data),
                JsonGroupPostCollection.class);
    }

    @ApiOperation("Returns the list of the group collections found by criteria")
    @GetMapping
    public Page<JsonGroupPostCollection> findGroupCollections(

            @ApiParam("Filters collections list by the owner group id")
            @RequestParam(value = "groupId", required = false) Long groupId,

            @ApiParam(value = "Filters collections by the person creator", hidden = true)
            @RequestParam(value = "personId", required = false) Long personId,

            @ApiParam("Filters collections by name matching the query pattern")
            @RequestParam(value = "query", required = false) String query,
            Pageable page) {

        GroupPostCollectionSearchCriteria criteria = GroupPostCollectionSearchCriteria.builder()
                .groupId(groupId)
                .personId(personId)
                .query(query)
                .build();

        return groupPostCollectionService.findGroupCollections(criteria, page)
                .map(c -> mapper.map(c, JsonGroupPostCollection.class));
    }

    @ApiOperation("Returns the group post collection by id")
    @GetMapping("/{groupCollectionId}")
    public JsonGroupPostCollection getGroupCollection(@PathVariable Long groupCollectionId) {
        return mapper.map(groupPostCollectionService.getGroupCollection(groupCollectionId),
                JsonGroupPostCollection.class);
    }

    @ApiOperation("Removes the group post collection by id")
    @DeleteMapping("/{groupCollectionId}")
    public void removeGroupCollection(
            @PathVariable Long groupCollectionId,
            @CurrentUser PersonDTO person) {

        groupPostCollectionService.removeGroupCollection(person, groupCollectionId);
    }

    @ApiOperation("Returns the list of posts that added to the provided group collection")
    @GetMapping("/{groupCollectionId}/posts")
    public Page<JsonPost> getGroupCollectionPosts(
            @RequestHeader("Authorization") String authorizationHeader,
            @ApiParam("The Id of the group post collection")
            @PathVariable Long groupCollectionId,
            @ApiParam("The search term query that filters posts by content")
            @RequestParam(required = false) String query,
            Pageable page
    ) {
        CollectionPostsSearchCriteria criteria = CollectionPostsSearchCriteria.builder()
                .query(query)
                .build();

        return groupPostCollectionService.getGroupCollectionPosts(groupCollectionId, criteria, page)
                .map(c -> mapper.map(c, JsonPost.class));
    }

    @ApiOperation("Adds the post to the provided group post collection")
    @ApiResponses({
            @ApiResponse(code = 200, message = "The post successfully added to the collection")
    })
    @PutMapping("/{groupCollectionId}/posts/{postId}")
    public JsonGroupPostCollection addPost(
            @PathVariable Long groupCollectionId,
            @PathVariable Long postId,
            @CurrentUser PersonDTO person) {
        GroupPostCollection col = groupPostCollectionService
                .addPost(person, postManager.getPost(postId), groupCollectionId);

        return mapper.map(col, JsonGroupPostCollection.class);
    }

    @ApiOperation("Removes the post from the group post collection")
    @ApiResponses({
            @ApiResponse(code = 200, message = "The post successfully removed from the collection")
    })
    @DeleteMapping("/{groupCollectionId}/posts/{postId}")
    public JsonGroupPostCollection removePost(
            @PathVariable Long groupCollectionId,
            @PathVariable Long postId,
            @CurrentUser PersonDTO person) {

        GroupPostCollection col = groupPostCollectionService
                .removePost(person, postManager.getPost(postId), groupCollectionId);

        return mapper.map(col, JsonGroupPostCollection.class);
    }

}
