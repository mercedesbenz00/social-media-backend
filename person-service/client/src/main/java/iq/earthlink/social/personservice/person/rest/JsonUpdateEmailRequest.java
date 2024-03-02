package iq.earthlink.social.personservice.person.rest;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class JsonUpdateEmailRequest {
    @ApiModelProperty("User old email")
    private String oldEmail;

    @ApiModelProperty("User new email")
    @NotBlank
    private String newEmail;

    @ApiModelProperty(value = "token")
    private String token;

    @ApiModelProperty(value = "Captcha response")
    private String captchaResponse;

    @ApiModelProperty(value = "Captcha site key")
    private String siteKey;

}
