package iq.earthlink.social.personservice.outbox.service;

import iq.earthlink.social.classes.data.event.EmailEvent;
import iq.earthlink.social.common.util.CommonConstants;
import iq.earthlink.social.personservice.outbox.model.OutboxMessage;
import iq.earthlink.social.personservice.outbox.repository.OutboxMessageRepository;
import iq.earthlink.social.personservice.service.KafkaProducerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
@Slf4j
public class EmailOutboxService {

    private final OutboxMessageRepository outboxMessageRepository;

    private final KafkaProducerService kafkaProducerService;

    public EmailOutboxService(OutboxMessageRepository outboxMessageRepository, KafkaProducerService kafkaProducerService) {
        this.outboxMessageRepository = outboxMessageRepository;
        this.kafkaProducerService = kafkaProducerService;
    }

    @Async
    public void sendEmail(EmailEvent emailEvent) {
        OutboxMessage outboxMessage = new OutboxMessage();
        outboxMessage.setRecipientEmail(emailEvent.getRecipientEmail());
        outboxMessage.setEmailType(emailEvent.getEmailType());
        outboxMessage.setTemplateModel(emailEvent.getTemplateModel());
        OutboxMessage savedMessage = outboxMessageRepository.save(outboxMessage);
        emailEvent.setEventId(savedMessage.getId());

        try {
            kafkaProducerService.sendMessage(CommonConstants.EMAIL_SEND, emailEvent);

            outboxMessage.setSent(true);
            outboxMessage.setSentAt(new Date());
            outboxMessageRepository.save(outboxMessage);
        } catch (Exception ex) {
            log.error("'sendEmail:' couldn't sent message to kafka with topic: " + CommonConstants.EMAIL_SEND, ex);
            outboxMessage.setSent(false);
            outboxMessageRepository.save(outboxMessage);
        }
    }
}

