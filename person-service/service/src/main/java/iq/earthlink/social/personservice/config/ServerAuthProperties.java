package iq.earthlink.social.personservice.config;

import iq.earthlink.social.personservice.authentication.enumeration.AuthMethod;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "server.auth")
public class ServerAuthProperties {

    public static final String SECRET_KEY = "SECRET_KEY";

    private String host;
    private String secret;
    private CaptchaConfig captchaSettings;
    private String method;
    private String apiAuthHeader;
    private int maxLoginAttempts;
    private int lockTimeDuration;

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getSecret() {
        secret = System.getProperty(SECRET_KEY);
        if (StringUtils.isEmpty(secret)) {
            secret = RandomStringUtils.randomAlphanumeric(32);
            System.setProperty(SECRET_KEY, secret);
        }
        return secret;
    }

    public void setSecret(String secret) {
        System.setProperty(SECRET_KEY, secret);
        this.secret = secret;
    }

    public CaptchaConfig getCaptchaSettings() {
        return captchaSettings;
    }

    public void setCaptchaSettings(CaptchaConfig captchaSettings) {
        this.captchaSettings = captchaSettings;
    }

    public String getMethod() {
        return StringUtils.isEmpty(method) ? AuthMethod.NA.name() : method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public int getMaxLoginAttempts() {
        return maxLoginAttempts;
    }

    public void setMaxLoginAttempts(int maxLoginAttempts) {
        this.maxLoginAttempts = maxLoginAttempts;
    }

    public int getLockTimeDuration() {
        return lockTimeDuration;
    }

    public void setLockTimeDuration(int lockTimeDuration) {
        this.lockTimeDuration = lockTimeDuration;
    }

    public String getApiAuthHeader() {
        return apiAuthHeader;
    }

    public void setApiAuthHeader(String apiAuthHeader) {
        this.apiAuthHeader = apiAuthHeader;
    }
}
