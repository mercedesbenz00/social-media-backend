package iq.earthlink.social.personservice.security;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.hibernate.validator.constraints.Range;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Data
@NoArgsConstructor
@ConfigurationProperties("social.authentication")
@Component
@Validated
public class AuthenticationProperties {

    @NonNull
    @Range(min = 1, max = 10000)
    private String codeExpirationInMinutes;

    @Range(min = 1, max = 10000)
    private String tokenExpirationInMinutes;

    @Range(min = 1, max = 10000)
    private String refreshTokenExpirationInMinutes;

    private String facebookURI;
    private String googleClientId;
    private String appleKeyId;
    private String appleTeamId;
    private String appleURI;

    //Time to live for blocked tokens in seconds:
    @Range(min = 1, max = 100000000)
    private String ttl;
}
