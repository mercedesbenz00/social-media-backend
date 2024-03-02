package iq.earthlink.social.personservice.person.impl.repository;

import iq.earthlink.social.personservice.person.BanSearchCriteria;
import iq.earthlink.social.personservice.person.model.PersonBan;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Date;
import java.util.List;
import java.util.Optional;

public interface BanRepository extends JpaRepository<PersonBan, Long> {

  @Query("SELECT DISTINCT b FROM PersonBan b "
          + "WHERE ((:#{#criteria.active} = true AND b.expiredAt > :date) OR (:#{#criteria.active} = false AND b.expiredAt < :date)) "
          + "AND (:#{#criteria.query} IS NULL "
          + "OR LOWER(b.bannedPerson.firstName) LIKE :#{#criteria.query} "
          + "OR LOWER(b.bannedPerson.lastName) LIKE :#{#criteria.query} "
          + "OR LOWER(b.bannedPerson.displayName) LIKE :#{#criteria.query}) "
          + "AND (:#{#criteria.bannedPersonId} IS NULL "
          + "OR b.bannedPerson.id = :#{#criteria.bannedPersonId})")
  Page<PersonBan> findBans(@Param("criteria") BanSearchCriteria criteria, @Param("date") Date date, Pageable page);

  @Query("SELECT b FROM PersonBan b "
          + "WHERE b.bannedPerson.id = :personId "
          + "AND b.expiredAt > :expiredAt")
  List<PersonBan> findActiveBans(@Param("personId") Long personId, @Param("expiredAt") Date expiredAt);

  Optional<PersonBan> findByAuthorIdAndBannedPersonId(Long authorId, Long bannedPersonId);
}
