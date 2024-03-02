package iq.earthlink.social.postservice.post.vote.repository;

import iq.earthlink.social.postservice.post.vote.CommentVote;
import iq.earthlink.social.postservice.post.vote.CommentVotePK;
import iq.earthlink.social.postservice.post.vote.VoteCount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface CommentVoteRepository extends JpaRepository<CommentVote, Long> {

    @Query("DELETE FROM CommentVote cv WHERE cv.id.personId = :personId AND cv.id.comment.id = :commentId")
    @Modifying
    void removeCommentVote(@Param("personId") Long personId, @Param("commentId") Long commentId);

    @Query("SELECT new iq.earthlink.social.postservice.post.vote.VoteCount(cv.id.comment.id, sum(case when cv.voteType = 1 then 1 else 0 end), sum(case when cv.voteType = 2 then 1 else 0 end)) "
            + "FROM CommentVote cv "
            + "WHERE cv.id.comment.id IN (:ids) "
            + "GROUP BY cv.id.comment.id")
    List<VoteCount> getCommentVoteCounts(@Param("ids") Collection<Long> ids);

    @Query("SELECT cv FROM CommentVote cv WHERE cv.id.personId = :#{#id.personId} and cv.id.comment.id = :#{#id.comment.id}")
    CommentVote findByPrimaryKey(@Param("id") CommentVotePK id);

    @Query("SELECT cv FROM CommentVote cv WHERE cv.id.personId = :personId and cv.id.comment.id IN :commentIds")
    List<CommentVote> getPersonVotesForComments(@Param("personId") Long personId, @Param("commentIds") Collection<Long> commentIds);

    @Query("SELECT cv FROM CommentVote cv WHERE cv.id.personId = :personId ")
    List<CommentVote> getAllPersonCommentVotes(@Param("personId") Long personId);

    @Query("SELECT cv FROM CommentVote cv WHERE cv.id.comment.id IN :commentIds")
    List<CommentVote> getCommentVotes(@Param("commentIds") List<Long> commentIds);
}
