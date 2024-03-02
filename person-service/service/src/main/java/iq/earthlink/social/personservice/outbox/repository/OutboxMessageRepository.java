package iq.earthlink.social.personservice.outbox.repository;

import iq.earthlink.social.personservice.outbox.model.OutboxMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OutboxMessageRepository extends JpaRepository<OutboxMessage, Long> {
    List<OutboxMessage> findByIsSentFalse();
}
