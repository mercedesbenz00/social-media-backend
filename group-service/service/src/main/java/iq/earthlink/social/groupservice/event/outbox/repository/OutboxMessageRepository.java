package iq.earthlink.social.groupservice.event.outbox.repository;

import iq.earthlink.social.groupservice.event.outbox.MessageStatus;
import iq.earthlink.social.groupservice.event.outbox.model.OutboxMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

@Repository
public interface OutboxMessageRepository extends JpaRepository<OutboxMessage, Long> {
    @Query("SELECT om FROM OutboxMessage om WHERE om.status='PENDING' AND om.attemptsNumber < :maxAttemptsNumber")
    List<OutboxMessage> findPendingMessages(@Param("maxAttemptsNumber") int maxAttemptsNumber);

    @Modifying
    @Query("UPDATE OutboxMessage om SET om.status = :status WHERE om.id IN :messageIds")
    void updateMessageStatusesByIds(@Param("messageIds") Set<Long> messageIds, @Param("status") MessageStatus status);
}