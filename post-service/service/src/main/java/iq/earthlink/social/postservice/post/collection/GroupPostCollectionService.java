package iq.earthlink.social.postservice.post.collection;

import iq.earthlink.social.postservice.person.dto.PersonDTO;
import iq.earthlink.social.postservice.post.CollectionPostsSearchCriteria;
import iq.earthlink.social.postservice.post.GroupPostCollectionData;
import iq.earthlink.social.postservice.post.model.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import javax.annotation.Nonnull;

public interface GroupPostCollectionService {

  @Nonnull
  GroupPostCollection createGroupCollection(@Nonnull PersonDTO person, @Nonnull GroupPostCollectionData data);

  @Nonnull
  GroupPostCollection getGroupCollection(@Nonnull Long groupCollectionId);

  void removeGroupCollection(@Nonnull PersonDTO person, Long groupCollectionId);

  @Nonnull
  Page<GroupPostCollection> findGroupCollections(@Nonnull GroupPostCollectionSearchCriteria criteria, @Nonnull Pageable page);

  @Nonnull
  GroupPostCollection addPost(@Nonnull PersonDTO person, @Nonnull Post post, @Nonnull Long groupCollectionId);

  @Nonnull
  GroupPostCollection removePost(@Nonnull PersonDTO person, Post post, Long groupCollectionId);

  @Nonnull
  Page<Post> getGroupCollectionPosts(Long groupCollectionId, @Nonnull CollectionPostsSearchCriteria criteria, Pageable page);
}
