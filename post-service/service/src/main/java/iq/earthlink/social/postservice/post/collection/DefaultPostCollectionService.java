package iq.earthlink.social.postservice.post.collection;

import iq.earthlink.social.classes.enumeration.PostState;
import iq.earthlink.social.exception.BadRequestException;
import iq.earthlink.social.exception.ForbiddenException;
import iq.earthlink.social.exception.NotFoundException;
import iq.earthlink.social.exception.NotUniqueException;
import iq.earthlink.social.postservice.person.dto.PersonDTO;
import iq.earthlink.social.postservice.post.CollectionPostsSearchCriteria;
import iq.earthlink.social.postservice.post.PostCollectionData;
import iq.earthlink.social.postservice.post.collection.repository.PostCollectionPostsRepository;
import iq.earthlink.social.postservice.post.collection.repository.PostCollectionRepository;
import iq.earthlink.social.postservice.post.model.Post;
import iq.earthlink.social.postservice.post.rest.JsonPost;
import iq.earthlink.social.postservice.post.rest.JsonPostCollection;
import iq.earthlink.social.postservice.util.PostStatisticsAndMediaUtil;
import org.dozer.Mapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static iq.earthlink.social.common.util.Preconditions.checkNotNull;

@Service
public class DefaultPostCollectionService implements PostCollectionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultPostCollectionService.class);
    private static final String ERROR_CHECK_NOT_NULL = "error.check.not.null";
    private static final String ERROR_NOT_FOUND_COLLECTION = "error.not.found.collection";
    private static final String COLLECTION_ID = "collectionId";
    private static final String PERSON = "person";
    private static final String PERSON_ID = "personId";

    private final PostCollectionRepository repository;
    private final PostCollectionPostsRepository postCollectionPostsRepository;
    private final PostStatisticsAndMediaUtil postStatisticsAndMediaUtil;
    private final Mapper mapper;

    public DefaultPostCollectionService(
            PostCollectionRepository repository,
            PostCollectionPostsRepository postCollectionPostsRepository,
            PostStatisticsAndMediaUtil postStatisticsAndMediaUtil,
            Mapper mapper) {
        this.repository = repository;
        this.postCollectionPostsRepository = postCollectionPostsRepository;
        this.postStatisticsAndMediaUtil = postStatisticsAndMediaUtil;
        this.mapper = mapper;
    }

    @Transactional
    @Nonnull
    @Override
    public PostCollection createCollection(
            @Nonnull Long personId,
            @Nonnull PostCollectionData data) {

        PostCollection c = new PostCollection();
        c.setOwnerId(personId);
        c.setName(data.getName());
        c.setPublic(data.getIsPublic());

        PostCollection collection = repository.save(c);
        LOGGER.info("Person: {} created new collection: {}", personId, collection);
        return collection;
    }

    @Transactional
    @Nonnull
    @Override
    public PostCollection updateCollection(
            @Nonnull Long personId,
            Long collectionId,
            @Nonnull PostCollectionData data) {
        PostCollection postCollection = repository.findById(collectionId)
                .orElseThrow(() -> new NotFoundException(ERROR_NOT_FOUND_COLLECTION, collectionId));

        checkPermission(personId, postCollection);

        if (!data.getName().isEmpty() && data.getName() != null){
            postCollection.setName(data.getName());
        }
        if (data.getIsPublic() != null){
            postCollection.setPublic(data.getIsPublic());
        }

        PostCollection collection = repository.save(postCollection);
        LOGGER.info("Person: {} created new collection: {}", personId, collection);
        return collection;
    }

    @Nonnull
    @Override
    public PostCollection getCollection(Long personId, @Nonnull Long collectionId) {
        checkNotNull(personId, ERROR_CHECK_NOT_NULL, PERSON_ID);
        checkNotNull(collectionId, ERROR_CHECK_NOT_NULL, COLLECTION_ID);

        PostCollection postCollection = repository.findById(collectionId)
                .orElseThrow(() -> new NotFoundException(ERROR_NOT_FOUND_COLLECTION, collectionId));

        if (!postCollection.isPublic()) {
            checkPermission(personId, postCollection);
        }
        return postCollection;
    }

    @Transactional
    @Override
    public void addPost(@Nonnull Long personId,
                        @Nonnull Post post,
                        @Nonnull Long collectionId) {
        checkNotNull(personId, ERROR_CHECK_NOT_NULL, PERSON_ID);
        checkNotNull(post, ERROR_CHECK_NOT_NULL, "post");
        checkNotNull(collectionId, ERROR_CHECK_NOT_NULL, COLLECTION_ID);

        if (post.getState() == PostState.PUBLISHED) {
            List<PostCollectionPost> collectionsByPostId = postCollectionPostsRepository.findByPostId(post.getId());
            Optional<PostCollectionPost> collectionByPostId = collectionsByPostId.stream()
                    .filter(p -> p.getPostCollection().getOwnerId().equals(personId))
                    .findFirst();

            if (collectionByPostId.isPresent()) {
                if (Objects.equals(collectionByPostId.get().getPostCollection().getId(), collectionId)) {
                    throw new NotUniqueException("error.post.already.added.to.collection");
                } else {
                    throw new NotUniqueException("error.post.already.added.to.another.collection");
                }
            }
            PostCollection postCollection = getCollection(personId, collectionId);
            if (Objects.equals(postCollection.getOwnerId(), personId)) {
                PostCollectionPost postCollectionPost = PostCollectionPost.builder()
                        .postCollection(postCollection)
                        .post(post)
                        .build();
                postCollectionPostsRepository.save(postCollectionPost);
                LOGGER.info("Person: {} added new post: {} to the collection: {}",
                        personId, post.getId(), collectionId);
            } else {
                throw new ForbiddenException("error.operation.not.permitted");
            }
        } else {
            throw new BadRequestException("error.invalid.post.status.to.add.collection");
        }
    }

    @Transactional
    @Override
    public void removePost(@Nonnull Long personId, Post post, Long collectionId) {
        checkNotNull(personId, ERROR_CHECK_NOT_NULL, PERSON_ID);
        checkNotNull(post, ERROR_CHECK_NOT_NULL, "post");
        checkNotNull(collectionId, ERROR_CHECK_NOT_NULL, COLLECTION_ID);

        PostCollection postCollection = getCollection(personId, collectionId);
        if (Objects.equals(postCollection.getOwnerId(), personId)) {
            postCollectionPostsRepository.deleteByPostCollectionIdAndPostId(collectionId, post.getId());
            LOGGER.info("Person: {} removed post: {} from the collection: {}",
                    personId, post.getId(), collectionId);
        } else {
            throw new ForbiddenException("error.person.can.not.delete.post.from.collection");
        }
    }

    @Nonnull
    @Override
    public Page<JsonPost> getCollectionPosts(@Nonnull Long personId,
                                             Long collectionId,
                                             @Nonnull CollectionPostsSearchCriteria criteria,
                                             Pageable page) {

        checkNotNull(personId, ERROR_CHECK_NOT_NULL, PERSON_ID);
        checkNotNull(collectionId, ERROR_CHECK_NOT_NULL, COLLECTION_ID);
        checkNotNull(page, ERROR_CHECK_NOT_NULL, "page");
        checkNotNull(criteria, ERROR_CHECK_NOT_NULL, "criteria");

        PostCollection collection = getCollection(personId, collectionId);
        checkPermission(personId, collection);

        Page<Post> posts = postCollectionPostsRepository.findPosts(collectionId, criteria, page);

        return posts.map(post -> {
            JsonPost jsonPost = mapper.map(post, JsonPost.class);
            return postStatisticsAndMediaUtil.enrichWithStatisticsAndMedia(personId, jsonPost);
        });
    }

    @Transactional
    @Override
    public void removeCollection(@Nonnull PersonDTO person, Long collectionId) {
        checkNotNull(person, ERROR_CHECK_NOT_NULL, PERSON);
        checkNotNull(collectionId, ERROR_CHECK_NOT_NULL, COLLECTION_ID);

        PostCollection postCollection = getCollection(person.getPersonId(), collectionId);

        if (Objects.equals(person.getPersonId(), postCollection.getOwnerId()) || person.isAdmin()) {
            postCollectionPostsRepository.deleteByPostCollectionId(collectionId);
            repository.delete(postCollection);
        } else {
            throw new ForbiddenException("error.person.can.no.delete.collection");
        }
    }

    @Nonnull
    @Override
    public Page<JsonPostCollection> findCollections(@Nonnull PostCollectionSearchCriteria criteria, @Nonnull Pageable page) {
        checkNotNull(criteria, ERROR_CHECK_NOT_NULL, PERSON);
        checkNotNull(page, ERROR_CHECK_NOT_NULL, "page");

        Page<PostCollection> allPostCollections = repository.findCollections(criteria, page);
        List<JsonPostCollection> jsonPostCollections = new ArrayList<>();

        for (PostCollection postCollection : allPostCollections) {
            List<PostCollectionPost> limitedPostCollectionPosts = postCollectionPostsRepository.findLimitedPostsForCollection(
                    postCollection, PageRequest.of(0, 4));

            List<JsonPost> limitedPosts = limitedPostCollectionPosts.stream()
                    .map(PostCollectionPost::getPost)
                    .map(post -> mapper.map(post, JsonPost.class))
                    .map(post -> postStatisticsAndMediaUtil.enrichWithStatisticsAndMedia(criteria.getPersonId(), post))
                    .toList();

            JsonPostCollection jsonPostCollection = mapper.map(postCollection, JsonPostCollection.class);
            jsonPostCollection.setPosts(limitedPosts);
            jsonPostCollections.add(jsonPostCollection);
        }

        return new PageImpl<>(jsonPostCollections, page, allPostCollections.getTotalElements());
    }

    @Override
    public JsonPostCollection findMyCollectionByPostId(Long personId, Long postId) {
        List<Long> collectionIdsByPostId = postCollectionPostsRepository.findCollectionIdByPostId(postId);

        if (collectionIdsByPostId.isEmpty()) {
            throw new NotFoundException("error.not.found.collection.by.post.id");
        }

        Optional<PostCollection> postCollectionOptional = repository.findByIdInAndOwnerId(collectionIdsByPostId, personId);
        return postCollectionOptional.map(postCollection -> mapper.map(postCollection, JsonPostCollection.class))
                .orElseThrow(() -> new NotFoundException("error.not.found.collection.by.post.id"));
    }

    private void checkPermission(Long personId, PostCollection collection) {
        boolean isOwner = Objects.equals(personId, collection.getOwnerId());
        if (!isOwner) {
            throw new ForbiddenException("error.operation.not.permitted");
        }
    }
}
