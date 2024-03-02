package iq.earthlink.social.groupservice.group.permission.repository;

import iq.earthlink.social.groupservice.group.GroupMemberStatus;
import iq.earthlink.social.groupservice.group.permission.GroupPermission;
import iq.earthlink.social.groupservice.group.permission.PermissionSearchCriteria;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface GroupPermissionRepository extends JpaRepository<GroupPermission, Long> {

    @Query("SELECT CASE WHEN count(p) > 0 THEN true ELSE false END"
            + " FROM GroupPermission p WHERE p.userGroup.id = ?2 AND  p.personId = ?1 AND p.permission = ?3")
    boolean existsPermission(Long personId, Long groupId, GroupMemberStatus permission);

    @Query("SELECT p FROM GroupPermission p "
            + "JOIN UserGroup g ON p.userGroup.id = g.id "
            + "WHERE (coalesce(:#{#criteria.groupIds}, null) IS NULL OR p.userGroup.id IN (:#{#criteria.groupIds})) "
            + "AND (:#{#criteria.query} = '%' OR LOWER(g.name) LIKE :#{#criteria.query} "
            + "OR SIMILARITY(g.name, :#{#criteria.query}) > :#{#criteria.similarityThreshold}) "
            + "AND (:#{#criteria.personId} IS NULL OR p.personId = :#{#criteria.personId})"
            + "AND (coalesce(:#{#criteria.statuses}, null) IS NULL OR p.permission IN (:#{#criteria.statuses}))")
    Page<GroupPermission> findPermissions(@Param("criteria") PermissionSearchCriteria criteria, Pageable page);

    @Query("SELECT p FROM GroupPermission p "
            + "JOIN UserGroup g ON p.userGroup.id = g.id "
            + "WHERE (coalesce(:#{#criteria.groupIds}, null) IS NULL OR p.userGroup.id IN (:#{#criteria.groupIds})) "
            + "AND (:#{#criteria.personId} IS NULL OR p.personId = :#{#criteria.personId})"
            + "AND (coalesce(:#{#criteria.statuses}, null) IS NULL OR p.permission IN (:#{#criteria.statuses}))")
    List<GroupPermission> findPermissionsInternal(@Param("criteria") PermissionSearchCriteria criteria);

    @Query("SELECT p FROM GroupPermission p "
            + "JOIN UserGroup g ON p.userGroup.id = g.id "
            + "WHERE (coalesce(:#{#criteria.groupIds}, null) IS NULL OR p.userGroup.id IN (:#{#criteria.groupIds})) "
            + "AND (:#{#criteria.query} = '%' OR LOWER(g.name) LIKE :#{#criteria.query} "
            + "OR SIMILARITY(g.name, :#{#criteria.query}) > :#{#criteria.similarityThreshold}) "
            + "AND (:#{#criteria.personId} IS NULL OR p.personId = :#{#criteria.personId})"
            + "AND (coalesce(:#{#criteria.statuses}, null) IS NULL OR p.permission IN (:#{#criteria.statuses})) "
            + "ORDER BY SIMILARITY(g.name, :#{#criteria.query}) DESC")
    Page<GroupPermission> findPermissionsOrderedBySimilarity(@Param("criteria") PermissionSearchCriteria criteria, Pageable page);

    @Query("SELECT p FROM GroupPermission p "
            + "WHERE p.userGroup.id = :groupId "
            + "AND p.personId = :personId")
    List<GroupPermission> findByPersonIdAndGroupId(
            Long personId, Long groupId);

    @Query("SELECT p FROM GroupPermission p "
            + "WHERE (coalesce(:groupIds, NULL) IS NULL OR p.userGroup.id IN (:groupIds)) "
            + "AND p.personId = :personId")
    List<GroupPermission> findByPersonIdAndGroupIds(Long personId, List<Long> groupIds);

    List<GroupPermission> findByPersonIdAndUserGroupIdAndPermissionIn(Long memberId, Long groupId, List<GroupMemberStatus> permissions);

    void deleteByUserGroupId(Long groupId);

    void deleteByPersonIdAndUserGroupId(Long personId, Long groupId);
}
