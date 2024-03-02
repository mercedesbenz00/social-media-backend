package iq.earthlink.social.personservice.person;

import iq.earthlink.social.classes.enumeration.RegistrationState;
import iq.earthlink.social.personservice.data.Gender;

import java.util.Date;

public interface PersonData {

  String getUsername();

  String getFirstName();

  String getLastName();

  Gender getGender();

  Date getBirthDate();

  Long getCityId();

  String getDisplayName();

  Boolean getIsVerifiedAccount();

  RegistrationState getState();

  String getBio();

}
