package iq.earthlink.social.personservice.person.impl.repository;

import iq.earthlink.social.personservice.person.model.PersonGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PersonGroupsRepository extends JpaRepository<PersonGroup, Long> {

    Optional<PersonGroup> getByPersonIdAndGroupId(Long personId, Long groupId);
}
