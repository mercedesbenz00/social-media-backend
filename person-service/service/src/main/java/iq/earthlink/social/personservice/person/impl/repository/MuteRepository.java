package iq.earthlink.social.personservice.person.impl.repository;

import iq.earthlink.social.personservice.person.model.PersonMute;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface MuteRepository extends JpaRepository<PersonMute, Long> {

  Page<PersonMute> findAllByPersonId(Long personId, Pageable page);

  @Query("DELETE FROM PersonMute m WHERE m.person.id = ?1 AND m.mutedPerson.id = ?2")
  @Modifying
  void removeMute(Long personOwnerId, Long personMutedId);

  List<PersonMute> findAllByMutedPersonId(Long mutedPersonId);
}
