package iq.earthlink.social.groupservice.event.outbox.service;

import iq.earthlink.social.groupservice.event.outbox.MessageStatus;
import iq.earthlink.social.groupservice.event.outbox.model.OutboxMessage;
import iq.earthlink.social.groupservice.event.outbox.repository.OutboxMessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@Slf4j
@RequiredArgsConstructor
public class KafkaMessageSender {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    private final OutboxMessageRepository outboxMessageRepository;

    @Scheduled(fixedDelayString = "${social.job.fixedDelay}")
    @Transactional
    public void jobToProcessOutboxMessages() {
        int maxAttempts = 3;
        List<OutboxMessage> messages = outboxMessageRepository.findPendingMessages(maxAttempts);
        Set<Long> messageIds = messages.stream().map(OutboxMessage::getId).collect(Collectors.toSet());
        if (!messageIds.isEmpty()) {
            outboxMessageRepository.updateMessageStatusesByIds(messageIds, MessageStatus.IN_PROGRESS);
        }
        for (OutboxMessage outboxMessage : messages) {
            try {
                kafkaTemplate.send(outboxMessage.getTopic(), outboxMessage.getKey(), outboxMessage.getPayload()).get();
                outboxMessage.setStatus(MessageStatus.IS_SENT);
                outboxMessage.setSentAt(new Date());
                outboxMessageRepository.save(outboxMessage);
            } catch (Exception ex) {
                log.error("'jobToProcessOutboxMessages:' couldn't sent message to kafka with topic: " + outboxMessage.getTopic(), ex);
                if (outboxMessage.getAttemptsNumber() + 1 >= maxAttempts) {
                    outboxMessage.setStatus(MessageStatus.FAILED);
                    outboxMessage.setNotes(ex.getMessage());
                } else {
                    outboxMessage.setStatus(MessageStatus.PENDING);
                }
            }
            outboxMessage.setAttemptsNumber(outboxMessage.getAttemptsNumber() + 1);
            outboxMessageRepository.save(outboxMessage);
        }
    }
}

