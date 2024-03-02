package iq.earthlink.social.notificationservice.service.token;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Data
@ConfigurationProperties("social.notificationservice.persontoken")
@Component
@Validated
public class PersonTokenProperties {
    private int deleteAfterDays;
}
