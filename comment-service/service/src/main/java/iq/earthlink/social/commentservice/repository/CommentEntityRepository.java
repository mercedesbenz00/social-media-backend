package iq.earthlink.social.commentservice.repository;

import iq.earthlink.social.common.data.model.CommentEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface CommentEntityRepository extends JpaRepository<CommentEntity, Long> {


    @Query("SELECT c FROM CommentEntity c WHERE c.objectId = :objectId " +
            "AND (((:showAll = FALSE OR :showAll IS NULL) AND c.isDeleted = FALSE) OR :showAll = TRUE) " +
            "AND c.replyTo IS NULL")
    Page<CommentEntity> findComments(@Param("objectId") UUID objectId, @Param("showAll") Boolean showAll, Pageable page);

    Optional<CommentEntity> findByIdAndObjectId(Long commentId, UUID objectId);

    @Query("SELECT c FROM CommentEntity c WHERE c.replyTo.id = :commentId AND c.objectId = :objectId " +
            "AND (((:showAll = FALSE OR :showAll IS NULL) AND c.isDeleted=FALSE) OR :showAll = TRUE) " +
            "ORDER BY c.createdAt DESC")
    Page<CommentEntity> findReplies(@Param("commentId") Long commentId, @Param("objectId") UUID objectId, @Param("showAll") Boolean showAll, Pageable page);

    Optional<CommentEntity> findByIdAndObjectIdAndAuthorId(Long commentId, UUID objectId, Long authorId);
}
