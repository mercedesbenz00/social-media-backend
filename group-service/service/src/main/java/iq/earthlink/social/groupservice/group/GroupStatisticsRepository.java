package iq.earthlink.social.groupservice.group;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GroupStatisticsRepository extends JpaRepository<UserGroupStats, Long> {

  @Modifying
  @Query("UPDATE UserGroupStats set publishedPostsCount = publishedPostsCount + :#{#groupStats.publishedPostsDelta}, " +
          "membersCount = membersCount + :#{#groupStats.membersDelta}, " +
          "score = publishedPostsCount + :#{#groupStats.publishedPostsDelta} + membersCount + :#{#groupStats.membersDelta} " +
          "where group.id = :#{#groupStats.userGroupId}")
  void updateGroupStatistics(@Param("groupStats") GroupStatistics groupStats);

  @Modifying
  @Query("UPDATE UserGroupStats s set s.publishedPostsCount = CASE WHEN (s.publishedPostsCount + :delta) < 0 THEN 0 " +
          "ELSE (s.publishedPostsCount + :delta) end where s.group.id = :groupId")
  void updatePostCount(@Param("groupId") Long groupId, @Param("delta") long delta);

}
