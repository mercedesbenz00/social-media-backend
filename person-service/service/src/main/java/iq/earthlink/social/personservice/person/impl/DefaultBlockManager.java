package iq.earthlink.social.personservice.person.impl;

import iq.earthlink.social.common.audit.AuditMarker;
import iq.earthlink.social.common.audit.AuditMessage;
import iq.earthlink.social.common.audit.EventAction;
import iq.earthlink.social.exception.NotUniqueException;
import iq.earthlink.social.personservice.person.BlockManager;
import iq.earthlink.social.personservice.person.impl.repository.BlockRepository;
import iq.earthlink.social.personservice.person.model.Person;
import iq.earthlink.social.personservice.person.model.PersonBlock;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Nonnull;

import static iq.earthlink.social.common.util.CommonConstants.ERROR_CHECK_NOT_NULL;
import static iq.earthlink.social.common.util.Preconditions.checkNotNull;

@Service
public class DefaultBlockManager implements BlockManager {

  private static final Logger LOGGER = LogManager.getLogger(DefaultBlockManager.class);

  private static final String OWNER = "owner";
  private static final String BLOCKED_PERSON = "blockedPerson";
  private static final String PAGE = "page";

  private final BlockRepository repository;

  public DefaultBlockManager(BlockRepository repository) {
    this.repository = repository;
  }

  @Transactional
  @Nonnull
  @Override
  public PersonBlock createBlock(@Nonnull Person owner, @Nonnull Person blockedPerson) {
    checkNotNull(owner, ERROR_CHECK_NOT_NULL, OWNER);
    checkNotNull(blockedPerson, ERROR_CHECK_NOT_NULL, BLOCKED_PERSON);

    PersonBlock block = new PersonBlock();
    block.setPerson(owner);
    block.setBlockedPerson(blockedPerson);

    try {
      PersonBlock savedBlock = repository.saveAndFlush(block);
      LOGGER.info("{} block successfully created", savedBlock);

      String logMessage = String.format("The the person \"%s\" was blocked", blockedPerson.getUsername());
      LOGGER.info(AuditMarker.getMarker(), AuditMessage.create(EventAction.BLOCK, logMessage, owner.getId(), blockedPerson.getId()));

      return savedBlock;
    } catch (DataIntegrityViolationException ex) {
      throw new NotUniqueException("error.person.is.already.blocked", blockedPerson.getId());
    }
  }

  @Transactional
  @Nonnull
  @Override
  public Page<PersonBlock> findBlocks(@Nonnull Person owner, @Nonnull Pageable page) {
    checkNotNull(owner, ERROR_CHECK_NOT_NULL, OWNER);
    checkNotNull(page, ERROR_CHECK_NOT_NULL, PAGE);

    return repository.findBlocks(owner.getId(), page);
  }

  @Transactional
  @Override
  public void removeBlock(@Nonnull Long ownerId, @Nonnull Person blockedPerson) {
    checkNotNull(ownerId, ERROR_CHECK_NOT_NULL, OWNER);
    checkNotNull(blockedPerson, ERROR_CHECK_NOT_NULL, BLOCKED_PERSON);

    repository.removeBlock(ownerId, blockedPerson.getId());
  }
}
