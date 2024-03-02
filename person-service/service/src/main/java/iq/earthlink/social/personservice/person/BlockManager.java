package iq.earthlink.social.personservice.person;

import iq.earthlink.social.personservice.person.model.Person;
import iq.earthlink.social.personservice.person.model.PersonBlock;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import javax.annotation.Nonnull;

public interface BlockManager {

  @Nonnull
  PersonBlock createBlock(@Nonnull Person owner, @Nonnull Person blockedPerson);

  @Nonnull
  Page<PersonBlock> findBlocks(@Nonnull Person owner, @Nonnull Pageable page);

  void removeBlock(@Nonnull Long ownerId, @Nonnull Person blockedPerson);

}
