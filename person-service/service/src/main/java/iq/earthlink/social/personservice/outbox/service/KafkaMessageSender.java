package iq.earthlink.social.personservice.outbox.service;

import iq.earthlink.social.classes.data.event.EmailEvent;
import iq.earthlink.social.common.util.CommonConstants;
import iq.earthlink.social.personservice.outbox.model.OutboxMessage;
import iq.earthlink.social.personservice.outbox.repository.OutboxMessageRepository;
import iq.earthlink.social.personservice.service.KafkaProducerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class KafkaMessageSender {

    private final KafkaProducerService kafkaProducerService;

    private final OutboxMessageRepository outboxMessageRepository;

    @Scheduled(fixedDelayString = "${social.job.fixedDelay}")
    public void jobToProcessOutboxMessages() {
        List<OutboxMessage> messages = outboxMessageRepository.findByIsSentFalse();

        for (OutboxMessage message : messages) {
            EmailEvent emailEvent = new EmailEvent();
            emailEvent.setRecipientEmail(message.getRecipientEmail());
            emailEvent.setEmailType(message.getEmailType());
            emailEvent.setTemplateModel(message.getTemplateModel());
            OutboxMessage outboxMessage = outboxMessageRepository.save(message);
            emailEvent.setEventId(outboxMessage.getId());

            try {
                kafkaProducerService.sendMessage(CommonConstants.EMAIL_SEND, emailEvent);

                message.setSent(true);
                message.setSentAt(new Date());
                outboxMessageRepository.save(message);
            } catch (Exception ex) {
                log.error("'jobToProcessOutboxMessages:' couldn't sent message to kafka with topic: " + CommonConstants.EMAIL_SEND, ex);
            }
            message.setAttemptsNumber(message.getAttemptsNumber() + 1);
            outboxMessageRepository.save(message);
        }
    }
}

