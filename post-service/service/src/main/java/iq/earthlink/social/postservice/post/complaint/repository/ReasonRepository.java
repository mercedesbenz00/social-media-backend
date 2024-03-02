package iq.earthlink.social.postservice.post.complaint.repository;

import iq.earthlink.social.postservice.post.complaint.ReasonSearchCriteria;
import iq.earthlink.social.postservice.post.complaint.model.Reason;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReasonRepository extends JpaRepository<Reason, Long> {

    @Query("SELECT r FROM Reason r " +
            "LEFT JOIN r.localizations l on LOWER(l.locale)=:#{#criteria.locale} " +
            "WHERE (:#{#criteria.query} IS NULL OR (LOWER(coalesce(l.name, r.name)) LIKE CONCAT('%', LOWER(?#{#criteria.query ?:''}), '%'))) ")
    Page<Reason> findReasons(@Param("criteria") ReasonSearchCriteria criteria, Pageable page);

}
