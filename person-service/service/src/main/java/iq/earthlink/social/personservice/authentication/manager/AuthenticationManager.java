package iq.earthlink.social.personservice.authentication.manager;

import iq.earthlink.social.personservice.person.ChangePasswordData;
import iq.earthlink.social.personservice.person.RegistrationData;
import iq.earthlink.social.personservice.person.model.Person;
import iq.earthlink.social.personservice.person.rest.*;

import javax.annotation.Nonnull;
import java.util.Map;

public interface AuthenticationManager {

    /**
     * Registers new person.
     *
     * @param data the registration data
     * @return newly created person
     */
    Person register(@Nonnull RegistrationData data);

    void changePassword(Person person, ChangePasswordData data);

    void forgotPassword(JsonForgotPasswordRequest request);

    void resetPassword(JsonResetPasswordRequest request);

    void confirmEmail(String email, String code);

    void resendConfirmationEmail(String email);

    Map<String, Object> refreshToken(String refreshToken);

    JsonAuthentication authenticateByEmail(JsonLoginRequest data);

    JsonAuthentication authenticateWithFacebook(JsonSSORequest data);

    JsonAuthentication authenticateWithGoogle(JsonSSORequest data);

    JsonAuthentication authenticateWithApple(JsonSSORequest data);

}
