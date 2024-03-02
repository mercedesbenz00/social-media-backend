package iq.earthlink.social.personservice.person.impl.repository;

import feign.Param;
import iq.earthlink.social.personservice.person.BanSearchCriteria;
import iq.earthlink.social.personservice.person.model.PersonGroupBan;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Date;
import java.util.List;
import java.util.Optional;

public interface GroupBanRepository extends JpaRepository<PersonGroupBan, Long> {

 @Query("SELECT DISTINCT b FROM PersonGroupBan b "
         + "WHERE ((:#{#criteria.active} = true AND b.expiredAt > :date) OR (:#{#criteria.active} = false AND b.expiredAt < :date)) "
         + "AND (:#{#criteria.query} IS NULL "
         + "OR LOWER(b.bannedPerson.firstName) LIKE :#{#criteria.query} "
         + "OR LOWER(b.bannedPerson.lastName) LIKE :#{#criteria.query} "
         + "OR LOWER(b.bannedPerson.displayName) LIKE :#{#criteria.query}) "
         + "AND ((:#{#criteria.bannedPersonIds}) IS NULL "
         + "OR b.bannedPerson.id IN (:#{#criteria.bannedPersonIds})) "
         + "AND ((:#{#criteria.groupIds}) IS NULL OR b.userGroupId IN (:#{#criteria.groupIds}))")
 Page<PersonGroupBan> findGroupBans(@Param("criteria") BanSearchCriteria criteria, @Param("date") Date date, Pageable page);

 @Query("SELECT b FROM PersonGroupBan b "
         + "WHERE b.bannedPerson.id = :personId "
         + "AND b.userGroupId= :groupId "
         + "AND b.expiredAt > :expiredAt")
 List<PersonGroupBan> findActiveBans(@Param("personId") Long personId, @Param("groupId") Long groupId, @Param("expiredAt") Date expiredAt);

 Optional<PersonGroupBan> findByAuthorIdAndBannedPersonIdAndUserGroupId(Long authorId, Long bannedPersonId, Long userGroupId);
}
