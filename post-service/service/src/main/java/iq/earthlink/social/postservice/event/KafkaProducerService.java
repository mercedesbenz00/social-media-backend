package iq.earthlink.social.postservice.event;

import iq.earthlink.social.postservice.event.outbox.MessageStatus;
import iq.earthlink.social.postservice.event.outbox.model.OutboxMessage;
import iq.earthlink.social.postservice.event.outbox.repository.OutboxMessageRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
@RequiredArgsConstructor
public class KafkaProducerService {
    private static final Logger logger =
            LoggerFactory.getLogger(KafkaProducerService.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final OutboxMessageRepository outboxMessageRepository;

    public void sendMessage(String topic, Object message) {
        sendMessage(topic, null, message);
    }

    @Async
    public void sendMessage(String topic, String key, Object message) {
        try {
            kafkaTemplate.send(topic, key, message).get();
            logger.info("Message sent -> {}", message);
        } catch (Exception ex) {
            logger.error("Issue with sending kafka message: {}", ex.getMessage());
            var outboxMessage = OutboxMessage.builder()
                    .topic(topic)
                    .key(key)
                    .payload(message)
                    .status(MessageStatus.PENDING)
                    .createdAt(new Date())
                    .attemptsNumber(1)
                    .build();
            outboxMessageRepository.save(outboxMessage);
        }
    }
}
