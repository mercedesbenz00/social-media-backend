package iq.earthlink.social.postservice.post.vote.repository;

import iq.earthlink.social.postservice.post.vote.PostVote;
import iq.earthlink.social.postservice.post.vote.PostVotePK;
import iq.earthlink.social.postservice.post.vote.VoteCount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;

import javax.persistence.QueryHint;
import java.util.Collection;
import java.util.List;

public interface PostVoteRepository extends JpaRepository<PostVote, Long> {

    @Query("DELETE FROM PostVote pv WHERE pv.id.post.id IN :postIds")
    @Modifying
    void removePostVotes(@Param("postIds") List<Long> postIds);

    @Query("DELETE FROM PostVote pv WHERE pv.id.personId = :personId AND pv.id.post.id = :postId")
    @Modifying
    void removePostVote(@Param("personId") Long personId, @Param("postId") Long postId);

    @Query("SELECT new iq.earthlink.social.postservice.post.vote.VoteCount(pv.id.post.id, sum(case when pv.voteType = 1 then 1 else 0 end), sum(case when pv.voteType = 2 then 1 else 0 end)) "
            + "FROM PostVote pv "
            + "WHERE pv.id.post.id IN :postIds "
            + "GROUP BY pv.id.post.id")
    List<VoteCount> getPostVoteCounts(@Param("postIds") Collection<Long> postIds);

    @Query("SELECT pv FROM PostVote pv WHERE pv.id.personId = :#{#id.personId} and pv.id.post.id = :#{#id.post.id}")
    PostVote findByPrimaryKey(@Param("id") PostVotePK id);

    @Query("SELECT pv FROM PostVote pv WHERE pv.id.personId = :personId and pv.id.post.id IN :postIds")
    List<PostVote> getPersonPostVotesForPosts(@Param("personId") Long personId, @Param("postIds") Collection<Long> postIds);

    @QueryHints(@QueryHint(name = org.hibernate.annotations.QueryHints.CACHEABLE, value = "true"))
    @Query("SELECT pv FROM PostVote pv WHERE pv.id.personId = :personId")
    List<PostVote> getAllPersonPostVotes(@Param("personId") Long personId);

    @Query("SELECT pv FROM PostVote pv WHERE pv.id.post.id IN :postIds")
    List<PostVote> getPostVotes(@Param("postIds") List<Long> postIds);
}
