package iq.earthlink.social.personservice.util;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@Getter
@Setter
@ConfigurationProperties("social.default")
public class CommonProperties {

    private String webUrl;
    private String defaultDisplayName;
    private int minAge;
    private int deleteAfterDays;
    private int usernameGeneratorMaxAttempts;
    private int changeEmailRequestExpirationInHours;
    private int bioMaxLength;

    @Value("${social.authentication.codeExpirationInMinutes}")
    private long codeExpirationInMinutes;
    @Value("${social.authentication.restricted.domains}")
    private String[] restrictedDomains;

}
