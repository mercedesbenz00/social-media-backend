package iq.earthlink.social.personservice.person;

import iq.earthlink.social.personservice.data.Gender;

import javax.annotation.Nonnull;
import java.util.Date;

public interface RegistrationData {

  String PASSWORD_REGEX = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[!@#&()–[{}]:;',?/*~$^+=<>]).{8,20}$";
  String FIRST_LAST_NAME_REGEX = "^[\\u0621-\\u064AA-Za-z][\\u0621-\\u064AA-Za-z \\-]*$";
  String DISPLAY_NAME_REGEX = "^[\\u0621-\\u064AA-Za-z][\\u0621-\\u064AA-Za-z \\-0-9_.]*$";
  String USERNAME_REGEX = "^[\\u0621-\\u064AA-Za-z\\-0-9_.]+$";

  String PASSWORD_PATTERN_ERROR = "{error.pattern.small.capital.number}";
  String PASSWORD_MATCH_ERROR = "{error.password.match}";
  String PASSWORD_COMMON_ERROR = "{error.password.common}";
  String FIRST_LAST_NAME_PATTERN_ERROR = "{error.pattern.only.letters.dash}";
  String DISPLAY_NAME_PATTERN_ERROR = "{error.pattern.only.letters.digits.dash.dot.underscore}";
  String USERNAME_PATTERN_ERROR = "{error.pattern.only.letters.dash.dot.number.underscore}";
  String FIELD_SIZE_ERROR = "{error.size.too.big}";
  String FUTURE_DATE_ERROR = "{error.future.date}";

  @Nonnull
  String getEmail();

  @Nonnull
  String getPassword();

  @Nonnull
  String getConfirmPassword();

//  @Nonnull
  String getFirstName();

//  @Nonnull
  String getLastName();

  String getDisplayName();

//  @Nonnull
  Gender getGender();

//  @Nonnull
  Date getBirthDate();

  Long getCityId();

  String getCaptchaResponse();

  String getBio();
}
