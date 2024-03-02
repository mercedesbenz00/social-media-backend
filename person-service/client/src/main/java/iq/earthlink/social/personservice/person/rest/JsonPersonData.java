package iq.earthlink.social.personservice.person.rest;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import iq.earthlink.social.classes.enumeration.RegistrationState;
import iq.earthlink.social.personservice.data.Gender;
import iq.earthlink.social.personservice.person.PersonData;
import iq.earthlink.social.personservice.person.RegistrationData;
import lombok.ToString;

import javax.validation.constraints.Past;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.util.Date;

/**
 * The json dto representation of {@link PersonData}.
 */
@ApiModel("The basic person data")
@ToString
public class JsonPersonData implements PersonData {

  private static final String USERNAME_JSON = "username";
  private static final String FIRST_NAME_JSON = "firstName";
  private static final String LAST_NAME_JSON = "lastName";
  private static final String GENDER_JSON = "gender";
  private static final String BIRTH_DATE_JSON = "birthDate";
  private static final String CITY_ID_JSON = "cityId";
  private static final String DISPLAY_NAME_JSON = "displayName";
  private static final String IS_VERIFIED_ACCOUNT = "isVerifiedAccount";
  private static final String REGISTRATION_STATE = "state";

  @ApiModelProperty(value = "The person username")
  @Pattern(regexp = RegistrationData.USERNAME_REGEX, message = RegistrationData.USERNAME_PATTERN_ERROR)
  @Size(min = 3, max = 25)
  private final String username;

  @ApiModelProperty(value = "The person first name")
  @Pattern(regexp = RegistrationData.FIRST_LAST_NAME_REGEX, message = RegistrationData.FIRST_LAST_NAME_PATTERN_ERROR)
  @Size(max = 25, message = RegistrationData.FIELD_SIZE_ERROR)
  private final String firstName;

  @ApiModelProperty(value = "The person last name")
  @Pattern(regexp = RegistrationData.FIRST_LAST_NAME_REGEX, message = RegistrationData.FIRST_LAST_NAME_PATTERN_ERROR)
  @Size(max = 25, message = RegistrationData.FIELD_SIZE_ERROR)
  private final String lastName;

  private final Gender gender;

  @ApiModelProperty(value = "The person birthdate in yyyy-MM-dd format", example = "1998-01-01")
  @Past
  private final Date birthDate;

  private final Long cityId;

  @ApiModelProperty(value = "The person bio (optional)")
  private String bio;

  @ApiModelProperty(value = "The person display name (optional)")
  @Size(max = 51, message = RegistrationData.FIELD_SIZE_ERROR)
  @Pattern(regexp = RegistrationData.DISPLAY_NAME_REGEX, message = RegistrationData.DISPLAY_NAME_PATTERN_ERROR)
  private final String displayName;

  @ApiModelProperty(value = "The property which defines of the person verification status")
  private final Boolean isVerifiedAccount;

  @ApiModelProperty(value = "The property which defines of the person verification status")
  private final RegistrationState state;

  @JsonCreator
  public JsonPersonData(
      @JsonProperty(USERNAME_JSON) String username,
      @JsonProperty(FIRST_NAME_JSON) String firstName,
      @JsonProperty(LAST_NAME_JSON) String lastName,
      @JsonProperty(GENDER_JSON) Gender gender,
      @JsonFormat(shape = Shape.STRING, pattern = "yyyy-MM-dd")
      @JsonProperty(BIRTH_DATE_JSON) Date birthDate,
      @JsonProperty(CITY_ID_JSON) Long cityId,
      @JsonProperty(DISPLAY_NAME_JSON) String displayName,
      @JsonProperty(IS_VERIFIED_ACCOUNT) Boolean isVerifiedAccount,
      @JsonProperty(REGISTRATION_STATE) RegistrationState state) {

    this.username = username;
    this.firstName = firstName;
    this.lastName = lastName;
    this.gender = gender;
    this.birthDate = birthDate;
    this.cityId = cityId;
    this.displayName = displayName;
    this.isVerifiedAccount = isVerifiedAccount;
    this.state = state;
  }

  @Override
  public String getUsername() {
    return username;
  }

  @Override
  public String getFirstName() {
    return firstName;
  }

  @Override
  public String getLastName() {
    return lastName;
  }

  @Override
  public Gender getGender() {
    return gender;
  }

  @Override
  public Date getBirthDate() {
    return birthDate;
  }

  @Override
  public Long getCityId() {
    return cityId;
  }

  @Override
  public String getDisplayName() {
    return displayName;
  }

  @Override
  public Boolean getIsVerifiedAccount() {
    return isVerifiedAccount;
  }

  @Override
  public RegistrationState getState() {
    return state;
  }

  @Override
  public String getBio() {
    return bio;
  }
}
