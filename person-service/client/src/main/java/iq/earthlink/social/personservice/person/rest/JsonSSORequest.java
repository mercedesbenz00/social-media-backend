package iq.earthlink.social.personservice.person.rest;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class JsonSSORequest {
    @ApiModelProperty(value = "SSO access token")
    @NotBlank
    private String accessToken;

    @ApiModelProperty(value = "First Name")
    private String firstName;

    @ApiModelProperty(value = "Last Name")
    private String lastName;

    @ApiModelProperty(value = "Client Id for the app")
    private String clientId;
}
