package iq.earthlink.social.personservice.config;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "server.auth.captcha.google")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CaptchaCredential {
    private String site;
    private String secret;
}
