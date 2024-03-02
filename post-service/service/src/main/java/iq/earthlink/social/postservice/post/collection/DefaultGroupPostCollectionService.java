package iq.earthlink.social.postservice.post.collection;

import iq.earthlink.social.exception.ForbiddenException;
import iq.earthlink.social.exception.NotFoundException;
import iq.earthlink.social.postservice.person.dto.PersonDTO;
import iq.earthlink.social.postservice.post.CollectionPostsSearchCriteria;
import iq.earthlink.social.postservice.post.GroupPostCollectionData;
import iq.earthlink.social.postservice.post.collection.repository.GroupPostCollectionRepository;
import iq.earthlink.social.postservice.post.model.Post;
import iq.earthlink.social.postservice.util.PermissionUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Nonnull;

import static iq.earthlink.social.common.util.Preconditions.checkNotNull;

@Service
@Transactional
public class DefaultGroupPostCollectionService implements GroupPostCollectionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultGroupPostCollectionService.class);
    private static final String ERROR_CHECK_NOT_NULL = "error.check.not.null";
    private static final String GROUP_COLLECTION_ID = "groupCollectionId";
    private static final String PERSON = "person";

    private final GroupPostCollectionRepository repository;
    private final PermissionUtil permissionUtil;

    public DefaultGroupPostCollectionService(
            GroupPostCollectionRepository repository,
            PermissionUtil permissionUtil) {
        this.repository = repository;
        this.permissionUtil = permissionUtil;
    }

    @Nonnull
    @Override
    public GroupPostCollection createGroupCollection(
            @Nonnull PersonDTO person,
            @Nonnull GroupPostCollectionData data) {
        checkManageGroupCollectionsPermissions(person, data.getGroupId());

        GroupPostCollection c = new GroupPostCollection();
        c.setGroupId(data.getGroupId());
        c.setAuthorId(person.getPersonId());
        c.setName(data.getName());
        c.setDefaultCollection(data.isDefaultCollection());

        return repository.save(c);
    }

    @Nonnull
    @Override
    public GroupPostCollection getGroupCollection(@Nonnull Long groupCollectionId) {
        checkNotNull(groupCollectionId, ERROR_CHECK_NOT_NULL, GROUP_COLLECTION_ID);

        return repository.findById(groupCollectionId)
                .orElseThrow(() -> new NotFoundException("error.not.found.collection", groupCollectionId));
    }

    @Override
    public void removeGroupCollection(@Nonnull PersonDTO person, Long groupCollectionId) {
        checkNotNull(person, ERROR_CHECK_NOT_NULL, PERSON);
        checkNotNull(groupCollectionId, ERROR_CHECK_NOT_NULL, GROUP_COLLECTION_ID);

        GroupPostCollection c = getGroupCollection(groupCollectionId);
        checkManageGroupCollectionsPermissions(person, c.getGroupId());
        repository.delete(c);
    }

    @Nonnull
    @Override
    public Page<GroupPostCollection> findGroupCollections(@Nonnull GroupPostCollectionSearchCriteria criteria, @Nonnull Pageable page) {
        checkNotNull(criteria, ERROR_CHECK_NOT_NULL, PERSON);
        checkNotNull(page, ERROR_CHECK_NOT_NULL, "page");

        return repository.findGroupCollections(criteria, page);
    }

    @Nonnull
    @Override
    public GroupPostCollection addPost(@Nonnull PersonDTO person, @Nonnull Post post,
                                       @Nonnull Long groupCollectionId) {
        checkNotNull(person, ERROR_CHECK_NOT_NULL, PERSON);
        checkNotNull(post, ERROR_CHECK_NOT_NULL, "post");
        checkNotNull(groupCollectionId, ERROR_CHECK_NOT_NULL, GROUP_COLLECTION_ID);

        GroupPostCollection c = getGroupCollection(groupCollectionId);
        checkManageGroupCollectionsPermissions(person, c.getGroupId());

        c.getPosts().add(post);
        return repository.save(c);
    }

    @Nonnull
    @Override
    public GroupPostCollection removePost(@Nonnull PersonDTO person, Post post, Long groupCollectionId) {
        checkNotNull(person, ERROR_CHECK_NOT_NULL, PERSON);
        checkNotNull(post, ERROR_CHECK_NOT_NULL, "post");
        checkNotNull(groupCollectionId, ERROR_CHECK_NOT_NULL, GROUP_COLLECTION_ID);

        GroupPostCollection c = getGroupCollection(groupCollectionId);
        checkManageGroupCollectionsPermissions(person, c.getGroupId());
        c.getPosts().remove(post);

        return repository.save(c);
    }

    @Nonnull
    @Override
    public Page<Post> getGroupCollectionPosts(Long groupCollectionId,
                                              @Nonnull CollectionPostsSearchCriteria criteria,
                                              Pageable page) {

        checkNotNull(groupCollectionId, ERROR_CHECK_NOT_NULL, GROUP_COLLECTION_ID);
        checkNotNull(page, ERROR_CHECK_NOT_NULL, "page");
        checkNotNull(criteria, ERROR_CHECK_NOT_NULL, "criteria");

        return repository.findPosts(groupCollectionId, criteria, page);
    }

    private void checkManageGroupCollectionsPermissions(PersonDTO person, Long groupId) {
        if (!permissionUtil.hasGroupPermissions(person, groupId)) {
            throw new ForbiddenException("error.person.can.not.modify.group.collection");
        }
    }
}
