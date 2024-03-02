package iq.earthlink.social.personservice.authentication.manager;

import iq.earthlink.social.exception.ForbiddenException;
import iq.earthlink.social.personservice.authentication.dto.CaptchaResponse;
import iq.earthlink.social.personservice.config.CaptchaConfig;
import iq.earthlink.social.personservice.config.CaptchaCredential;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;


class CaptchaServiceTest {
    @InjectMocks
    private CaptchaService captchaService;

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private CaptchaConfig captchaConfig;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    void whenReceiveInvalidCaptchaResponse_throwException() {
        //given
        CaptchaResponse response = new CaptchaResponse();
        response.setSuccess(false);
        given(captchaConfig.getCredentials()).willReturn(new CaptchaCredential[]{new CaptchaCredential("key", "sdf")});
        given(captchaConfig.getVerifyUrl()).willReturn("https://verify-url.com");
        given(restTemplate.getForObject(any(), any())).willReturn(response);

        //when
        //then
        assertThatThrownBy(() -> captchaService.processCaptchaResponse(null, ""))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("Provided wrong or empty captcha token");

        assertThatThrownBy(() -> captchaService.processCaptchaResponse("#$%^&*", ""))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("Provided wrong or empty captcha token");

        assertThatThrownBy(() -> captchaService.processCaptchaResponse("captchaResponse123", "key"))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("Provided wrong captcha token");
    }
}
