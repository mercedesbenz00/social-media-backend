package iq.earthlink.social.personservice.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "server.auth.captcha")
@Data
public class CaptchaConfig {
    private CaptchaCredential[] credentials;
    private int maxAttempt;
    private int attemptCacheExpireAfter;
    private String verifyUrl;
}
