package iq.earthlink.social.groupservice.tag;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface TagManager {

  Tag createTag(Long personId, String name);

  Tag getTag(Long id);

  Page<Tag> findTags(String query, Pageable page);

  void delete(Long personId, boolean isAdmin, Long id);
}
