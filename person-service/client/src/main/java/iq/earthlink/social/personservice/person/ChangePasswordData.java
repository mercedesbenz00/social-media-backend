package iq.earthlink.social.personservice.person;

import javax.annotation.Nonnull;

public interface ChangePasswordData {

  @Nonnull
  String getOldPassword();

  @Nonnull
  String getPassword();

  @Nonnull
  String getConfirmPassword();

}
