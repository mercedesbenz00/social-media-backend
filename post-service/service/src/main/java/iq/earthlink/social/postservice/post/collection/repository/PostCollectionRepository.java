package iq.earthlink.social.postservice.post.collection.repository;

import iq.earthlink.social.postservice.post.collection.PostCollection;
import iq.earthlink.social.postservice.post.collection.PostCollectionSearchCriteria;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PostCollectionRepository extends JpaRepository<PostCollection, Long> {
    @Query("SELECT c FROM PostCollection c "
            + "WHERE (:#{#criteria.personId} IS NULL OR c.ownerId = :#{#criteria.personId}) "
            + "AND (:#{#criteria.query} IS NULL OR LOWER(c.name) LIKE CONCAT('%', LOWER(?#{#criteria.query ?:''}), '%'))")
    Page<PostCollection> findCollections(@Param("criteria") PostCollectionSearchCriteria criteria, Pageable page);

    Optional<PostCollection> findByIdInAndOwnerId(List<Long> collectionIdByPostId, Long personId);
}
