package iq.earthlink.social.personservice.person.impl.repository;

import iq.earthlink.social.personservice.person.PersonSearchCriteria;
import iq.earthlink.social.personservice.person.model.Person;
import iq.earthlink.social.personservice.person.rest.ActivatedUsers;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;

import javax.persistence.QueryHint;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PersonRepository extends JpaRepository<Person, Long> {

    Person findByEmailIgnoreCase(String email);

    @QueryHints(@QueryHint(name = org.hibernate.annotations.QueryHints.CACHEABLE, value = "true"))
    Optional<Person> findByUsernameIgnoreCase(String username);

    @QueryHints(@QueryHint(name = org.hibernate.annotations.QueryHints.CACHEABLE, value = "true"))
    Optional<Person> findByUuid(UUID uuid);

    @QueryHints(@QueryHint(name = org.hibernate.annotations.QueryHints.CACHEABLE, value = "true"))
    @Query("SELECT p FROM Person p "
            + "WHERE (:#{#criteria.query} = '%' "
            + "OR LOWER(p.firstName) LIKE :#{#criteria.query} "
            + "OR LOWER(p.lastName) LIKE :#{#criteria.query} "
            + "OR LOWER(p.lastFirstName) LIKE :#{#criteria.query} "
            + "OR LOWER(p.firstLastName) LIKE :#{#criteria.query} "
            + "OR LOWER(p.displayName) LIKE :#{#criteria.query} "
            + "OR SIMILARITY(p.firstName, :#{#criteria.query}) > :#{#criteria.similarityThreshold} "
            + "OR SIMILARITY(p.lastName, :#{#criteria.query}) > :#{#criteria.similarityThreshold} "
            + "OR SIMILARITY(p.lastFirstName, :#{#criteria.query}) > :#{#criteria.similarityThreshold} "
            + "OR SIMILARITY(p.firstLastName, :#{#criteria.query}) > :#{#criteria.similarityThreshold} "
            + "OR SIMILARITY(p.displayName, :#{#criteria.query}) > :#{#criteria.similarityThreshold}) "
            + "AND (:#{#criteria.displayNameQuery} = '%' "
            + "OR LOWER(p.displayName) LIKE :#{#criteria.displayNameQuery} "
            + "OR SIMILARITY(p.displayName, :#{#criteria.displayNameQuery}) > :#{#criteria.similarityThreshold})"
            + "AND (coalesce(:#{#criteria.personIds}, NULL) IS NULL OR p.id IN ?#{#criteria.personIds}) "
            + "AND (coalesce(:#{#criteria.personIdsToExclude}, NULL) IS NULL OR p.id NOT IN ?#{#criteria.personIdsToExclude}) "
            + "AND (:#{#criteria.showDeleted} = true OR p.deletedDate IS NULL)")
    Page<Person> findPersons(@Param("criteria") PersonSearchCriteria criteria, Pageable page);

    @QueryHints(@QueryHint(name = org.hibernate.annotations.QueryHints.CACHEABLE, value = "true"))
    @Query("SELECT p FROM Person p "
            + "WHERE (:#{#criteria.query} = '%' "
            + "OR LOWER(p.firstName) LIKE :#{#criteria.query} "
            + "OR LOWER(p.lastName) LIKE :#{#criteria.query} "
            + "OR LOWER(p.lastFirstName) LIKE :#{#criteria.query} "
            + "OR LOWER(p.firstLastName) LIKE :#{#criteria.query} "
            + "OR LOWER(p.displayName) LIKE :#{#criteria.query} "
            + "OR SIMILARITY(p.firstName, :#{#criteria.query}) > :#{#criteria.similarityThreshold} "
            + "OR SIMILARITY(p.lastName, :#{#criteria.query}) > :#{#criteria.similarityThreshold} "
            + "OR SIMILARITY(p.lastFirstName, :#{#criteria.query}) > :#{#criteria.similarityThreshold} "
            + "OR SIMILARITY(p.firstLastName, :#{#criteria.query}) > :#{#criteria.similarityThreshold} "
            + "OR SIMILARITY(p.displayName, :#{#criteria.query}) > :#{#criteria.similarityThreshold}) "
            + "AND (:#{#criteria.displayNameQuery} = '%' "
            + "OR LOWER(p.displayName) LIKE :#{#criteria.displayNameQuery} "
            + "OR SIMILARITY(p.displayName, :#{#criteria.displayNameQuery}) > :#{#criteria.similarityThreshold})"
            + "AND (coalesce(:#{#criteria.personIds}, NULL) IS NULL OR p.id IN ?#{#criteria.personIds}) "
            + "AND (coalesce(:#{#criteria.personIdsToExclude}, NULL) IS NULL OR p.id NOT IN ?#{#criteria.personIdsToExclude}) "
            + "AND (:#{#criteria.showDeleted} = true OR p.deletedDate IS NULL) "
            + "ORDER BY SIMILARITY(p.displayName, :#{#criteria.query}) DESC, SIMILARITY(p.displayName, :#{#criteria.displayNameQuery}) DESC")
    Page<Person> findPersonsOrderedBySimilarity(@Param("criteria") PersonSearchCriteria criteria, Pageable page);

    @QueryHints(@QueryHint(name = org.hibernate.annotations.QueryHints.CACHEABLE, value = "true"))
    @Query("SELECT p FROM Person p "
            + "WHERE (:#{#criteria.query} = '%' "
            + "OR LOWER(p.firstName) LIKE :#{#criteria.query} "
            + "OR LOWER(p.lastName) LIKE :#{#criteria.query} "
            + "OR LOWER(p.lastFirstName) LIKE :#{#criteria.query} "
            + "OR LOWER(p.firstLastName) LIKE :#{#criteria.query} "
            + "OR LOWER(p.displayName) LIKE :#{#criteria.query} "
            + "OR SIMILARITY(p.firstName, :#{#criteria.query}) > :#{#criteria.similarityThreshold} "
            + "OR SIMILARITY(p.lastName, :#{#criteria.query}) > :#{#criteria.similarityThreshold} "
            + "OR SIMILARITY(p.lastFirstName, :#{#criteria.query}) > :#{#criteria.similarityThreshold} "
            + "OR SIMILARITY(p.firstLastName, :#{#criteria.query}) > :#{#criteria.similarityThreshold} "
            + "OR SIMILARITY(p.displayName, :#{#criteria.query}) > :#{#criteria.similarityThreshold}) "
            + "AND (:#{#criteria.displayNameQuery} = '%' "
            + "OR LOWER(p.displayName) LIKE :#{#criteria.displayNameQuery} "
            + "OR SIMILARITY(p.displayName, :#{#criteria.displayNameQuery}) > :#{#criteria.similarityThreshold})"
            + "AND (coalesce(:#{#criteria.personIds}, NULL) IS NULL OR p.id IN ?#{#criteria.personIds}) "
            + "AND (coalesce(:#{#criteria.personIdsToExclude}, NULL) IS NULL OR p.id NOT IN ?#{#criteria.personIdsToExclude}) "
            + "AND (:#{#criteria.showDeleted} = true OR p.deletedDate IS NULL) "
            + "AND p.id <> :#{#criteria.currentPersonId} "
            + "ORDER BY "
            + "CASE "
            + "WHEN p.id IN :subscribedTo THEN 1 "
            + "WHEN p.id IN :followers THEN 2 "
            + "ELSE 3 "
            + "END, "
            + "p.displayName ASC")
    Page<Person> findPersonsWithFollowingsFirst(@Param("criteria") PersonSearchCriteria criteria,
                                                @Param("followers") List<Long> followerIds,
                                                @Param("subscribedTo") List<Long> subscribedToIds, Pageable page);

    @Query("SELECT p FROM Person p " +
            "WHERE p.deletedDate IS NOT NULL " +
            "AND p.deletedDate < :deleteBefore " +
            "AND p.email IS NOT NULL")
    Iterable<Person> findInactiveProfiles(Date deleteBefore);

    @Query("SELECT new iq.earthlink.social.personservice.person.rest.ActivatedUsers(to_char(p.createdAt, 'YYYY-MM-dd') AS date, count(p.id)) " +
            "FROM Person p " +
            "WHERE (coalesce(:fromDate, NULL) IS NULL OR p.createdAt >= :fromDate) and p.deletedDate IS NULL " +
            "and p.isRegistrationCompleted = true and p.isConfirmed = true " +
            "GROUP BY date")
    List<ActivatedUsers> getActivatedUsersPerDay(@Param("fromDate") Date fromDate);

    @Query("SELECT new iq.earthlink.social.personservice.person.rest.ActivatedUsers(to_char(p.createdAt, 'YYYY-MM') AS date, count(p.id)) " +
            "FROM Person p " +
            "WHERE (coalesce(:fromDate, NULL) IS NULL OR p.createdAt >= :fromDate) and p.deletedDate IS NULL " +
            "and p.isRegistrationCompleted = true and p.isConfirmed = true " +
            "GROUP BY date")
    List<ActivatedUsers> getActivatedUsersPerMonth(@Param("fromDate") Date fromDate);

    @Query("SELECT new iq.earthlink.social.personservice.person.rest.ActivatedUsers(to_char(p.createdAt, 'YYYY') AS date, count(p.id)) " +
            "FROM Person p " +
            "WHERE (coalesce(:fromDate, NULL) IS NULL OR p.createdAt >= :fromDate) and p.deletedDate IS NULL " +
            "and p.isRegistrationCompleted = true and p.isConfirmed = true " +
            "GROUP BY date")
    List<ActivatedUsers> getActivatedUsersPerYear(@Param("fromDate") Date fromDate);

    @Query("SELECT count(p.id) from Person p " +
            "WHERE p.deletedDate IS NULL and p.isRegistrationCompleted = true and p.isConfirmed = true")
    Long getAllUsersCount();

    @Query("SELECT count(p.id) from Person p " +
            "WHERE (coalesce(:fromDate, NULL) IS NULL OR p.createdAt >= :fromDate) and p.deletedDate IS NULL " +
            "and p.isRegistrationCompleted = true and p.isConfirmed = true")
    Long getNewUsersCount(@Param("fromDate") Date fromDate);

    @Modifying
    @Query("UPDATE Person p set p.followerCount = CASE WHEN (p.followerCount + :delta) < 0 THEN 0 ELSE (p.followerCount + :delta) end where p.id = :personId")
    void updateUserFollowersCount(@Param("personId") Long personId, @Param("delta") long delta);

    @Modifying
    @Query("UPDATE Person p set p.followingCount = CASE WHEN (p.followingCount + :delta) < 0 THEN 0 ELSE (p.followingCount + :delta) end where p.id = :personId")
    void updateUserFollowingCount(@Param("personId") Long personId, @Param("delta") long delta);


    @Modifying
    @Query("UPDATE Person p set p.groupCount = CASE WHEN (p.groupCount + :delta) < 0 THEN 0 ELSE (p.groupCount + :delta) end where p.id = :personId")
    void updateUserGroupsCount(@Param("personId") Long personId, @Param("delta") long delta);

    @Modifying
    @Query("UPDATE Person p set p.postCount = CASE WHEN (p.postCount + :delta) < 0 THEN 0 ELSE (p.postCount + :delta) end where p.id = :personId")
    void updatePostCount(@Param("personId") Long personId, @Param("delta") long delta);

    @Modifying
    @Query(value = "UPDATE PERSON PP " +
            "SET FOLLOWING_COUNT = " +
            "(SELECT COUNT(*)  " +
            "FROM FOLLOWING F " +
            "WHERE F.SUBSCRIBER_ID = PP.ID " +
            "GROUP BY F.SUBSCRIBER_ID) " +
            "WHERE PP.ID IN " +
            "(SELECT P.ID " +
            "FROM PERSON P, " +
            "(SELECT COUNT(*) AS ACTUAL_COUNT, " +
            "F.SUBSCRIBER_ID " +
            "FROM FOLLOWING F " +
            "GROUP BY F.SUBSCRIBER_ID) S " +
            "WHERE P.ID = S.SUBSCRIBER_ID " +
            "AND FOLLOWING_COUNT <> ACTUAL_COUNT)", nativeQuery = true)
    void syncPersonsFollowingCount();

    @Modifying
    @Query(value = "UPDATE PERSON PP " +
            "SET FOLLOWER_COUNT = " +
            "(SELECT COUNT(*) " +
            "FROM FOLLOWING F " +
            "WHERE F.SUBSCRIBED_TO_ID = PP.ID " +
            "GROUP BY F.SUBSCRIBED_TO_ID) " +
            "WHERE PP.ID IN " +
            "(SELECT P.ID " +
            "FROM PERSON P, " +
            "(SELECT COUNT(*) AS ACTUAL_COUNT, " +
            "F.SUBSCRIBED_TO_ID " +
            "FROM FOLLOWING F " +
            "GROUP BY F.SUBSCRIBED_TO_ID) S " +
            "WHERE P.ID = S.SUBSCRIBED_TO_ID " +
            "AND FOLLOWER_COUNT <> ACTUAL_COUNT)", nativeQuery = true)
    void syncPersonsFollowersCount();


}
