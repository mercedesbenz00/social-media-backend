package iq.earthlink.social.groupservice.tag;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TagRepository extends JpaRepository<Tag, Long> {

  // TODO: query should be: SELECT t FROM Tag t WHERE (:q IS NULL OR lower(t.tag) LIKE concat(lower(:q, '%')))
  // TODO: but lower function called even when :q is null
  @Query("SELECT t FROM Tag t WHERE (:q IS NULL OR t.tagName LIKE %:q%)")
  Page<Tag> findTags(@Param("q") String query, Pageable page);
}
