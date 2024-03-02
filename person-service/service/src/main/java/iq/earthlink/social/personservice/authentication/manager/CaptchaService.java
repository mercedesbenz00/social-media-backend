package iq.earthlink.social.personservice.authentication.manager;

import iq.earthlink.social.exception.BadRequestException;
import iq.earthlink.social.exception.ForbiddenException;
import iq.earthlink.social.personservice.authentication.dto.CaptchaResponse;
import iq.earthlink.social.personservice.config.CaptchaConfig;
import iq.earthlink.social.personservice.config.CaptchaCredential;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestOperations;

import java.net.URI;
import java.util.Arrays;
import java.util.Objects;
import java.util.regex.Pattern;

@Service
@Slf4j
public class CaptchaService implements ICaptchaService {
    private static final Logger LOGGER = LoggerFactory.getLogger(CaptchaService.class);
    private static final Pattern RESPONSE_CAPTCHA_PATTERN = Pattern.compile("[A-Za-z0-9_-]+");


    private final CaptchaConfig captchaConfig;
    private final RestOperations restTemplate;

    public CaptchaService(CaptchaConfig captchaConfig, RestOperations restTemplate) {
        this.captchaConfig = captchaConfig;
        this.restTemplate = restTemplate;
    }

    @Override
    public void processCaptchaResponse(String response, String siteKey) {
        // Response sanity check:
        if (!(StringUtils.hasLength(response) && RESPONSE_CAPTCHA_PATTERN.matcher(response).matches())) {
            throw new ForbiddenException("Provided wrong or empty captcha token");
        }

        if (!StringUtils.hasLength(siteKey)) {
            throw new ForbiddenException("Provided empty site key");
        }

        if (!validateCaptcha(response, siteKey)) {
            LOGGER.info("Throwing forbidden exception as the captcha is invalid.");
            throw new ForbiddenException("Provided wrong captcha token");
        }
    }

    private boolean validateCaptcha(String response, String siteKey) {
        LOGGER.info("Going to validate the captcha response = {}", response);
        try {

            CaptchaCredential settings = Arrays.stream(captchaConfig.getCredentials())
                    .filter(credentials -> credentials.getSite().equals(siteKey))
                    .findFirst()
                    .orElseThrow(() -> new BadRequestException("Provided wrong site key"));

            URI verifyUri = URI.create(captchaConfig.getVerifyUrl() + "?secret=" + settings.getSecret() + "&response=" + response);

            CaptchaResponse apiResponse = restTemplate.getForObject(verifyUri, CaptchaResponse.class);

            if (Objects.nonNull(apiResponse) && apiResponse.isSuccess()) {
                LOGGER.info("Captcha API response = {}", apiResponse);
                return true;
            } else {
                assert apiResponse != null;
                return false;
            }
        } catch (final RestClientException e) {
            LOGGER.error("Some exception occurred while binding to the recaptcha endpoint.", e);
            return false;
        }
    }
}
