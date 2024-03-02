package iq.earthlink.social.groupservice.group.member;

import iq.earthlink.social.groupservice.group.GroupMemberSearchCriteria;
import iq.earthlink.social.groupservice.group.UserGroup;
import iq.earthlink.social.groupservice.group.rest.JoinGroupRequests;
import iq.earthlink.social.groupservice.group.rest.enumeration.ApprovalState;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface GroupMemberRepository extends JpaRepository<GroupMember, Long> {

  @Query("SELECT m FROM GroupMember m WHERE m.personId = :personId AND m.group.id = :groupId AND m.state='APPROVED'")
  GroupMember findActiveMember(@Param("groupId") Long groupId, @Param("personId") Long personId);

  @Query("SELECT m FROM GroupMember m WHERE m.state='APPROVED'")
  List<GroupMember> getAllActiveGroupMembers();

  Optional<GroupMember> findByPersonIdAndGroupId(Long personId, Long groupId);

  @Query("SELECT m FROM GroupMember m " +
          "LEFT JOIN Person p on p.personId = m.personId "
          + "WHERE m.group.id = :#{#criteria.groupId} "
          + "AND (coalesce(:#{#criteria.personIds}, null) IS NULL OR m.personId IN (:#{#criteria.personIds})) "
          + "AND (:#{#criteria.query} IS NULL OR LOWER(p.displayName) LIKE CONCAT('%', LOWER(:#{#criteria.query ?:''}), '%')) "
          + "AND (coalesce(:#{#criteria.states}, null) IS NULL OR m.state IN (:#{#criteria.states}))")
  Page<GroupMember> findMembers(@Param("criteria") GroupMemberSearchCriteria criteria, Pageable page);

  @Query("DELETE FROM GroupMember m WHERE m.group.id = ?1")
  @Modifying
  void deleteMembers(Long groupId);

  @Query("DELETE FROM GroupMember m WHERE m.personId = :personId")
  @Modifying
  void deleteMemberFromGroups(Long personId);

  @Query("SELECT count(m.personId) FROM GroupMember m WHERE m.personId = :personId AND m.state='APPROVED' GROUP BY m.personId")
  Long countJoinedGroupsByPerson(@Param("personId") Long personId);

  @Query("SELECT new map(count(m) as cnt, m.personId as personId) from GroupMember m group by m.personId")
  List<Map<String, Long>> findGroupCountsByPersons();

  List<GroupMember> findByPersonId(Long personId);

  @Query("UPDATE GroupMember m SET m.displayName=:displayName WHERE m.personId = :personId")
  @Modifying
  void updateDisplayNameByPersonId(@Param("personId") Long personId, @Param("displayName") String displayName);

  List<GroupMember> findByStateAndGroupIn(ApprovalState state, List<UserGroup> group);

  List<GroupMember> findByGroupIn(List<UserGroup> group);

  @Query("SELECT new iq.earthlink.social.groupservice.group.rest.JoinGroupRequests(to_char(m.createdAt, 'YYYY-MM') AS date, count(m.id)) " +
          "FROM GroupMember m " +
          "WHERE (coalesce(:fromDate, NULL) IS NULL OR m.createdAt >= :fromDate) and m.state = 'PENDING' " +
          "GROUP BY date ")
  List<JoinGroupRequests> getJoinGroupRequestsPerMonth(@Param("fromDate") Date fromDate);

  @Query("SELECT new iq.earthlink.social.groupservice.group.rest.JoinGroupRequests(to_char(m.createdAt, 'YYYY-MM-dd') AS date, count(m.id)) " +
          "FROM GroupMember m " +
          "WHERE (coalesce(:fromDate, NULL) IS NULL OR m.createdAt >= :fromDate) and m.state = 'PENDING' " +
          "GROUP BY date ")
  List<JoinGroupRequests> getJoinGroupRequestsPerDay(@Param("fromDate") Date fromDate);

  @Query("SELECT new iq.earthlink.social.groupservice.group.rest.JoinGroupRequests(to_char(m.createdAt, 'YYYY') AS date, count(m.id)) " +
          "FROM GroupMember m " +
          "WHERE (coalesce(:fromDate, NULL) IS NULL OR m.createdAt >= :fromDate) and m.state = 'PENDING' " +
          "GROUP BY date ")
  List<JoinGroupRequests> getJoinGroupRequestsPerYear(@Param("fromDate") Date fromDate);

  @Query("SELECT count(m.id) FROM GroupMember m WHERE m.group.id = :groupId AND m.state='PENDING'")
  Long countPendingJoinRequests(@Param("groupId") Long groupId);

  @Query("SELECT count(m.id) FROM GroupMember m WHERE m.group.id = :groupId AND m.state='APPROVED'")
  Long countGroupMembers(@Param("groupId") Long groupId);

  @Modifying
  @Query("UPDATE GroupMember m set m.publishedPostsCount = CASE WHEN (m.publishedPostsCount + :delta) < 0 THEN 0 " +
          "ELSE (m.publishedPostsCount + :delta) end where m.personId = :personId and m.group.id = :groupId")
  void updatePostCount(@Param("personId") Long personId, @Param("groupId") Long groupId, @Param("delta") long delta);

}
