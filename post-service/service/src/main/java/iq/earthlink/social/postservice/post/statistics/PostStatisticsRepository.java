package iq.earthlink.social.postservice.post.statistics;

import iq.earthlink.social.postservice.post.PostStatisticsDTO;
import iq.earthlink.social.postservice.post.rest.PostStatisticsGap;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;

import javax.persistence.QueryHint;
import java.util.List;
import java.util.Optional;

public interface PostStatisticsRepository extends JpaRepository<PostStatistics, Long> {
    @QueryHints(@QueryHint(name = org.hibernate.annotations.QueryHints.CACHEABLE, value = "true"))
    Optional<PostStatistics> findByPostId(Long postId);

    List<PostStatistics> findByPostIdIn(List<Long> postIds);

    @Modifying
    void deletePostStatisticsByPostId(Long postId);


    @Modifying
    @Query(value = "UPDATE post_statistics set last_activity_at = :#{#ps.lastActivityAt}, " +
            "comments_count = comments_count + :#{#ps.commentsDelta}, " +
            "upvotes_count = upvotes_count + :#{#ps.upvotesDelta}, " +
            "downvotes_count = downvotes_count + :#{#ps.downvotesDelta}, " +
            "comments_upvotes_count = comments_upvotes_count + :#{#ps.commentsUpvotesDelta}, " +
            "comments_downvotes_count = comments_downvotes_count + :#{#ps.commentsDownvotesDelta}, " +
            "score = round((comments_count + :#{#ps.commentsDelta} + upvotes_count + :#{#ps.upvotesDelta} + comments_upvotes_count + :#{#ps.commentsUpvotesDelta} + " +
            "(case when (downvotes_count + :#{#ps.downvotesDelta}) = 0 then 0 else ((upvotes_count + :#{#ps.upvotesDelta}) * 1.0 / (downvotes_count + :#{#ps.downvotesDelta})) end) + " +
            "(case when (comments_downvotes_count + :#{#ps.commentsDownvotesDelta}) = 0 then 0 else ((comments_upvotes_count + :#{#ps.commentsUpvotesDelta}) * 1.0 / (comments_downvotes_count + :#{#ps.commentsDownvotesDelta})) end)), 2) " +
            "where post_id = :#{#ps.postId}", nativeQuery = true)
    void updatePostStatistics(@Param("ps") PostStatisticsDTO ps);


    @Modifying
    @Query(value = "UPDATE post_statistics p SET " +
            "comments_count =  coalesce((SELECT count(c) from post p1 JOIN comment c on p1.id = c.post_id WHERE c.is_deleted=false and p1.id = p.post_id GROUP BY p1.id), 0), " +
            "upvotes_count = coalesce((SELECT count(pv) from post p2 JOIN post_vote pv on p2.id = pv.post_id WHERE pv.vote_type = 1 AND p2.id = p.post_id GROUP BY p2.id), 0), " +
            "downvotes_count = coalesce((SELECT count(pv1) from post p3 JOIN post_vote pv1 on p3.id = pv1.post_id WHERE pv1.vote_type = 2 and p3.id = p.post_id GROUP BY p3.id), 0), " +
            "comments_upvotes_count = coalesce((SELECT count(cv) from post p4 JOIN comment c1 on c1.post_id = p4.id JOIN comment_vote cv on cv.comment_id = c1.id WHERE c1.is_deleted=false and cv.vote_type = 1 and p4.id = p.post_id GROUP BY p4.id), 0), " +
            "comments_downvotes_count = coalesce((SELECT count(cv1) from post p5 JOIN comment c2 on c2.post_id = p5.id JOIN comment_vote cv1 on cv1.comment_id = c2.id WHERE c2.is_deleted=false and cv1.vote_type = 2 and p5.id = p.post_id GROUP BY p5.id), 0) " +
            "WHERE p.post_id > 0", nativeQuery = true)
    void synchronizePostStatistics();

    @Modifying
    @Query(value = "UPDATE post_statistics p SET " +
            "score = round(comments_count + upvotes_count + comments_upvotes_count + " +
            "          (CASE WHEN downvotes_count = 0 THEN 0 ELSE (upvotes_count  * 1.0 / downvotes_count) END) + " +
            "          (CASE WHEN  comments_downvotes_count = 0 THEN 0 ELSE (comments_upvotes_count * 1.0 / comments_downvotes_count) END)) " +
            "WHERE p.post_id > 0", nativeQuery = true)
    void updatePostScore();

    @Query("SELECT new iq.earthlink.social.postservice.post.rest.PostStatisticsGap(p.id, count(c), ps.commentsCount) " +
            "from Post p " +
            "LEFT JOIN PostStatistics ps on ps.post.id = p.id " +
            "left join Comment c on p.id = c.post.id " +
            "group by p.id, ps.commentsCount " +
            "HAVING count(c) <> ps.commentsCount")
    List<PostStatisticsGap> findOutdatedPosts();
}
