package iq.earthlink.social.postservice.post.complaint.repository;

import iq.earthlink.social.classes.enumeration.PostState;
import iq.earthlink.social.postservice.post.PostComplaintState;
import iq.earthlink.social.postservice.post.complaint.model.PostComplaint;
import iq.earthlink.social.postservice.post.complaint.model.Reason;
import iq.earthlink.social.postservice.post.model.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PostComplaintRepository extends JpaRepository<PostComplaint, Long> {

    @Query("SELECT c FROM PostComplaint c WHERE c.post.id = ?1")
    Page<PostComplaint> findComplaints(Long postId, Pageable page);

    void deleteByPostId(Long postId);

    Page<PostComplaint> findByPostIdAndState(Long postId, PostComplaintState state, Pageable page);

    boolean existsByReason(Reason reason);

    @Query("SELECT p.post FROM PostComplaint p WHERE p.post.userGroupId IN (:groupIds) " +
            "AND p.state = :complaintState " +
            "AND p.post.state = :postState " +
            "AND p.createdAt = (SELECT max(pp.createdAt) " +
            "FROM PostComplaint pp " +
            "WHERE pp.post.id=p.post.id " +
            "AND pp.state = :complaintState " +
            "AND pp.post.state = :postState)")
    Page<Post> findPostsWithComplaints(@Param("groupIds") List<Long> groupIds,
                                       @Param("complaintState") PostComplaintState complaintState,
                                       @Param("postState") PostState postState, Pageable page);

    Optional<PostComplaint> findByAuthorIdAndPostId(Long authorId, Long postId);

    @Modifying
    @Query("UPDATE PostComplaint pc " +
            "SET pc.state='POST_REJECTED', " +
            "pc.resolvingText = :resolvingText, " +
            "pc.resolverId = :resolverPersonId, " +
            "pc.resolvingDate= CURRENT_DATE " +
            "WHERE pc.post.id = :postId " +
            "AND pc.state = 'PENDING'")
    void resolvePendingPostComplaints(@Param("postId") Long postId,
                                      @Param("resolverPersonId") Long resolverPersonId,
                                      @Param("resolvingText") String resolvingText);

    @Query("SELECT count(c.id) from PostComplaint c where c.post.authorId = :personId")
    Long getAllPostComplaintsCountByPerson(@Param("personId") Long personId);
}
