package iq.earthlink.social.groupservice.tag;

import iq.earthlink.social.exception.ForbiddenException;
import iq.earthlink.social.exception.NotFoundException;
import iq.earthlink.social.exception.NotUniqueException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static iq.earthlink.social.common.util.Preconditions.checkNotNull;

@Service
public class DefaultTagManager implements TagManager {

  private static final Logger LOGGER = LoggerFactory.getLogger(DefaultTagManager.class);

  private final TagRepository repository;

  public DefaultTagManager(TagRepository repository) {
    this.repository = repository;
  }

  @Transactional
  @Override
  public Tag createTag(Long personId, String name) {
    checkNotNull(personId, "error.check.not.null", "personId");
    checkNotNull(name, "error.check.not.null", "name");

    Tag t = new Tag();
    t.setTagName(name);
    t.setAuthorId(personId);

    try {
      return repository.saveAndFlush(t);
    } catch (DataIntegrityViolationException ex) {
      throw new NotUniqueException("error.tag.already.exists", name);
    }
  }

  @Override
  public Tag getTag(Long id) {
    return repository.findById(id)
        .orElseThrow(() -> new NotFoundException("error.not.found.tag", id));
  }

  @Override
  public Page<Tag> findTags(String query, Pageable page) {
    return repository.findTags(query, page);
  }

  @Transactional
  @Override
  public void delete(Long personId, boolean isAdmin, Long id) {

    Tag tag = getTag(id);
    if (isAdmin) {
      repository.delete(tag);
      LOGGER.info("Tag {} successfully deleted by {}", tag, personId);
    } else {
      throw new ForbiddenException("error.operation.not.permitted");
    }

  }
}
