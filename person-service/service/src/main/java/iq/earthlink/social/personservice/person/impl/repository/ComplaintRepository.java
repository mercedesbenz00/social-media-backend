package iq.earthlink.social.personservice.person.impl.repository;

import iq.earthlink.social.personservice.person.PersonComplaintSearchCriteria;
import iq.earthlink.social.personservice.person.model.PersonComplaint;
import iq.earthlink.social.personservice.person.model.PersonComplaintStats;
import iq.earthlink.social.personservice.person.model.ReportedPerson;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ComplaintRepository extends JpaRepository<PersonComplaint, Long> {

    @Query("SELECT c FROM PersonComplaint c WHERE c.owner.id = ?1")
    Page<PersonComplaint> findPersonComplaints(Long personId, Pageable page);

    @Query("DELETE FROM PersonComplaint c WHERE c.owner.id = ?1 AND c.person.id = ?2")
    @Modifying
    void removeComplaint(Long ownerId, Long personId);

    @Query("SELECT c FROM PersonComplaint c " +
            "WHERE (coalesce(:personIds, null) IS NULL OR c.person.id IN :personIds) " +
            "AND (coalesce(:userGroupIds, null) IS NULL OR c.userGroupId IN :userGroupIds) " +
            "AND c.state = :state")
    Page<PersonComplaint> findByPersonsAndGroupsAndState(@Param("personIds") List<Long> personIds, @Param("userGroupIds") List<Long> userGroupIds,
                                                         @Param("state") PersonComplaint.PersonComplaintState state, Pageable page);

    @Query("SELECT c.person.id as id, " +
            "avatarFile as avatar, " +
            "c.person.displayName as displayName, " +
            "c.person.createdAt as createdAt, " +
            "c.person.postCount as postCount, " +
            "count(*) as totalCount, " +
            "(CASE WHEN p.expiredAt>CURRENT_DATE THEN TRUE ELSE FALSE END) as isBanned, " +
            "(CASE WHEN p.expiredAt>CURRENT_DATE THEN p.expiredAt END) as banExpiresAt " +
            "FROM PersonComplaint c LEFT JOIN PersonBan p " +
            "ON c.person.id = p.bannedPerson.id " +
            "LEFT JOIN MediaFile avatarFile " +
            "ON c.person.avatar.id = avatarFile.id " +
            "WHERE c.state=:#{#criteria.complaintState} " +
            "AND DATE(c.createdAt) >=:#{#criteria.fromDate} and DATE(c.createdAt)<=:#{#criteria.toDate} " +
            "AND (:#{#criteria.personStatus.name()}='ANY' " +
            "OR (:#{#criteria.personStatus.name()}='ACTIVE' AND (p.expiredAt IS NULL OR  p.expiredAt < CURRENT_DATE)) " +
            "OR (:#{#criteria.personStatus.name()}='BANNED' AND p.expiredAt > CURRENT_DATE)) " +
            "AND (coalesce(:#{#criteria.personIds}, null) IS NULL OR c.person.id IN :#{#criteria.personIds}) " +
            "AND c.person.deletedDate IS NULL " +
            "AND (:#{#criteria.query} = '%' OR LOWER(c.person.displayName) LIKE :#{#criteria.query} " +
            "OR SIMILARITY(c.person.displayName, :#{#criteria.query}) > :#{#criteria.similarityThreshold}) " +
            "GROUP BY c.person.id, avatarFile.id, c.person.displayName, c.person.postCount, c.person.createdAt, p.expiredAt " +
            "ORDER BY totalCount DESC")
    Page<ReportedPerson> findPersonsWithComplaints(@Param("criteria") PersonComplaintSearchCriteria criteria,
                                                   Pageable page);


    @Query("SELECT count(p.id) as  complaintCount, max(p.createdAt) as lastComplaintDate from PersonComplaint p " +
            "where p.person.id = :personId and p.state = :state")
    PersonComplaintStats getPersonComplainStats(@Param("personId") Long personId, PersonComplaint.PersonComplaintState state);

    List<PersonComplaint> findByPersonIdAndStateAndUserGroupIdIsNull(Long personId, PersonComplaint.PersonComplaintState state);

    List<PersonComplaint> findByPersonIdAndState(Long personId, PersonComplaint.PersonComplaintState state);

    List<PersonComplaint> findByPersonIdAndStateAndUserGroupId(Long personId, PersonComplaint.PersonComplaintState state, Long userGroupId);

    Optional<PersonComplaint> findByOwnerIdAndPersonId(Long ownerId, Long personId);

    List<PersonComplaint> findByPersonIdAndStateAndUserGroupIdIn(Long personId, PersonComplaint.PersonComplaintState state, List<Long> userGroupIds);

}
