package iq.earthlink.social.personservice.authentication.manager;

public interface ICaptchaService {
    void processCaptchaResponse(String response, String siteKey);
}
