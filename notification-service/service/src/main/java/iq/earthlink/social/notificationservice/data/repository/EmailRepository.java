package iq.earthlink.social.notificationservice.data.repository;

import iq.earthlink.social.notificationservice.data.model.Email;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface EmailRepository extends JpaRepository<Email, Long> {

    @Query("SELECT e FROM Email e " +
            "WHERE e.isSent=false " +
            "AND e.attemptsNumber<=?1 " +
            "ORDER BY e.createdAt ASC, e.attemptsNumber ASC")
    Page<Email> getPendingEmails(int attemptsLimit, Pageable pageable);

    Optional<Email> getByEventId(Long eventId);
}
