package iq.earthlink.social.postservice.controller.v1;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import iq.earthlink.social.classes.data.validation.NewEntityGroup;
import iq.earthlink.social.postservice.person.CurrentUser;
import iq.earthlink.social.postservice.person.dto.PersonDTO;
import iq.earthlink.social.postservice.post.CollectionPostsSearchCriteria;
import iq.earthlink.social.postservice.post.PostManager;
import iq.earthlink.social.postservice.post.collection.PostCollectionSearchCriteria;
import iq.earthlink.social.postservice.post.collection.PostCollectionService;
import iq.earthlink.social.postservice.post.rest.JsonPost;
import iq.earthlink.social.postservice.post.rest.JsonPostCollection;
import iq.earthlink.social.postservice.post.rest.JsonPostCollectionData;
import iq.earthlink.social.security.DefaultSecurityProvider;
import org.dozer.Mapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Api(value = "PostCollectionApi", tags = "Post Collection Api")
@RestController
@RequestMapping("/api/v1/post-collections")
public class PostCollectionApi {

    private final PostCollectionService postCollectionService;
    private final PostManager postManager;
    private final Mapper mapper;
    private final DefaultSecurityProvider securityProvider;

    public PostCollectionApi(
            PostCollectionService postCollectionService,
            PostManager postManager, Mapper mapper,
            DefaultSecurityProvider securityProvider) {
        this.postCollectionService = postCollectionService;
        this.postManager = postManager;
        this.mapper = mapper;
        this.securityProvider = securityProvider;
    }

    @ApiOperation("Creates the personal posts collection")
    @PostMapping
    public JsonPostCollection create(
            @RequestHeader("Authorization") String authorizationHeader,
            @Validated(NewEntityGroup.class) @RequestBody JsonPostCollectionData data
    ) {
        Long personId = securityProvider.getPersonIdFromAuthorization(authorizationHeader);

        return mapper.map(postCollectionService.createCollection(personId, data), JsonPostCollection.class);
    }

    @ApiOperation("Update the personal posts collection")
    @PutMapping("{collectionId}")
    public JsonPostCollection update(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable Long collectionId,
            @Validated(NewEntityGroup.class) @RequestBody JsonPostCollectionData data
    ) {
        Long personId = securityProvider.getPersonIdFromAuthorization(authorizationHeader);

        return mapper.map(postCollectionService.updateCollection(personId, collectionId, data), JsonPostCollection.class);
    }

    @ApiOperation("Returns the list of persons post collections matched by criteria")
    @GetMapping
    public Page<JsonPostCollection> findCollections(
            @RequestHeader("Authorization") String authorizationHeader,
            @ApiParam("Filters person collections by matched collection name")
            @RequestParam(value = "query", required = false) String query,
            Pageable page) {
        Long personId = securityProvider.getPersonIdFromAuthorization(authorizationHeader);

        PostCollectionSearchCriteria criteria = PostCollectionSearchCriteria.builder()
                .personId(personId)
                .query(query)
                .build();

        return postCollectionService.findCollections(criteria, page)
                .map(c -> mapper.map(c, JsonPostCollection.class));
    }

    @ApiOperation("Returns the user's post collection that contains provided post")
    @GetMapping("/my-collection-by-post-id")
    public JsonPostCollection findMyCollectionByPostId(
            @RequestHeader("Authorization") String authorizationHeader,
            @ApiParam("Filters the post collections by the post included in collection")
            @RequestParam(value = "postId") Long postId) {
        Long personId = securityProvider.getPersonIdFromAuthorization(authorizationHeader);

        return postCollectionService.findMyCollectionByPostId(personId, postId);
    }

    @ApiOperation("Returns the post collection found by the id")
    @GetMapping("/{collectionId}")
    public JsonPostCollection getCollection(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable Long collectionId) {
        Long personId = securityProvider.getPersonIdFromAuthorization(authorizationHeader);

        return mapper.map(postCollectionService.getCollection(personId, collectionId), JsonPostCollection.class);
    }

    @ApiOperation("Returns the posts list that belongs to the collection")
    @GetMapping("/{collectionId}/posts")
    public Page<JsonPost> getCollectionPosts(
            @RequestHeader("Authorization") String authorizationHeader,
            @ApiParam("The Id of the post collection")
            @PathVariable Long collectionId,
            @ApiParam("Filters the post collections by the post included in collection")
            @RequestParam(value = "postId", required = false) Long postId,
            @ApiParam("The search term query that filters posts by content")
            @RequestParam(required = false) String query,
            Pageable page
    ) {
        Long personId = securityProvider.getPersonIdFromAuthorization(authorizationHeader);

        CollectionPostsSearchCriteria criteria = CollectionPostsSearchCriteria.builder()
                .postId(postId)
                .query(query)
                .build();

        return postCollectionService.getCollectionPosts(personId, collectionId, criteria, page);
    }

    @ApiOperation("Adds new post to the collection")
    @PutMapping("/{collectionId}/posts/{postId}")
    public void addPost(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable Long collectionId,
            @PathVariable Long postId
    ) {
        Long personId = securityProvider.getPersonIdFromAuthorization(authorizationHeader);
        postCollectionService.addPost(personId, postManager.getPost(postId), collectionId);
    }

    @ApiOperation("Removes the post from the collection")
    @DeleteMapping("/{collectionId}/posts/{postId}")
    public void removePost(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable Long collectionId,
            @PathVariable Long postId
    ) {
        Long personId = securityProvider.getPersonIdFromAuthorization(authorizationHeader);
        postCollectionService.removePost(personId, postManager.getPost(postId), collectionId);
    }

    @ApiOperation("Removes collection. Only owner can remove collection")
    @DeleteMapping("/{collectionId}")
    public void removeCollection(
            @PathVariable Long collectionId,
            @CurrentUser PersonDTO person) {

        postCollectionService.removeCollection(person, collectionId);
    }
}
