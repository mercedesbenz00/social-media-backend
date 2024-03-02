package iq.earthlink.social.security.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "server.auth")
public class ServerAuthProperties {

    private String secret;
    private String apiAuthHeader;
    private String apiSecretKey;

    public String getSecret() {
        return secret;
    }
    public void setSecret(String secret) {
        this.secret = secret;
    }

    public String getApiAuthHeader() {
        return apiAuthHeader;
    }

    public void setApiAuthHeader(String apiAuthHeader) {
        this.apiAuthHeader = apiAuthHeader;
    }

    public String getApiSecretKey() {
        return apiSecretKey;
    }

    public void setApiSecretKey(String apiSecretKey) {
        this.apiSecretKey = apiSecretKey;
    }
}
