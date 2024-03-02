package iq.earthlink.social.postservice.person.repository;

import iq.earthlink.social.postservice.person.model.Person;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PersonRepository extends JpaRepository<Person, Long> {
    Optional<Person> getPersonByUuid(UUID personUuid);

    Optional<Person> getPersonByPersonId(Long personId); // needs to be refactored

    List<Person> getPersonByPersonIdIn(List<Long> personId); // needs to be refactored

    List<Person> getPersonByUuidIn(List<UUID> personUuids);
}
