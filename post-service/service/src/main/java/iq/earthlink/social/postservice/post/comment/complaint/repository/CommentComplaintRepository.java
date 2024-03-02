package iq.earthlink.social.postservice.post.comment.complaint.repository;

import iq.earthlink.social.classes.enumeration.CommentComplaintState;
import iq.earthlink.social.postservice.post.comment.Comment;
import iq.earthlink.social.postservice.post.comment.complaint.CommentComplaint;
import iq.earthlink.social.postservice.post.complaint.model.Reason;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CommentComplaintRepository extends JpaRepository<CommentComplaint, Long> {

    @Query("SELECT c FROM CommentComplaint c WHERE c.comment.id = ?1")
    Page<CommentComplaint> findComplaints(Long commentId, Pageable page);

    @Query("SELECT c.comment FROM CommentComplaint c " +
            "WHERE c.comment.post.userGroupId IN (:groupIds) " +
            "AND c.state=:complaintState " +
            "AND c.comment.isDeleted = :deleted " +
            "AND c.createdAt = (SELECT max(cp.createdAt) " +
                "FROM CommentComplaint cp " +
                "WHERE cp.comment.id=c.comment.id " +
                "AND cp.state = :complaintState " +
                "AND cp.comment.isDeleted = :deleted)")
    Page<Comment> findCommentsWithComplaints(@Param("groupIds") List<Long> groupIds,
                                             @Param("complaintState") CommentComplaintState complaintState,
                                             @Param("deleted") boolean deleted, Pageable page);

    void deleteByCommentIdIn(List<Long> commentIds);

    @Modifying
    @Query("UPDATE CommentComplaint cc " +
            "SET cc.state='COMMENT_REJECTED', " +
            "cc.resolvingText = :resolvingText, " +
            "cc.resolverId = :resolverPersonId, " +
            "cc.resolvingDate= CURRENT_DATE " +
            "WHERE cc.comment.id = :commentId " +
            "AND cc.state = 'PENDING'")
    void resolvePendingCommentComplaints(@Param("commentId") Long commentId,
                                         @Param("resolverPersonId") Long resolverPersonId,
                                         @Param("resolvingText") String resolvingText);


    boolean existsByReason(Reason reason);

    List<CommentComplaint> findByCommentIdAndState(Long commentId, CommentComplaintState state);

    Optional<CommentComplaint> findByAuthorIdAndCommentId(Long authorId, Long commentId);

    Optional<CommentComplaint> findByComplaintUuid(UUID complaintUuid);
    @Query("SELECT count(c.id) from CommentComplaint  c where c.comment.authorId = :personId")
    Long getAllCommentComplaintsCountByPerson(@Param("personId") Long personId);
}
