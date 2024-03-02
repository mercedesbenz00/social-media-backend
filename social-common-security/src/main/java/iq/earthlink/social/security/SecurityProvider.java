package iq.earthlink.social.security;

import com.auth0.jwt.interfaces.DecodedJWT;

public interface SecurityProvider {

  String UNDERSCORE = "_";
  String AUTH_KEY = "auth_key";
  String ROLES = "roles";

  DecodedJWT decodedJWTToken(String token);

  /**
   * Finds whether token is blocked.
   *
   * @param token the token that needs to be checked.
   */
  Boolean isTokenBlacklisted(String token);

  boolean isValidToken(String token);

  String getSubjectFromJWT(String token);

  String[] getRolesFromJWT(String token);

  String[] getRolesFromAuthorization(String authorization);

  String getAuthKeyFromJWT(String token);

  Long getPersonIdFromAuthorization(String token);
}
