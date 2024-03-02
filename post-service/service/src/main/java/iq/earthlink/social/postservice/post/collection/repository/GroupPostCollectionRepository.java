package iq.earthlink.social.postservice.post.collection.repository;

import iq.earthlink.social.postservice.post.CollectionPostsSearchCriteria;
import iq.earthlink.social.postservice.post.collection.GroupPostCollection;
import iq.earthlink.social.postservice.post.collection.GroupPostCollectionSearchCriteria;
import iq.earthlink.social.postservice.post.model.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface GroupPostCollectionRepository extends JpaRepository<GroupPostCollection, Long> {
    @Query("SELECT p FROM GroupPostCollection c JOIN c.posts p WHERE c.id = :#{#groupCollectionId} "
            +  "AND (:#{#criteria.query} IS NULL OR LOWER(p.content) "
            +  "LIKE CONCAT('%', LOWER(?#{#criteria.query ?:''}), '%')) ")
    Page<Post> findPosts(Long groupCollectionId, CollectionPostsSearchCriteria criteria, Pageable page);

    @Query("SELECT c FROM GroupPostCollection c "
            + "WHERE (:#{#criteria.groupId} IS NULL OR c.groupId = :#{#criteria.groupId}) "
            + "AND (:#{#criteria.personId} IS NULL OR c.authorId = :#{#criteria.personId}) "
            + "AND (:#{#criteria.query} IS NULL OR c.name LIKE %?#{#criteria.query}%)"
    )
    Page<GroupPostCollection> findGroupCollections(
            @Param("criteria") GroupPostCollectionSearchCriteria criteria, Pageable page);

    List<GroupPostCollection> findAllByPostsId(Long postId);
}
