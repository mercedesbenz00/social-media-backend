package iq.earthlink.social.notificationservice.service.notification;

import iq.earthlink.social.classes.enumeration.EmailType;
import iq.earthlink.social.notificationservice.data.model.Email;
import iq.earthlink.social.notificationservice.data.repository.EmailRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring5.SpringTemplateEngine;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender emailSender;

    private final SpringTemplateEngine thymeleafTemplateEngine;

    private final EmailRepository emailRepository;

    @Value( "${spring.mail.from}" )
    private String from;

    @Async
    public void sendUsingTemplateAsync(String recipientEmail, EmailType emailType, Map<String, Object> templateModel, Long eventId) {
        String htmlBody = getHtmlBody(emailType, templateModel);
        try {
            sendEmail(recipientEmail, emailType.getTitle(), htmlBody);
            saveEmail(recipientEmail, emailType.getTitle(), templateModel, emailType, eventId, true);
        } catch (Exception ex) {
            log.error("'sendEmailSync:' couldn't sent an email to: " + recipientEmail);
            saveEmail(recipientEmail, emailType.getTitle(), templateModel, emailType, eventId);
        }
    }

    public Optional<Email> getEmailByEventId(Long eventId) {
        return emailRepository.getByEventId(eventId);
    }

    private void sendEmail(String to, String subject, String htmlBody) throws MessagingException {
        MimeMessage message = emailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setFrom(this.from);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(htmlBody, true);
        emailSender.send(message);
    }

    private void saveEmail(String recipientEmail, String subject, Map<String, Object> templateModel, EmailType emailType, Long eventId) {
        saveEmail(recipientEmail, subject, templateModel, emailType, eventId, false);
    }

    private void saveEmail(String recipientEmail, String subject, Map<String, Object> templateModel, EmailType emailType, Long eventId, boolean isSent) {
        Email email = Email.builder()
                .recipientEmail(recipientEmail)
                .subject(subject)
                .templateModel(templateModel)
                .type(emailType)
                .isSent(isSent)
                .eventId(eventId)
                .build();
        if (isSent) {
            email.setSentAt(new Date());
        }
        emailRepository.save(email);
    }

    private String getHtmlBody(EmailType emailType, Map<String, Object> templateModel) {
        Locale locale = LocaleContextHolder.getLocale();
        Context thymeleafContext = new Context(locale);
        thymeleafContext.setVariables(templateModel);
        return thymeleafTemplateEngine.process(emailType.getCode(), thymeleafContext);
    }

    @Scheduled(fixedDelayString = "${social.job.fixedDelay}", initialDelayString = "${social.job.initialDelay}")
    public void jobToSendEmail() {
        Page<Email> listOfEmails = emailRepository.getPendingEmails(3, PageRequest.of(0, 10));
        listOfEmails.forEach(email -> {
            try {
                String htmlBody = getHtmlBody(email.getType(), email.getTemplateModel());
                sendEmail(email.getRecipientEmail(), email.getSubject(), htmlBody);
                email.setSent(true);
                email.setSentAt(new Date());
            } catch (Exception ex) {
                log.error("'jobToSendEmail:' couldn't sent an email to: " + email.getRecipientEmail(), ex);
                email.setAttemptsNumber(email.getAttemptsNumber() + 1);
            }
            emailRepository.save(email);
        });
    }
}
