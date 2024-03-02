package iq.earthlink.social.personservice.person.impl.repository;

import iq.earthlink.social.personservice.person.model.PersonConfiguration;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PersonConfigurationRepository extends JpaRepository<PersonConfiguration, String> {

  Optional<PersonConfiguration> findByPersonId(Long personId);
}
