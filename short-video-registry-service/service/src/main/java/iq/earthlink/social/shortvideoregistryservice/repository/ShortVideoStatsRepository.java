package iq.earthlink.social.shortvideoregistryservice.repository;

import iq.earthlink.social.shortvideoregistryservice.model.ShortVideoStats;
import org.springframework.data.cassandra.repository.CassandraRepository;
import org.springframework.data.cassandra.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface ShortVideoStatsRepository extends CassandraRepository<ShortVideoStats, UUID> {

    @Query("UPDATE short_video_stats set dislikes = dislikes + :dislikesOffset," +
            " likes = likes + :likesOffset" +
            " WHERE id = :videoId")
    void updateVotes(@Param("videoId") UUID videoId, @Param("likesOffset") long likesOffset, @Param("dislikesOffset") long dislikesOffset);

    @Query("UPDATE short_video_stats set comments = comments + :commentsOffset" +
            " WHERE id = :videoId")
    void updateComments(@Param("videoId") UUID videoId, @Param("commentsOffset") long commentsOffset);
}