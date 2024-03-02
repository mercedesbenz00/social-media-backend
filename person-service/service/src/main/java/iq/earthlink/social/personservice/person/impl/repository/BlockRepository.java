package iq.earthlink.social.personservice.person.impl.repository;

import iq.earthlink.social.personservice.person.model.PersonBlock;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface BlockRepository extends JpaRepository<PersonBlock, Long> {

  @Query("SELECT b FROM PersonBlock b WHERE b.person.id = ?1")
  Page<PersonBlock> findBlocks(Long ownerId, Pageable page);

  @Query("DELETE FROM PersonBlock b WHERE b.person.id = ?1 AND b.blockedPerson.id = ?2")
  @Modifying
  void removeBlock(Long ownerId, Long blockedPersonId);
}
