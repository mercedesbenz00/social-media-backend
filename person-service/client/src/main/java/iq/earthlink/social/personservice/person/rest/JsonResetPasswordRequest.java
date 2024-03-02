package iq.earthlink.social.personservice.person.rest;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.Objects;

@Data
public class JsonResetPasswordRequest {
    @ApiModelProperty("The current user's email")
    @NotBlank
    private String email;

    @ApiModelProperty("The new password")
    @NotBlank
    @Length(min = 8, max = 20)
    private String password;

    @ApiModelProperty("The repeated new password")
    @NotBlank
    @Length(min = 8, max = 20)
    private String confirmationPassword;

    @ApiModelProperty("The reset code")
    @NotNull
    private Integer code;

    @AssertTrue(message = "password should match confirmPassword")
    public boolean isPasswordMatch() {
        return Objects.equals(password, confirmationPassword);
    }
}
