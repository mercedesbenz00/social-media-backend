package iq.earthlink.social.personservice.data.dto;

import iq.earthlink.social.personservice.data.Gender;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.Email;
import javax.validation.constraints.Past;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.util.Date;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PersonNewRequest {
    public static final int DISPLAY_NAME_MAX_LENGTH = 255;
    public static final int USER_NAME_MAX_LENGTH = 25;

    private String id;

    @Pattern(regexp = "^[\\u0621-\\u064AA-Za-z][\\u0621-\\u064AA-Za-z -]+$", message = "error.pattern.only.english.or.arabic")
    @Size(max = 255)
    private String firstName;

    @Pattern(regexp = "^[\\u0621-\\u064AA-Za-z][\\u0621-\\u064AA-Za-z -]+$", message = "error.pattern.only.english.or.arabic")
    @Size(max = 255)
    private String lastName;

    @Pattern(regexp = "^[\\u0621-\\u064AA-Za-z]+$", message = "error.pattern.only.english.or.arabic")
    @Size(max = USER_NAME_MAX_LENGTH)
    private String username;

    @Size(max = DISPLAY_NAME_MAX_LENGTH)
    @Pattern(regexp = "^[\\u0621-\\u064AA-Za-z][\\u0621-\\u064AA-Za-z \\-0-9]+$", message = "error.pattern.only.symbols.digits.dash")
    private String displayName;

    @Past
    private Date birthDate;

    @Email
    private String email;
    private String phoneNumber;
    private Long countryId;
    private Long cityId;
    private String language;
    private Gender gender;
}
