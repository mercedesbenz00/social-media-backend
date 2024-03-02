package iq.earthlink.social.postservice.post.collection;

import iq.earthlink.social.postservice.person.dto.PersonDTO;
import iq.earthlink.social.postservice.post.CollectionPostsSearchCriteria;
import iq.earthlink.social.postservice.post.PostCollectionData;
import iq.earthlink.social.postservice.post.model.Post;
import iq.earthlink.social.postservice.post.rest.JsonPost;
import iq.earthlink.social.postservice.post.rest.JsonPostCollection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import javax.annotation.Nonnull;

public interface PostCollectionService {

  @Nonnull
  PostCollection createCollection(@Nonnull Long personId, @Nonnull PostCollectionData data);

  @Nonnull
  PostCollection updateCollection(@Nonnull Long personId, Long collectionId, @Nonnull PostCollectionData data);

  @Nonnull
  PostCollection getCollection(Long personId, @Nonnull Long collectionId);

  void addPost(@Nonnull Long personId, @Nonnull Post post, @Nonnull Long collectionId);

  void removePost(@Nonnull Long personId, Post post, Long collectionId);

  @Nonnull
  Page<JsonPost> getCollectionPosts(@Nonnull Long personId, Long collectionId, @Nonnull CollectionPostsSearchCriteria criteria, Pageable page);

  void removeCollection(@Nonnull PersonDTO person, Long collectionId);

  @Nonnull
  Page<JsonPostCollection> findCollections(@Nonnull PostCollectionSearchCriteria criteria, @Nonnull Pageable page);

  JsonPostCollection findMyCollectionByPostId(Long personId, Long postId);
}
