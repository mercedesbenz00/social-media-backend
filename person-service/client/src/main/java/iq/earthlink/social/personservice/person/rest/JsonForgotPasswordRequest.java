package iq.earthlink.social.personservice.person.rest;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;

@Data
public class JsonForgotPasswordRequest {

    @ApiModelProperty(value = "The person email", example = "a@test.com")
    @Email
    @NotBlank
    private String email;

    @ApiModelProperty(value = "Captcha response")
    private String captchaResponse;

    @ApiModelProperty(value = "Captcha site key")
    private String siteKey;
}
