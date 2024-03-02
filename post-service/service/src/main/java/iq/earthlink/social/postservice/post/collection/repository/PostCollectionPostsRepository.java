package iq.earthlink.social.postservice.post.collection.repository;

import iq.earthlink.social.postservice.post.CollectionPostsSearchCriteria;
import iq.earthlink.social.postservice.post.collection.PostCollection;
import iq.earthlink.social.postservice.post.collection.PostCollectionPost;
import iq.earthlink.social.postservice.post.model.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PostCollectionPostsRepository extends JpaRepository<PostCollectionPost, Long> {
    //
    @Query("SELECT p FROM PostCollectionPost c JOIN c.post p "
            +  "WHERE c.postCollection.id = :#{#collectionId} "
            +  "AND p.state = 'PUBLISHED' "
            +  "AND (:#{#criteria.postId} IS NULL OR :#{#criteria.postId} = p.id) "
            +  "AND (:#{#criteria.query} IS NULL OR LOWER(p.content) "
            +  "LIKE CONCAT('%', LOWER(?#{#criteria.query ?:''}), '%')) "
            +  "ORDER BY p.publishedAt DESC")
    Page<Post> findPosts(Long collectionId, CollectionPostsSearchCriteria criteria, Pageable page);

    @Query("SELECT pcp FROM PostCollectionPost pcp " +
            "WHERE pcp.postCollection = :collection " +
            "ORDER BY pcp.createdAt ASC")
    List<PostCollectionPost> findLimitedPostsForCollection(@Param("collection") PostCollection collection, Pageable pageable);

    void deleteByPostCollectionIdAndPostId(Long collectionId, Long postId);

    void deleteByPostCollectionId(Long collectionId);

    void deleteByPostId(Long postId);

    Optional<PostCollectionPost> findByPostCollectionIdAndPostId(Long collectionId, Long postId);

    @Query("SELECT pcp.postCollection.id FROM PostCollectionPost pcp " +
            "WHERE pcp.post.id = :postId")
    List<Long> findCollectionIdByPostId(Long postId);

    List<PostCollectionPost> findByPostId(Long postId);
}
