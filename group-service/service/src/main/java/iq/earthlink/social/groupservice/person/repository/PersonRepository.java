package iq.earthlink.social.groupservice.person.repository;

import iq.earthlink.social.groupservice.person.model.Person;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PersonRepository extends JpaRepository<Person, Long> {
    Optional<Person> getPersonByUuid(UUID personUuid);

    @Query(value = "SELECT * from person p " +
            "WHERE (:#{#query} IS NULL OR LOWER(p.display_name) LIKE CONCAT('%', LOWER(:#{#query ?:''}), '%')) " +
            "AND p.person_id NOT IN (:#{#excludePersonIds}) " +
            "LIMIT :queryLimit", nativeQuery = true)
    List<Person> getByDisplayNameWithExclusion(String query, List<Long> excludePersonIds, int queryLimit);

    Optional<Person> getPersonByPersonId(Long personId); // needs to be refactored

    List<Person> getPersonByPersonIdIn(List<Long> personId); // needs to be refactored

    List<Person> getPersonByUuidIn(List<UUID> personUuids);
}
