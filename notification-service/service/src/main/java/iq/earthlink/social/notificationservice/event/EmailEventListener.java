package iq.earthlink.social.notificationservice.event;

import iq.earthlink.social.classes.data.event.EmailEvent;
import iq.earthlink.social.common.util.CommonConstants;
import iq.earthlink.social.notificationservice.service.notification.EmailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class EmailEventListener {

    private final EmailService emailService;

    public EmailEventListener(EmailService emailService) {
        this.emailService = emailService;
    }

    @KafkaListener(topics = CommonConstants.EMAIL_SEND, groupId = "notification-service-email",
            containerFactory = "emailListenerContainerFactory")
    public void emailEvent(@Payload EmailEvent emailEvent) {
        if(emailService.getEmailByEventId(emailEvent.getEventId()).isEmpty()) {
            emailService.sendUsingTemplateAsync(emailEvent.getRecipientEmail(),
                    emailEvent.getEmailType(), emailEvent.getTemplateModel(), emailEvent.getEventId());
        } else {
            log.info("Event with id = {}, has already been received", emailEvent.getEventId());
        }
    }
}
