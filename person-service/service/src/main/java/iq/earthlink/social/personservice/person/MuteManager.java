package iq.earthlink.social.personservice.person;

import iq.earthlink.social.personservice.person.model.Person;
import iq.earthlink.social.personservice.person.model.PersonMute;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import javax.annotation.Nonnull;
import java.util.List;

public interface MuteManager {

  /**
   * Finds mutes created by the owner person.
   *
   * @param owner the mutes' owner person
   * @param page the pagination params
   */
  @Nonnull
  Page<PersonMute> findMutes(@Nonnull Person owner, @Nonnull Pageable page);

  /**
   * Creates new mute record created by the owner against muted person.
   *
   * @param owner the mute's owner person
   * @param mutedPerson the muted person
   */
  @Nonnull
  PersonMute createMute(@Nonnull Person owner, @Nonnull Person mutedPerson);

  /**
   * Removes previously created mute for muted person.
   * @param ownerId the mute's owner person id
   * @param mutedPerson the muted person
   */
  void removeMute(@Nonnull Long ownerId, @Nonnull Person mutedPerson);

  List<Long> findWhoMutes(Long personId);
}
