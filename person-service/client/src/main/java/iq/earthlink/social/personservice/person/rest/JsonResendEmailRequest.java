package iq.earthlink.social.personservice.person.rest;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class JsonResendEmailRequest {
    @ApiModelProperty("User email")
    @NotBlank
    private String email;

    @ApiModelProperty(value = "Captcha response")
    private String captchaResponse;

    @ApiModelProperty(value = "Captcha site key")
    private String siteKey;
}
