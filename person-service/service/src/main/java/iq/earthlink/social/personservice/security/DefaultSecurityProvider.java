package iq.earthlink.social.personservice.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTCreator;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.*;
import com.auth0.jwt.interfaces.DecodedJWT;
import iq.earthlink.social.exception.InvalidTokenRequestException;
import iq.earthlink.social.personservice.config.ServerAuthProperties;
import iq.earthlink.social.personservice.person.model.Person;
import iq.earthlink.social.personservice.security.model.Authority;
import iq.earthlink.social.personservice.security.model.Role;
import iq.earthlink.social.personservice.security.repository.AuthorityRepository;
import iq.earthlink.social.personservice.security.repository.BlockedTokenRepository;
import iq.earthlink.social.personservice.security.repository.RoleRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import javax.annotation.Nonnull;
import java.time.Instant;
import java.util.*;

import static iq.earthlink.social.common.util.CommonConstants.ERROR_CHECK_NOT_NULL;
import static iq.earthlink.social.common.util.Preconditions.checkNotNull;

@Service
@Slf4j
public class DefaultSecurityProvider implements SecurityProvider {

    private final PasswordEncoder passwordEncoder;
    private final RoleRepository roleRepository;
    private final BlockedTokenRepository blockedTokenRepository;
    private final AuthorityRepository authorityRepository;

    private final AuthenticationProperties properties;
    private final ServerAuthProperties authProperties;

    public DefaultSecurityProvider(
            PasswordEncoder passwordEncoder,
            RoleRepository roleRepository,
            BlockedTokenRepository blockedTokenRepository,
            AuthorityRepository authorityRepository,
            AuthenticationProperties properties, ServerAuthProperties authProperties) {
        this.passwordEncoder = passwordEncoder;
        this.roleRepository = roleRepository;
        this.blockedTokenRepository = blockedTokenRepository;
        this.authorityRepository = authorityRepository;
        this.properties = properties;
        this.authProperties = authProperties;
    }

    @Override
    public Set<Role> getRoles(String... codes) {
        if (ArrayUtils.isEmpty(codes)) {
            return Collections.emptySet();
        }

        return roleRepository.findAllByCodeIn(Arrays.asList(codes));
    }

    @Override
    public Set<Authority> getAuthorities(String... codes) {
        if (ArrayUtils.isEmpty(codes)) {
            return Collections.emptySet();
        }

        return authorityRepository.findAllByCodeIn(Arrays.asList(codes));
    }

    @Override
    public String encode(String password) {
        if (StringUtils.isEmpty(password)) {
            throw new IllegalArgumentException("error.password.is.empty");
        }

        return passwordEncoder.encode(password);
    }

    @Override
    public boolean matchPassword(String rawPassword, String encodedPassword) {
        if (StringUtils.isAnyEmpty(rawPassword, encodedPassword)) {
            return false;
        }

        return passwordEncoder.matches(rawPassword, encodedPassword);
    }

    @Override
    public DecodedJWT decodedJWTToken(String token) {
        Algorithm algorithm = Algorithm.HMAC256(authProperties.getSecret().getBytes());
        JWTVerifier verifier = JWT.require(algorithm).build();
        return verifier.verify(token);
    }

    @Override
    public String generateToken(Person person) {
        return generateJWTToken(person.getId(), person.getUuid(), Long.parseLong(properties.getTokenExpirationInMinutes()),
                TOKEN, new ArrayList<>(person.getRoles()));
    }

    @Override
    public String generateRefreshToken(Person person) {
        return generateJWTToken(person.getId(), person.getUuid(), Long.parseLong(properties.getRefreshTokenExpirationInMinutes()), REFRESH_TOKEN, null);
    }

    @Override
    public void blockToken(@NotNull String token) {
        checkNotNull(token, ERROR_CHECK_NOT_NULL, TOKEN);

        String authKey = getAuthKeyFromJWT(token);
        String[] idKey = authKey.split(UNDERSCORE);

        BlockedToken blockedToken = BlockedToken.builder()
                .personId(Long.valueOf(idKey[0]))
                .key(idKey[1])
                .createdAt(new Date(System.currentTimeMillis()))
                .ttl(Long.parseLong(properties.getTtl()))
                .build();

        blockedTokenRepository.save(blockedToken);
    }

    @Nonnull
    @Override
    public Boolean isTokenBlacklisted(@Nonnull String token) {
        String authKey = getAuthKeyFromJWT(token);
        String[] idKey = authKey.split(UNDERSCORE);

        return blockedTokenRepository.findByPersonIdAndKey(Long.valueOf(idKey[0]), idKey[1]).isPresent();
    }

    private String generateJWTToken(Long id, UUID subject, long expirationTimeInMinutes, String type, List<String> roles) {
        Algorithm algorithm = Algorithm.HMAC256(authProperties.getSecret().getBytes());
        JWTCreator.Builder jwtBuilder = JWT.create()
                .withSubject(subject.toString())
                .withIssuedAt(Date.from(Instant.now()))
                .withExpiresAt(new Date(System.currentTimeMillis() + expirationTimeInMinutes * 60 * 1000))
                .withClaim(TYPE, type);

        if (Objects.nonNull(roles)) {
            jwtBuilder.withClaim(ROLES, roles);
        }
        jwtBuilder.withClaim(AUTH_KEY, StringUtils.joinWith(UNDERSCORE, id, java.util.UUID.randomUUID()));

        return jwtBuilder.sign(algorithm);
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
    public String getAuthKeyFromJWT(String token) {
        DecodedJWT decodedJWT = decodedJWTToken(token);

        return decodedJWT.getClaim(AUTH_KEY).asString();
    }

    @Override
    public String getTypeFromJWT(String token) {
        DecodedJWT decodedJWT = decodedJWTToken(token);

        return decodedJWT.getClaim(TYPE).asString();
    }

    @Override
    public Date getExpireTime(String token) {
        DecodedJWT decodedJWT = decodedJWTToken(token);

        return decodedJWT.getExpiresAt();
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
