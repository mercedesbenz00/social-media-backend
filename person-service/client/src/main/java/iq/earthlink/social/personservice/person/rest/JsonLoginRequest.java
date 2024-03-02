package iq.earthlink.social.personservice.person.rest;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;

@Data
public class JsonLoginRequest {
    @ApiModelProperty(value = "The person email", example = "a@test.com")
    @Email
    @NotBlank
    private String email;

    @ApiModelProperty(value = "The person password", example = "Q1w2e3r4")
    @Length(min = 8, max = 20)
    @NotBlank
    private String password;
}
