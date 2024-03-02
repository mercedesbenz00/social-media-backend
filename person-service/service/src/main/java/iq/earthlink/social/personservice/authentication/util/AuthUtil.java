package iq.earthlink.social.personservice.authentication.util;

import iq.earthlink.social.classes.data.event.EmailEvent;
import iq.earthlink.social.classes.enumeration.EmailType;
import iq.earthlink.social.common.util.CommonUtil;
import iq.earthlink.social.exception.BadRequestException;
import iq.earthlink.social.personservice.outbox.service.EmailOutboxService;
import iq.earthlink.social.personservice.person.model.Person;
import iq.earthlink.social.personservice.util.CommonProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

import static iq.earthlink.social.personservice.util.Constants.EMAIL;

@Component
public class AuthUtil {

    private final EmailOutboxService emailOutboxService;
    private final CommonProperties commonProperties;

    public AuthUtil(EmailOutboxService emailOutboxService, CommonProperties commonProperties) {
        this.emailOutboxService = emailOutboxService;
        this.commonProperties = commonProperties;
    }

    public void sendConfirmationEmail(Person person) {
        Map<String, Object> emailTemplateModel = new HashMap<>();
        String confirmURL = generateURL("/auth/confirm", Map.of("code", person.getConfirmCode(), EMAIL, person.getEmail()));
        emailTemplateModel.put("firstName", person.getFirstName() != null ? person.getFirstName() : "User");
        emailTemplateModel.put(EMAIL, person.getEmail());
        emailTemplateModel.put("confirmURL", confirmURL);
        emailTemplateModel.put("code", person.getConfirmCode());
        emailOutboxService.sendEmail(EmailEvent.builder()
                .recipientEmail(person.getEmail())
                .emailType(EmailType.EMAIL_CONFIRMATION)
                .templateModel(emailTemplateModel)
                .build());
    }

    public void sendResetPasswordEmail(Person person, Integer code) {
        Map<String, Object> emailTemplateModel = new HashMap<>();
        String resetPasswordURL = generateURL("/auth/reset-password", Map.of("code", code.toString(), EMAIL, person.getEmail()));
        emailTemplateModel.put("code", code);
        emailTemplateModel.put("firstName", person.getFirstName() != null ? person.getFirstName() : "User");
        emailTemplateModel.put(EMAIL, person.getEmail());
        emailTemplateModel.put("resetPasswordURL", resetPasswordURL);
        emailOutboxService.sendEmail(EmailEvent.builder()
                .recipientEmail(person.getEmail())
                .emailType(EmailType.RESET_PASSWORD)
                .templateModel(emailTemplateModel)
                .build());
    }

    public void checkRestrictedDomains(String email) {
        String[] restrictedDomains = commonProperties.getRestrictedDomains();
        String[] emailParts = email.split("@");
        String emailDomain = emailParts[emailParts.length - 1];
        if (restrictedDomains != null && restrictedDomains.length > 0) {
            for (String domain : restrictedDomains) {
                if (emailDomain.equals(domain)) {
                    return;
                }
            }
            throw new BadRequestException("error.restricted.email.domain");
        }
    }

    private String generateURL(String path, Map<String, String> queryParams) {
        return CommonUtil.generateURL(commonProperties.getWebUrl(), path, queryParams);
    }
}
