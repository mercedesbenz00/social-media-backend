package iq.earthlink.social.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.*;
import com.auth0.jwt.interfaces.DecodedJWT;
import iq.earthlink.social.exception.InvalidTokenRequestException;
import iq.earthlink.social.security.config.ServerAuthProperties;
import iq.earthlink.social.security.repository.BlockedTokenRepository;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
@Slf4j
public class DefaultSecurityProvider implements SecurityProvider {
    private final BlockedTokenRepository blockedTokenRepository;
    private final ServerAuthProperties authProperties;

    public DefaultSecurityProvider(
            BlockedTokenRepository blockedTokenRepository,
            ServerAuthProperties authProperties) {
        this.blockedTokenRepository = blockedTokenRepository;
        this.authProperties = authProperties;
    }

    @Override
    public DecodedJWT decodedJWTToken(String token) {
        Algorithm algorithm = Algorithm.HMAC256(authProperties.getSecret().getBytes());
        JWTVerifier verifier = JWT.require(algorithm).build();
        return verifier.verify(token);
    }

    @Override
    public Boolean isTokenBlacklisted(String token) {
        String authKey = getAuthKeyFromJWT(token);
        String[] idKey = authKey.split(UNDERSCORE);

        return blockedTokenRepository.findByPersonIdAndKey(Long.valueOf(idKey[0]), idKey[1]).isPresent();
    }

    @Override
    public boolean isValidToken(String token) {
        try {
            // Check if token is valid and not blacklisted:
            return !isTokenBlacklisted(token);
        } catch (AlgorithmMismatchException ex) {
            log.error("Verifier uses different algorithm for the decryption");
            throw new InvalidTokenRequestException("error.token.invalid.algorithm", token);
        } catch (SignatureVerificationException ex) {
            log.error("Signature is invalid for jwt token");
            throw new InvalidTokenRequestException("error.token.invalid.signature", token);
        } catch (TokenExpiredException ex) {
            log.error("Expired JWT token");
            throw new InvalidTokenRequestException("error.token.expired", token);
        } catch (InvalidClaimException ex) {
            log.error("JWT claims is incorrect");
            throw new InvalidTokenRequestException("error.token.illegal.argument", token);
        } catch (JWTDecodeException ex) {
            log.error("Invalid JWT Token");
            throw new InvalidTokenRequestException("error.token.invalid", token);
        }
    }

    @Override
    public String getSubjectFromJWT(String token) {
        DecodedJWT decodedJWT = decodedJWTToken(token);

        return decodedJWT.getSubject();
    }

    @Override
    public String[] getRolesFromJWT(String token) {
        DecodedJWT decodedJWT = decodedJWTToken(token);

        return decodedJWT.getClaim(ROLES).asArray(String.class);
    }

    @Override
    public String[] getRolesFromAuthorization(String authorization) {
        String token = getToken(authorization);

        return getRolesFromJWT(token);
    }

    @Override
    public String getAuthKeyFromJWT(String token) {
        DecodedJWT decodedJWT = decodedJWTToken(token);

        return decodedJWT.getClaim(AUTH_KEY).asString();
    }

    @Override
    public Long getPersonIdFromAuthorization(String authorization) {
        String token = getToken(authorization);
        DecodedJWT decodedJWT = decodedJWTToken(token);
        String authKey = decodedJWT.getClaim(AUTH_KEY).asString();
        String personId = authKey.split(UNDERSCORE)[0];
        return Long.valueOf(personId);
    }

    @NotNull
    private String getToken(String authorization) {
        if (Objects.isNull(authorization) || !authorization.startsWith("Bearer ")) {
            throw new IllegalArgumentException();
        }
        return authorization.substring(7);
    }

}
