package iq.earthlink.social.personservice.security;

import com.auth0.jwt.interfaces.DecodedJWT;
import iq.earthlink.social.personservice.person.model.Person;
import iq.earthlink.social.personservice.security.model.Authority;
import iq.earthlink.social.personservice.security.model.Role;

import javax.annotation.Nonnull;
import java.util.Date;
import java.util.Set;

public interface SecurityProvider {

  String UNDERSCORE = "_";
  String AUTH_KEY = "auth_key";
  String ROLES = "roles";
  String TYPE = "type";
  String TOKEN = "token";
  String REFRESH_TOKEN = "refreshToken";
  String EXPIRE_TIME = "expireTime";

  /**
   * Returns roles by provided codes.
   *
   * @param codes the role codes
   */
  Set<Role> getRoles(String... codes);

  /**
   * Returns authorities by provided codes.
   *
   * @param codes the authority codes
   */
  Set<Authority> getAuthorities(String... codes);

  /**
   * Returns encoded password.
   * @param password the raw password
   */
  String encode(String password);

  /**
   * Compares password on equality, returns true if passwords match.
   * @param rawPassword the raw password (not encoded)
   * @param encodedPassword the encoded password
   */
  boolean matchPassword(String rawPassword, String encodedPassword);


  DecodedJWT decodedJWTToken(String token);

  /**
   * Generate JWT token
   * @param person entity of the Person
   */
  String generateToken(Person person);


  /**
   * Generate JWT refresh token
   * @param person entity of the Person
   */
  String generateRefreshToken(Person person);

  /**
   * Creates new JWT block.
   *
   * @param token the token that needs to be blocked.
   */
  void blockToken(@Nonnull String token);


  /**
   * Finds whether token is blocked.
   *
   * @param token the token that needs to be checked.
   */
  Boolean isTokenBlacklisted(@Nonnull String token);

  boolean isValidToken(String token);

  String getSubjectFromJWT(String token);

  String[] getRolesFromJWT(String token);

  String getAuthKeyFromJWT(String token);

  String getTypeFromJWT(String token);

  Date getExpireTime(String token);

  Long getPersonIdFromAuthorization(String authorization);
}
