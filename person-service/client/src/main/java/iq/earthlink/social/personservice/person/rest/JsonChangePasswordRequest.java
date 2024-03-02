package iq.earthlink.social.personservice.person.rest;

import io.swagger.annotations.ApiModelProperty;
import iq.earthlink.social.common.util.PasswordUtil;
import iq.earthlink.social.personservice.person.ChangePasswordData;
import iq.earthlink.social.personservice.person.RegistrationData;
import lombok.Data;

import javax.validation.constraints.*;
import java.util.Objects;

@Data
public class JsonChangePasswordRequest implements ChangePasswordData {

  @ApiModelProperty("The current user password")
  @NotBlank
  private String oldPassword;

  @ApiModelProperty("The new password")
  @NotBlank
  @Size(min = 8, max = 20)
  @Pattern(regexp = RegistrationData.PASSWORD_REGEX, message = RegistrationData.PASSWORD_PATTERN_ERROR)
  private String password;

  @ApiModelProperty("The repeated new password")
  @NotBlank
  @Size(min = 8, max = 20)
  private String confirmPassword;

  @AssertTrue(message = RegistrationData.PASSWORD_MATCH_ERROR)
  private boolean isPasswordMatch() {
    return Objects.equals(password, confirmPassword);
  }

  @AssertFalse(message = RegistrationData.PASSWORD_COMMON_ERROR)
  private boolean isPasswordCommon() {
    return PasswordUtil.isCommonWord(password);
  }
}
