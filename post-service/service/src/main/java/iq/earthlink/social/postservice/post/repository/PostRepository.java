package iq.earthlink.social.postservice.post.repository;

import iq.earthlink.social.classes.data.dto.GroupMemberPosts;
import iq.earthlink.social.classes.enumeration.PostState;
import iq.earthlink.social.postservice.group.AccessType;
import iq.earthlink.social.postservice.post.PostSearchCriteria;
import iq.earthlink.social.postservice.post.model.Post;
import iq.earthlink.social.postservice.post.rest.CreatedPosts;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;

import javax.persistence.QueryHint;
import java.util.*;

public interface PostRepository extends JpaRepository<Post, Long> {

    @QueryHints(@QueryHint(name = org.hibernate.annotations.QueryHints.CACHEABLE, value = "true"))
    @Query("SELECT p FROM Post p "
            + "WHERE (:#{#criteria.query} = '%' OR LOWER(p.content) LIKE :#{#criteria.query} "
            + "OR SIMILARITY(p.content, :#{#criteria.query}) > :#{#criteria.similarityThreshold}) "
            + "AND p.userGroupId IN (SELECT ug.groupId "
            +                         "FROM UserGroup ug "
            +                         "LEFT JOIN GroupMember gm "
            +                         "ON ug.id = gm.userGroup.id "
            +                         "WHERE gm.person.personId = :#{#criteria.userId} OR ug.accessType='PUBLIC') "
            + "AND (coalesce(:#{#criteria.groupIds}, null) IS NULL OR p.userGroupId IN :#{#criteria.groupIds}) "
            + "AND (coalesce(:#{#criteria.states}, null) IS NULL OR p.state IN (:#{#criteria.states})) "
            + "AND (coalesce(:#{#criteria.authorIds},null) IS NULL OR p.authorId IN (:#{#criteria.authorIds})) "
            + "AND (:#{#criteria.pinned} IS NULL OR p.pinned = :#{#criteria.pinned}) ")
    Page<Post> findPosts(@Param("criteria") PostSearchCriteria criteria, Pageable page);

    @QueryHints(@QueryHint(name = org.hibernate.annotations.QueryHints.CACHEABLE, value = "true"))
    @Query("SELECT p FROM Post p "
            + "LEFT JOIN PostStatistics ps on ps.post.id = p.id "
            + "WHERE (:#{#criteria.query} = '%' OR LOWER(p.content) LIKE :#{#criteria.query} "
            + "OR SIMILARITY(p.content, :#{#criteria.query}) > :#{#criteria.similarityThreshold}) "
            + "AND p.userGroupId IN (SELECT ug.groupId "
            +                         "FROM UserGroup ug "
            +                         "LEFT JOIN GroupMember gm "
            +                         "ON ug.id = gm.userGroup.id "
            +                         "WHERE gm.person.personId = :#{#criteria.userId} OR ug.accessType='PUBLIC') "
            + "AND (coalesce(:#{#criteria.groupIds}, null) IS NULL OR p.userGroupId IN :#{#criteria.groupIds}) "
            + "AND p.state = 'PUBLISHED' "
            + "AND (coalesce(:#{#criteria.authorIds},null) IS NULL OR p.authorId IN (:#{#criteria.authorIds})) "
            + "ORDER BY ps.score DESC")
    Page<Post> findTopPosts(@Param("criteria") PostSearchCriteria criteria, Pageable page);

    @QueryHints(@QueryHint(name = org.hibernate.annotations.QueryHints.CACHEABLE, value = "true"))
    @Query("SELECT p FROM Post p "
            + "LEFT JOIN PostStatistics ps on ps.post.id = p.id "
            + "WHERE p.publishedAt >= :date "
            + "AND (:#{#criteria.query} = '%' OR LOWER(p.content) LIKE :#{#criteria.query} "
            + "OR SIMILARITY(p.content, :#{#criteria.query}) > :#{#criteria.similarityThreshold}) "
            + "AND p.userGroupId IN (SELECT ug.groupId "
            +                         "FROM UserGroup ug "
            +                         "LEFT JOIN GroupMember gm "
            +                         "ON ug.id = gm.userGroup.id "
            +                         "WHERE gm.person.personId = :#{#criteria.userId} OR ug.accessType='PUBLIC') "
            + "AND (coalesce(:#{#criteria.groupIds}, null) IS NULL OR p.userGroupId IN :#{#criteria.groupIds}) "
            + "AND (coalesce(:#{#criteria.authorIds},null) IS NULL OR p.authorId IN (:#{#criteria.authorIds})) "
            + "AND p.state = 'PUBLISHED' "
            + "ORDER BY ps.score DESC")
    Page<Post> findTopPostsInTimeInterval(@Param("criteria") PostSearchCriteria criteria, @Param("date") Date date, Pageable page);

    @Modifying
    @Query(value = "UPDATE Post p set p.authorDisplayName = :displayName " +
            "WHERE p.authorId = :authorId and p.state not in ('DELETED', 'REJECTED')")
    void updatePostAuthorDisplayName(@Param("displayName") String displayName, @Param("authorId") Long authorId);

    @Modifying
    @Query(value = "UPDATE Post p set p.authorUuid = :authorUuid WHERE p.authorId = :authorId")
    void updatePostAuthorUuidById(@Param("authorId") Long authorId, @Param("authorUuid") UUID authorUuid);

    @Modifying
    @Query(value = "UPDATE Post p set p.userGroupType = :userGroupType WHERE p.userGroupId = :userGroupId")
    void updatePostUserGroupType(@Param("userGroupId") Long userGroupId, @Param("userGroupType") AccessType userGroupType);

    @Modifying
    @Query(value = "UPDATE Post p set p.userGroupType = :userGroupType " +
            "WHERE p.userGroupId = :userGroupId and p.state not in ('DELETED', 'REJECTED')")
    void updatePostGroupTypeByGroupId(@Param("userGroupType") AccessType userGroupType, @Param("userGroupId") Long userGroupId);

    @Query("SELECT new map(count(p) as cnt, p.authorId as authorId) from Post p group by p.authorId")
    List<Map<String, Long>> findPostCountsForAuthors();

    @Query("SELECT new iq.earthlink.social.classes.data.dto.GroupMemberPosts(p.userGroupId, p.authorId, count(p) as postsCount) " +
            "FROM Post p " +
            "WHERE p.state = 'PUBLISHED' " +
            "GROUP BY p.userGroupId, p.authorId")
    List<GroupMemberPosts> findPublishedPostsForUserGroupAndAuthor();

    @Query("SELECT new iq.earthlink.social.postservice.post.rest.CreatedPosts(to_char(p.publishedAt, 'YYYY-MM-dd') AS date, count(p.id)) " +
            "FROM Post p " +
            "WHERE (coalesce(:fromDate, NULL) IS NULL OR p.publishedAt >= :fromDate) and p.state = 'PUBLISHED' " +
            "GROUP BY date")
    List<CreatedPosts> getCreatedPostsPerDay(@Param("fromDate") Date fromDate);

    @Query("SELECT new iq.earthlink.social.postservice.post.rest.CreatedPosts(to_char(p.publishedAt, 'YYYY-MM') AS date, count(p.id)) " +
            "FROM Post p " +
            "WHERE (coalesce(:fromDate, NULL) IS NULL OR p.publishedAt >= :fromDate) and p.state = 'PUBLISHED' " +
            "GROUP BY date")
    List<CreatedPosts> getCreatedPostsPerMonth(@Param("fromDate") Date fromDate);

    @Query("SELECT new iq.earthlink.social.postservice.post.rest.CreatedPosts(to_char(p.publishedAt, 'YYYY') AS date, count(p.id)) " +
            "FROM Post p " +
            "WHERE (coalesce(:fromDate, NULL) IS NULL OR p.publishedAt >= :fromDate) and p.state = 'PUBLISHED' " +
            "GROUP BY date")
    List<CreatedPosts> getCreatedPostsPerYear(@Param("fromDate") Date fromDate);

    @Query("SELECT count(p.id) from Post p " +
            "WHERE p.state = 'PUBLISHED' ")
    long getAllPostsCount();

    @Query("SELECT count(p.id) from Post p " +
            "WHERE(coalesce(:fromDate, NULL) IS NULL OR p.publishedAt >= :fromDate) and p.state = 'PUBLISHED' ")
    long getNewPostsCount(@Param("fromDate") Date fromDate);

    @QueryHints(@QueryHint(name = org.hibernate.annotations.QueryHints.CACHEABLE, value = "true"))
    Optional<Post> findByPostUuid(UUID postUuid);

    List<Post> findByPostUuidIn(List<UUID> postUuids);

    @NotNull
    Optional<Post> findById(@NotNull Long id);

    List<Post> findByRepostedFromId(Long repostedFromId);

    @Query(value = "SELECT p.user_group_id, COUNT(*) as postCount " +
            "FROM post p " +
            "WHERE p.created_at >= CURRENT_DATE - INTERVAL '90' DAY " +
            "AND p.user_group_id IS NOT NULL " +
            "AND p.state = 'PUBLISHED' " +
            "AND p.author_id = :personId " +
            "GROUP BY p.user_group_id " +
            "ORDER BY postCount DESC " +
            "LIMIT 10", nativeQuery = true)
    List<Long> findTopGroupIdsByPostFrequency(Long personId);

    @Query("SELECT count(p.id) from Post p " +
            "WHERE p.state = :state and p.userGroupId = :groupId")
    Long getPostsCountByStateAndGroup(@Param("state") PostState state, @Param("groupId") Long groupId);

    List<Post> findByState(PostState state);
}
