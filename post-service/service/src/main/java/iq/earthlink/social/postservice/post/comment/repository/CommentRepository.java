package iq.earthlink.social.postservice.post.comment.repository;

import iq.earthlink.social.postservice.post.comment.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;

import javax.persistence.QueryHint;
import java.util.Optional;
import java.util.UUID;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    @QueryHints(@QueryHint(name = org.hibernate.annotations.QueryHints.CACHEABLE, value = "true"))
    @Query("SELECT c FROM Comment c WHERE c.post.id = ?1 AND (((?2=FALSE OR ?2 IS NULL) AND c.isDeleted=FALSE) OR ?2=TRUE) AND c.replyTo IS NULL")
    Page<Comment> findComments(Long postId, Boolean showAll, Pageable page);

    @QueryHints(@QueryHint(name = org.hibernate.annotations.QueryHints.CACHEABLE, value = "true"))
    Page<Comment> findByPostId(Long postId, Pageable page);

    @QueryHints(@QueryHint(name = org.hibernate.annotations.QueryHints.CACHEABLE, value = "true"))
    @Query("SELECT c FROM Comment c WHERE c.replyTo.id = ?1 AND (((?2=FALSE OR ?2 IS NULL) AND c.isDeleted=FALSE) OR ?2=TRUE) ORDER BY c.createdAt DESC")
    Page<Comment> findReplies(Long commentId, Boolean showAll, Pageable page);

    @QueryHints(@QueryHint(name = org.hibernate.annotations.QueryHints.CACHEABLE, value = "true"))
    Optional<Comment> findByCommentUuid(UUID commentUuid);

    @Modifying
    @Query(value = "UPDATE Comment c set c.authorUuid = :authorUuid WHERE c.authorId = :authorId")
    void updateCommentAuthorUuidById(@Param("authorId") Long authorId, @Param("authorUuid") UUID authorUuid);
}
