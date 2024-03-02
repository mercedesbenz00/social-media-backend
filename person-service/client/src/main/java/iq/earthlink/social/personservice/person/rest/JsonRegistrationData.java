package iq.earthlink.social.personservice.person.rest;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.google.common.base.Objects;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import iq.earthlink.social.common.util.PasswordUtil;
import iq.earthlink.social.personservice.data.Gender;
import iq.earthlink.social.personservice.person.RegistrationData;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.*;
import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel("Person registration data")
public class JsonRegistrationData implements RegistrationData {

    @ApiModelProperty(value = "The person email", example = "a@test.com")
    @Email
    @NotBlank
    private String email;

    @ApiModelProperty(value = "The person password", example = "Q1w2e3r4")
    @Size(min = 8, max = 20)
    @NotBlank
    @Pattern(regexp = PASSWORD_REGEX, message = PASSWORD_PATTERN_ERROR)
    private String password;

    @ApiModelProperty(value = "The person repeated password", example = "Q1w2e3r4")
    @Size(min = 8, max = 20)
    @NotBlank
    private String confirmPassword;

    @ApiModelProperty(value = "The person first name")
//    @NotBlank
    @Pattern(regexp = FIRST_LAST_NAME_REGEX, message = FIRST_LAST_NAME_PATTERN_ERROR)
    @Size(max = 25, message = FIELD_SIZE_ERROR)
    private String firstName;

    @ApiModelProperty(value = "The person last name")
//    @NotBlank
    @Pattern(regexp = FIRST_LAST_NAME_REGEX, message = FIRST_LAST_NAME_PATTERN_ERROR)
    @Size(max = 25, message = FIELD_SIZE_ERROR)
    private String lastName;

    @ApiModelProperty(value = "The person display name (optional)")
    @Size(max = 51, message = FIELD_SIZE_ERROR)
    @Pattern(regexp = DISPLAY_NAME_REGEX, message = DISPLAY_NAME_PATTERN_ERROR)
    private String displayName;

    @ApiModelProperty(value = "The person bio (optional)")
    private String bio;

    @ApiModelProperty(value = "The person gender")
//    @NotNull
    private Gender gender;

    @ApiModelProperty(value = "The person birthdate in yyyy-MM-dd format", example = "1998-01-01")
//    @NotNull
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    @Past(message = FUTURE_DATE_ERROR)
    private Date birthDate;

    @ApiModelProperty(value = "The person city (optional)")
    private Long cityId;

    @AssertTrue(message = PASSWORD_MATCH_ERROR)
    private boolean isPasswordMatch() {
        return Objects.equal(password, confirmPassword);
    }

    @AssertFalse(message = PASSWORD_COMMON_ERROR)
    private boolean isPasswordCommon() {
        return PasswordUtil.isCommonWord(password);
    }

    @ApiModelProperty(value = "Captcha response")
    private String captchaResponse;

    @ApiModelProperty(value = "Captcha site key")
    private String siteKey;
}
