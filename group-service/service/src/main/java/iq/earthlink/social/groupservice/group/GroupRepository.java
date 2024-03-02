package iq.earthlink.social.groupservice.group;

import iq.earthlink.social.groupservice.group.rest.CreateGroupRequests;
import iq.earthlink.social.groupservice.group.rest.CreatedGroups;
import iq.earthlink.social.groupservice.group.rest.enumeration.AccessType;
import iq.earthlink.social.groupservice.group.rest.enumeration.ApprovalState;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Date;
import java.util.List;

public interface GroupRepository extends JpaRepository<UserGroup, Long> {

    @Query("SELECT g as group, max(s.membersCount) as membersCount, max(s.publishedPostsCount) as publishedPostsCount, max(s.score) as score "
            + "FROM UserGroup g "
            + "LEFT JOIN GroupMember m ON m.group.id = g.id "
            + "LEFT JOIN g.stats s "
            + "LEFT JOIN g.categories gc "
            + "WHERE ((:#{#criteria.memberId} IS NULL AND g.visibility = 'EVERYONE') "
            + "OR (:#{#criteria.memberId} IS NOT NULL "
            + "AND (:#{#criteria.isAdmin} = TRUE OR g.visibility = 'EVERYONE' "
            + "OR (m.state IN ('INVITED','APPROVED') AND :#{#criteria.memberId} = m.personId)))) "
            + "AND (:#{#criteria.query} = '%' OR LOWER(g.name) LIKE :#{#criteria.query} "
            + "OR SIMILARITY(g.name, :#{#criteria.query}) > :#{#criteria.similarityThreshold}) "
            + "AND (:#{#criteria.groupIds} IS NULL OR g.id IN :#{#criteria.groupIds}) "
            + "AND (:#{#criteria.categoryIds} IS NULL OR gc.id IN :#{#criteria.categoryIds}) "
            + "AND ((:#{#criteria.states}) IS NULL OR g.state IN (:#{#criteria.states})) "
            + "GROUP BY g.id")
    Page<UserGroupModel> findGroups(@Param("criteria") GroupSearchCriteria criteria, Pageable page);

    @Query("SELECT g as group, max(s.membersCount) as membersCount, max(s.publishedPostsCount) as publishedPostsCount, max(s.score) as score "
            + "FROM UserGroup g "
            + "LEFT JOIN GroupMember m ON m.group.id = g.id "
            + "LEFT JOIN g.stats s "
            + "LEFT JOIN g.categories gc "
            + "WHERE ((:#{#criteria.memberId} IS NULL AND g.visibility = 'EVERYONE') "
            + "OR (:#{#criteria.memberId} IS NOT NULL "
            + "AND (:#{#criteria.isAdmin} = TRUE OR g.visibility = 'EVERYONE' "
            + "OR (m.state IN ('INVITED','APPROVED') AND :#{#criteria.memberId} = m.personId)))) "
            + "AND (:#{#criteria.query} = '%' OR LOWER(g.name) LIKE :#{#criteria.query} "
            + "OR SIMILARITY(g.name, :#{#criteria.query}) > :#{#criteria.similarityThreshold}) "
            + "AND (:#{#criteria.groupIds} IS NULL OR g.id IN :#{#criteria.groupIds}) "
            + "AND (:#{#criteria.categoryIds} IS NULL OR gc.id IN :#{#criteria.categoryIds}) "
            + "AND ((:#{#criteria.states}) IS NULL OR g.state IN (:#{#criteria.states})) "
            + "GROUP BY g.id "
            + "ORDER BY SIMILARITY(g.name, :#{#criteria.query}) DESC")
    Page<UserGroupModel> findGroupsOrderedBySimilarity(@Param("criteria") GroupSearchCriteria criteria, Pageable page);

    @Query("SELECT DISTINCT g FROM UserGroup g " +
            "JOIN GroupMember m ON m.group.id = g.id " +
            "LEFT JOIN g.categories c " +
            "LEFT JOIN g.tags t " +
            "WHERE m.group.id NOT IN (SELECT m2.group.id FROM GroupMember m2 WHERE m2.personId = :personId) " +
            "AND g.state ='APPROVED' " +
            "AND g.visibility = 'EVERYONE' " +
            "AND (coalesce(:categoryIds, NULL) IS NULL OR c.id IN (:categoryIds)) ")
    Page<UserGroup> findSimilarGroups(@Param("personId") Long personId, @Param("categoryIds") List<Long> categoryIds, Pageable page);

    @Query("SELECT DISTINCT g FROM UserGroup g " +
            "JOIN GroupMember m ON m.group.id = g.id " +
            "WHERE m.group.id NOT IN (SELECT m2.group.id FROM GroupMember m2 WHERE m2.personId = :personId) " +
            "AND g.state ='APPROVED' " +
            "AND g.visibility = 'EVERYONE' " +
            "AND m.personId IN :subscribedToIds")
    Page<UserGroup> findSuggestedGroups(@Param("personId") Long personId, @Param("subscribedToIds") List<Long> subscribedToIds, Pageable page);

    @Query("SELECT g as group, s.membersCount as membersCount, s.publishedPostsCount as publishedPostsCount, s.score as score "
            + "FROM UserGroup g "
            + "JOIN g.stats s "
            + "WHERE g.state ='APPROVED' "
            + "AND g.visibility = 'EVERYONE' "
            + "ORDER BY :orderBy DESC")
    Page<UserGroupModel> findOrderedGroups(@Param("orderBy") String orderBy, Pageable page);

    Page<UserGroup> findByStateIn(List<ApprovalState> states, Pageable page);

    @Query("SELECT g as group, m.createdAt as memberSince, m.publishedPostsCount as publishedPostsCount, m.visitedAt as visitedAt, m.state as state "
            + "FROM GroupMember m "
            + "JOIN UserGroup g ON m.group.id = g.id "
            + "WHERE m.personId = :personId "
            + "AND (((:#{#criteria.states}) IS NULL AND (m.state='APPROVED' OR m.state = 'INVITED')) OR m.state in (:#{#criteria.states})) "
            + "AND (:#{#criteria.query} = '%' OR LOWER(g.name) LIKE :#{#criteria.query} "
            + "OR SIMILARITY(g.name, :#{#criteria.query}) > :#{#criteria.similarityThreshold}) "
            + "AND g.state ='APPROVED'")
    Page<MemberUserGroupModel> findMyGroups(@Param("personId") Long personId, @Param("criteria") GroupSearchCriteria criteria, Pageable page);

    @Query("SELECT g as group, m.createdAt as memberSince, m.publishedPostsCount as publishedPostsCount, m.visitedAt as visitedAt, m.state as state "
            + "FROM GroupMember m "
            + "JOIN UserGroup g ON m.group.id = g.id "
            + "WHERE m.personId = :personId "
            + "AND (((:#{#criteria.states}) IS NULL AND (m.state='APPROVED' OR m.state = 'INVITED')) OR m.state in (:#{#criteria.states})) "
            + "AND (:#{#criteria.query} = '%' OR LOWER(g.name) LIKE :#{#criteria.query} "
            + "OR SIMILARITY(g.name, :#{#criteria.query}) > :#{#criteria.similarityThreshold}) "
            + "AND g.state ='APPROVED' "
            + "ORDER BY SIMILARITY(g.name, :#{#criteria.query}) DESC")
    Page<MemberUserGroupModel> findMyGroupsOrderedBySimilarity(@Param("personId") Long personId, @Param("criteria") GroupSearchCriteria criteria, Pageable page);

    @Query("SELECT g as group, m.createdAt as memberSince, m.publishedPostsCount as publishedPostsCount, m.visitedAt as visitedAt, m.state as state "
            + "FROM GroupMember m "
            + "JOIN UserGroup g ON m.group.id = g.id "
            + "JOIN GroupPermission gp ON g.id = gp.userGroup.id "
            + "WHERE m.personId = :personId "
            + "AND gp.personId = :personId "
            + "AND (gp.permission = :#{#criteria.status}) "
            + "AND (((:#{#criteria.states}) IS NULL AND (m.state='APPROVED' OR m.state = 'INVITED')) OR m.state in (:#{#criteria.states})) "
            + "AND (:#{#criteria.query} = '%' OR LOWER(g.name) LIKE :#{#criteria.query} "
            + "OR SIMILARITY(g.name, :#{#criteria.query}) > :#{#criteria.similarityThreshold}) "
            + "AND g.state ='APPROVED'")
    Page<MemberUserGroupModel> findMyGroupsByPermission(@Param("personId") Long personId, @Param("criteria") GroupSearchCriteria criteria, Pageable page);

    @Query("SELECT g as group, m.createdAt as memberSince, m.publishedPostsCount as publishedPostsCount, m.visitedAt as visitedAt, m.state as state "
            + "FROM GroupMember m "
            + "JOIN UserGroup g ON m.group.id = g.id "
            + "JOIN GroupPermission gp ON g.id = gp.userGroup.id "
            + "WHERE m.personId = :personId "
            + "AND gp.personId = :personId "
            + "AND (gp.permission = :#{#criteria.status}) "
            + "AND (((:#{#criteria.states}) IS NULL AND (m.state='APPROVED' OR m.state = 'INVITED')) OR m.state in (:#{#criteria.states})) "
            + "AND (:#{#criteria.query} = '%' OR LOWER(g.name) LIKE :#{#criteria.query} "
            + "OR SIMILARITY(g.name, :#{#criteria.query}) > :#{#criteria.similarityThreshold}) "
            + "AND g.state ='APPROVED' "
            + "ORDER BY SIMILARITY(g.name, :#{#criteria.query}) DESC")
    Page<MemberUserGroupModel> findMyGroupsByPermissionOrderedBySimilarity(@Param("personId") Long personId, @Param("criteria") GroupSearchCriteria criteria, Pageable page);

    @Query("SELECT new iq.earthlink.social.groupservice.group.rest.CreatedGroups(to_char(g.createdAt, 'YYYY-MM') AS date, count(g.id)) " +
            "FROM UserGroup g " +
            "WHERE (coalesce(:fromDate, NULL) IS NULL OR g.createdAt >= :fromDate) and g.state = 'APPROVED' " +
            "GROUP BY date ")
    List<CreatedGroups> getCreatedGroupsPerMonth(@Param("fromDate") Date fromDate);

    @Query("SELECT new iq.earthlink.social.groupservice.group.rest.CreatedGroups(to_char(g.createdAt, 'YYYY-MM-dd') AS date, count(g.id)) " +
            "FROM UserGroup g " +
            "WHERE (coalesce(:fromDate, NULL) IS NULL OR g.createdAt >= :fromDate) and g.state = 'APPROVED' " +
            "GROUP BY date ")
    List<CreatedGroups> getCreatedGroupsPerDay(@Param("fromDate") Date fromDate);

    @Query("SELECT new iq.earthlink.social.groupservice.group.rest.CreatedGroups(to_char(g.createdAt, 'YYYY') AS date, count(g.id)) " +
            "FROM UserGroup g " +
            "WHERE (coalesce(:fromDate, NULL) IS NULL OR g.createdAt >= :fromDate) and g.state = 'APPROVED' " +
            "GROUP BY date ")
    List<CreatedGroups> getCreatedGroupsPerYear(@Param("fromDate") Date fromDate);

    @Query("SELECT new iq.earthlink.social.groupservice.group.rest.CreateGroupRequests(to_char(g.createdAt, 'YYYY-MM') AS date, count(g.id)) " +
            "FROM UserGroup g " +
            "WHERE (coalesce(:fromDate, NULL) IS NULL OR g.createdAt >= :fromDate) and g.state = 'PENDING' " +
            "GROUP BY date ")
    List<CreateGroupRequests> getCreateGroupRequestsPerMonth(@Param("fromDate") Date fromDate);

    @Query("SELECT new iq.earthlink.social.groupservice.group.rest.CreateGroupRequests(to_char(g.createdAt, 'YYYY-MM-dd') AS date, count(g.id)) " +
            "FROM UserGroup g " +
            "WHERE (coalesce(:fromDate, NULL) IS NULL OR g.createdAt >= :fromDate) and g.state = 'PENDING' " +
            "GROUP BY date ")
    List<CreateGroupRequests> getCreateGroupRequestsPerDay(@Param("fromDate") Date fromDate);

    @Query("SELECT new iq.earthlink.social.groupservice.group.rest.CreateGroupRequests(to_char(g.createdAt, 'YYYY') AS date, count(g.id)) " +
            "FROM UserGroup g " +
            "WHERE (coalesce(:fromDate, NULL) IS NULL OR g.createdAt >= :fromDate) and g.state = 'PENDING' " +
            "GROUP BY date ")
    List<CreateGroupRequests> getCreateGroupRequestsPerYear(@Param("fromDate") Date fromDate);

    @Query("SELECT count(g.id) from UserGroup g " +
            "WHERE g.state = 'APPROVED' ")
    Long getAllGroupsCount();

    @Query("SELECT count(g.id) from UserGroup g " +
            "WHERE (coalesce(:fromDate, NULL) IS NULL OR g.createdAt >= :fromDate) and g.state = 'APPROVED'")
    Long getNewGroupsCount(@Param("fromDate") Date fromDate);

    List<UserGroup> findByAccessType(AccessType accessType);

    List<UserGroup> findByAccessTypeAndIdIn(AccessType accessType, List<Long> groupIds);

    List<UserGroup> findByIdIn(List<Long> groupIds);
}
