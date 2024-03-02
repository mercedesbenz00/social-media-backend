package iq.earthlink.social.personservice.authentication.manager;

import iq.earthlink.social.classes.data.dto.JsonMediaFile;
import iq.earthlink.social.classes.data.dto.JsonSizedImage;
import iq.earthlink.social.classes.enumeration.RegistrationState;
import iq.earthlink.social.common.file.SizedImage;
import iq.earthlink.social.common.util.Code;
import iq.earthlink.social.common.util.CommonConstants;
import iq.earthlink.social.exception.*;
import iq.earthlink.social.personservice.authentication.dto.SSOUserModel;
import iq.earthlink.social.personservice.authentication.enumeration.ProviderName;
import iq.earthlink.social.personservice.authentication.model.ResourceProvider;
import iq.earthlink.social.personservice.authentication.repository.ResourceProviderRepository;
import iq.earthlink.social.personservice.authentication.util.AuthUtil;
import iq.earthlink.social.personservice.config.ServerAuthProperties;
import iq.earthlink.social.personservice.person.BanManager;
import iq.earthlink.social.personservice.person.ChangePasswordData;
import iq.earthlink.social.personservice.person.RegistrationData;
import iq.earthlink.social.personservice.person.impl.repository.PersonRepository;
import iq.earthlink.social.personservice.person.model.Person;
import iq.earthlink.social.personservice.person.model.PersonBan;
import iq.earthlink.social.personservice.person.rest.*;
import iq.earthlink.social.personservice.security.SecurityProvider;
import iq.earthlink.social.personservice.service.*;
import iq.earthlink.social.personservice.util.CommonProperties;
import iq.earthlink.social.personservice.util.ProfileUtil;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Nonnull;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static iq.earthlink.social.common.util.CommonConstants.ERROR_CHECK_NOT_NULL;
import static iq.earthlink.social.common.util.Preconditions.checkNotNull;
import static iq.earthlink.social.personservice.util.Constants.USER;
import static java.util.stream.Collectors.groupingBy;
import static org.apache.commons.lang3.StringUtils.*;

@Service
@RequiredArgsConstructor
public class DefaultAuthenticationManager implements AuthenticationManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultAuthenticationManager.class);

    private final SecurityProvider securityProvider;
    private final PersonRepository personRepository;
    private final ProfileUtil profileUtil;
    private final AppleService appleService;
    private final GoogleService googleService;
    private final FacebookService facebookService;
    private final BanManager banManager;
    private final CommonProperties commonProperties;
    private final ResourceProviderRepository resourceProviderRepository;
    private final ChatAdministrationService chatAdministrationService;
    private final AuthUtil authUtil;
    private final ServerAuthProperties serverAuthProperties;
    private final Random rnd = new Random();
    private final KafkaProducerService kafkaProducerService;
    private static final String ERROR_USER_WITH_EMAIL_NOT_FOUND = "error.user.with.email.not.found";


    @Transactional
    @Override
    public Person register(@Nonnull RegistrationData data) {
        checkNotNull(data, ERROR_CHECK_NOT_NULL, "data");
        profileUtil.checkBio(data.getBio());
        authUtil.checkRestrictedDomains(data.getEmail());

        final Person person = Person.builder()
                .email(toRootLowerCase(data.getEmail()))
                .password(securityProvider.encode(data.getPassword()))
                .personRoles(securityProvider.getRoles(USER))
                .personAuthorities(Collections.emptySet())
                .confirmCode(Code.next(6))
                .uuid(UUID.randomUUID())
                .accountNonLocked(true)
                .state(RegistrationState.ACCOUNT_CREATED)
                .bio(data.getBio())
                .build();

        profileUtil.generateDisplayName(person);
        profileUtil.generateUsername(personRepository, person, 0);

        Person savedPerson;
        try {
            savedPerson = personRepository.saveAndFlush(person);
            LOGGER.info("Successfully registered new person: {}", savedPerson);
        } catch (DataIntegrityViolationException ex) {
            throw new NotUniqueException("error.email.already.registered", ex);
        } catch (Exception ex) {
            LOGGER.error("Failed to register new user", ex);
            throw new IllegalArgumentException("Failed to register new user");
        }

        authUtil.sendConfirmationEmail(savedPerson);

        return savedPerson;
    }

    @Override
    public JsonAuthentication authenticateByEmail(JsonLoginRequest data) {
        Person person = personRepository.findByEmailIgnoreCase(data.getEmail());

        if (person == null) {
            throw new NotFoundException("error.person.wrong.email.or.password");
        }

        if (person.isAccountNonLocked() || unlockWhenTimeExpired(person)) {
            // If user account is non-locked, or lock time is expired, verify password:
            if (person.getPassword() != null && securityProvider.matchPassword(data.getPassword(), person.getPassword())) {

                if (!person.isConfirmed()) {
                    throw new ForbiddenException("error.email.not.confirmed");
                }

                checkBan(person.getId());

                String token = securityProvider.generateToken(person);
                String refreshToken = securityProvider.generateRefreshToken(person);
                Date expireTime = securityProvider.getExpireTime(token);
                activatePersonAccountIfNeeded(person);
                return JsonAuthentication.builder()
                        .expireTime(expireTime)
                        .token(token)
                        .refreshToken(refreshToken)
                        .build();
            } else {
                // Incorrect password - increase failed attempts counter:
                if (person.getFailedAttempt() < serverAuthProperties.getMaxLoginAttempts()) {
                    increaseFailedAttempts(person);
                    throw new NotFoundException("error.person.wrong.email.or.password");
                } else {
                    // Lock account if maximum failed attempts is reached:
                    lock(person);
                    throw new AccountLockedException("error.user.account.locked", serverAuthProperties.getMaxLoginAttempts(),
                            serverAuthProperties.getLockTimeDuration());
                }
            }
        } else {
            throw new AccountLockedException("error.user.account.locked", serverAuthProperties.getMaxLoginAttempts(),
                    serverAuthProperties.getLockTimeDuration());
        }
    }

    @Transactional
    @Override
    public void changePassword(Person person, ChangePasswordData data) {
        Person personModel = personRepository.findById(person.getId())
                .orElseThrow(() -> new NotFoundException("error.person.not.found.byId", person.getId()));

        String currentPass = personModel.getPassword();

        if (securityProvider.matchPassword(data.getOldPassword(), currentPass)) {
            if (Objects.equals(data.getPassword(), data.getConfirmPassword())) {
                personModel.setPassword(securityProvider.encode(data.getPassword()));
                personRepository.save(personModel);
                LOGGER.info("Successfully changed password for person: {}", person.getId());
            } else {
                throw new BadRequestException("error.password.confirmPassword.should.match");
            }
        } else {
            throw new BadRequestException("error.old.password.is.wrong");
        }
    }

    @Override
    public void forgotPassword(JsonForgotPasswordRequest request) {
        Person person = personRepository.findByEmailIgnoreCase(request.getEmail());
        if (person == null) {
            throw new NotFoundException(ERROR_USER_WITH_EMAIL_NOT_FOUND);
        }

        int code = rnd.nextInt(999999);
        long codeExpirationInMinutes = commonProperties.getCodeExpirationInMinutes();
        Date expireAt = new Date(System.currentTimeMillis() + codeExpirationInMinutes * 60 * 1000);
        person.setResetCode(code);
        person.setResetCodeExpireAt(expireAt);
        personRepository.save(person);
        authUtil.sendResetPasswordEmail(person, code);
    }

    @Override
    public void resetPassword(JsonResetPasswordRequest request) {
        Person person = personRepository.findByEmailIgnoreCase(request.getEmail());
        if (person == null) {
            throw new NotFoundException(ERROR_USER_WITH_EMAIL_NOT_FOUND);
        }
        if (Objects.equals(person.getResetCode(), request.getCode()) && person.getResetCodeExpireAt().after(new Date())) {
            if (request.isPasswordMatch()) {
                person.setPassword(securityProvider.encode(request.getPassword()));
                person.setResetCodeExpireAt(null);
                person.setResetCode(null);
                personRepository.save(person);
            } else {
                throw new BadRequestException("error.password.confirmPassword.should.match");
            }
        } else {
            throw new BadRequestException("error.password.reset.session.expired");
        }
    }

    @Override
    @Transactional
    public JsonAuthentication authenticateWithFacebook(JsonSSORequest data) {
        SSOUserModel facebookUserModel = facebookService.getUserDetails(data.getAccessToken(), "email,first_name,last_name,gender,birthday");
        return authenticateSSOUser(facebookUserModel);
    }

    @Override
    @Transactional
    public JsonAuthentication authenticateWithGoogle(JsonSSORequest data) {
        SSOUserModel googleUser = googleService.getUserDetails(data.getAccessToken());
        return authenticateSSOUser(googleUser);
    }

    @Override
    @Transactional
    public JsonAuthentication authenticateWithApple(JsonSSORequest data) {
        SSOUserModel appleUser = appleService.getUserDetails(data);
        return authenticateSSOUser(appleUser);
    }

    @Transactional
    public JsonAuthentication authenticateSSOUser(@Nonnull SSOUserModel userModel) {
        checkNotNull(userModel.getId(), ERROR_CHECK_NOT_NULL, "providerId");

        Optional<ResourceProvider> provider = resourceProviderRepository.findByProviderIdAndProviderName(userModel.getId(), userModel.getProviderName());

        if (provider.isPresent()) {
            return processExistingProvider(provider.get());
        } else {
            return processNewProvider(userModel);
        }
    }

    private JsonAuthentication processExistingProvider(ResourceProvider resourceProvider) {
        Person person = resourceProvider.getPerson();
        checkBan(person.getId());

        String token = securityProvider.generateToken(person);
        String refreshToken = securityProvider.generateRefreshToken(person);
        Date expireTime = securityProvider.getExpireTime(token);
        activatePersonAccountIfNeeded(person);
        resetFailedAttempts(person);

        return JsonAuthentication.builder()
                .expireTime(expireTime)
                .token(token)
                .refreshToken(refreshToken)
                .build();
    }

    private JsonAuthentication processNewProvider(SSOUserModel userModel) {
        checkSSOUserRequiredFields(userModel);

        if (userModel.getEmail() != null) {
            authUtil.checkRestrictedDomains(userModel.getEmail());
        }

        ResourceProvider resourceProvider = ResourceProvider
                .builder()
                .providerId(userModel.getId())
                .providerName(userModel.getProviderName())
                .build();

        Person newPerson = createNewPerson(userModel);

        String token = securityProvider.generateToken(newPerson);
        String refreshToken = securityProvider.generateRefreshToken(newPerson);
        Date expireTime = securityProvider.getExpireTime(token);

        resourceProvider.setPerson(newPerson);
        try {
            resourceProviderRepository.save(resourceProvider);
        } catch (Exception ex) {
            LOGGER.error("authenticateSSOUser: Failed to add new authentication resource provider", ex);
            throw new IllegalArgumentException("Failed to add new authentication resource provider");
        }

        return JsonAuthentication.builder()
                .expireTime(expireTime)
                .token(token)
                .refreshToken(refreshToken)
                .build();
    }

    private Person createNewPerson(SSOUserModel userModel) {
        if (Objects.isNull(userModel.getEmail())) {
            return createPersonWithoutEmail(userModel);
        } else {
            Person existingPerson = personRepository.findByEmailIgnoreCase(userModel.getEmail());
            if (Objects.isNull(existingPerson)) {
                return createPersonWithEmail(userModel);
            } else {
                checkBan(existingPerson.getId());
                return existingPerson;
            }
        }
    }

    private Person createPersonWithoutEmail(SSOUserModel userModel) {
        Person newPerson = Person.builder()
                .personRoles(securityProvider.getRoles(USER))
                .personAuthorities(Collections.emptySet())
                .firstName(normalizeSpace(userModel.getFirstName()))
                .lastName(normalizeSpace(userModel.getLastName()))
                .gender(userModel.getGender())
                .birthDate(userModel.getBirthday())
                .isRegistrationCompleted(true)
                .state(RegistrationState.INFO_PROVIDED)
                .isConfirmed(true)
                .uuid(UUID.randomUUID())
                .build();

        profileUtil.generateDisplayName(newPerson);
        profileUtil.generateUsername(personRepository, newPerson, 0);

        try {
            personRepository.save(newPerson);
            kafkaProducerService.sendMessageOnFailureThrowError(CommonConstants.PERSON_CREATED, getJsonPersonProfileFromEntity(newPerson));
        } catch (Exception ex) {
            LOGGER.error("authenticateSSOUser: Failed to create new person", ex);
            throw new IllegalArgumentException("Failed to create new person");
        }

        return newPerson;
    }

    private Person createPersonWithEmail(SSOUserModel userModel) {
        Person newPerson = Person.builder()
                .email(userModel.getEmail())
                .personRoles(securityProvider.getRoles(USER))
                .personAuthorities(Collections.emptySet())
                .firstName(normalizeSpace(userModel.getFirstName()))
                .lastName(normalizeSpace(userModel.getLastName()))
                .gender(userModel.getGender())
                .birthDate(userModel.getBirthday())
                .isRegistrationCompleted(true)
                .state(RegistrationState.INFO_PROVIDED)
                .isConfirmed(true)
                .uuid(UUID.randomUUID())
                .build();

        profileUtil.generateDisplayName(newPerson);
        profileUtil.generateUsername(personRepository, newPerson, 0);

        try {
            personRepository.save(newPerson);
            kafkaProducerService.sendMessageOnFailureThrowError(CommonConstants.PERSON_CREATED, getJsonPersonProfileFromEntity(newPerson));
        } catch (Exception ex) {
            LOGGER.error("authenticateSSOUser: Failed to create new person", ex);
            throw new IllegalArgumentException("Failed to create new person");
        }

        return newPerson;
    }


    @Override
    public void confirmEmail(String email, String code) {
        checkNotNull(code, ERROR_CHECK_NOT_NULL, "code");

        if (isBlank(email)) {
            throw new BadRequestException("error.email.not.blank");
        }

        Person person = personRepository.findByEmailIgnoreCase(email);
        if (person == null) {
            throw new NotFoundException(ERROR_USER_WITH_EMAIL_NOT_FOUND);
        }

        if (Objects.equals(person.getConfirmCode(), code)) {
            person.setConfirmed(true);
            person.setState(RegistrationState.EMAIL_CONFIRMED);
            person.setConfirmCode(null);
            personRepository.saveAndFlush(person);
        } else
            throw new BadRequestException("error.email.or.code.wrong");
    }

    @Override
    public void resendConfirmationEmail(String email) {
        Person person = personRepository.findByEmailIgnoreCase(email);
        if (person != null && !person.isConfirmed()) {
            try {
                person.setConfirmCode(Code.next());
                personRepository.save(person);
                authUtil.sendConfirmationEmail(person);
            } catch (Exception exception) {
                throw new RestApiException(HttpStatus.INTERNAL_SERVER_ERROR, "error.confirmation.email.resent.issue", exception.getMessage());
            }
        } else {
            throw new NotFoundException(ERROR_USER_WITH_EMAIL_NOT_FOUND);
        }
    }

    @Override
    public Map<String, Object> refreshToken(String refreshToken) {

        if (securityProvider.isValidToken(refreshToken)
                && securityProvider.getTypeFromJWT(refreshToken).equals("refreshToken")) {

            UUID uuid = UUID.fromString(securityProvider.getSubjectFromJWT(refreshToken));
            Person person = personRepository.findByUuid(uuid)
                    .orElseThrow(() -> new NotFoundException(Person.class, uuid));
            // Block old token:
            securityProvider.blockToken(refreshToken);

            // Generate new tokens:
            String newToken = securityProvider.generateToken(person);
            String newRefreshToken = securityProvider.generateRefreshToken(person);
            Date expireTime = securityProvider.getExpireTime(newToken);

            Map<String, Object> tokens = new HashMap<>();
            tokens.put(SecurityProvider.TOKEN, newToken);
            tokens.put(SecurityProvider.REFRESH_TOKEN, newRefreshToken);
            tokens.put(SecurityProvider.EXPIRE_TIME, expireTime);

            return tokens;
        } else {
            throw new InvalidTokenRequestException("error.refresh.token.invalid", refreshToken);
        }
    }

    private void increaseFailedAttempts(@Nonnull Person person) {
        int newFailAttempts = person.getFailedAttempt() + 1;
        person.setFailedAttempt(newFailAttempts);
        personRepository.save(person);
    }

    private void lock(@Nonnull Person person) {
        if (person.isAccountNonLocked()) {
            person.setAccountNonLocked(false);
            person.setLockTime(new Date());

            personRepository.saveAndFlush(person);
        }
    }

    private boolean unlockWhenTimeExpired(@Nonnull Person person) {
        if (!person.isAccountNonLocked() && person.getLockTime() != null) {
            long lockTimeInMillis = person.getLockTime().getTime();
            long currentTimeInMillis = System.currentTimeMillis();
            long lockTimeDurationInMillis = (long) serverAuthProperties.getLockTimeDuration() * 60 * 60 * 1000;

            if (lockTimeInMillis + lockTimeDurationInMillis < currentTimeInMillis) {
                resetFailedAttempts(person);
                return true;
            }

            return false;
        }
        return true;
    }

    private void resetFailedAttempts(@Nonnull Person person) {
        if (!person.isAccountNonLocked()) {
            person.setAccountNonLocked(true);
            person.setLockTime(null);
            person.setFailedAttempt(0);

            personRepository.saveAndFlush(person);
        }
    }

    private static void checkSSOUserRequiredFields(SSOUserModel ssoUser) {
        if (ssoUser == null ||
                (!ProviderName.FACEBOOK.equals(ssoUser.getProviderName()) && ssoUser.getEmail() == null) ||
                ssoUser.getFirstName() == null ||
                ssoUser.getLastName() == null) {
            throw new ForbiddenException("error.sso.required.fields.missing");
        }
    }

    private void checkBan(Long personId) {
        List<PersonBan> personBans = banManager.getActiveBans(personId);

        if (CollectionUtils.isNotEmpty(personBans)) {
            PersonBan latestBan = personBans.stream().sorted(Comparator.comparing(PersonBan::getExpiredAt).reversed())
                    .toList().get(0);

            LocalDateTime now = LocalDateTime.now().with(LocalTime.MIN);
            LocalDateTime expiredDate = LocalDateTime.ofInstant(latestBan.getExpiredAt().toInstant(), ZoneId.systemDefault());
            long days = ChronoUnit.DAYS.between(now, expiredDate);

            if (days > 0) {
                throw new ForbiddenException("error.person.is.banned.reason", days, latestBan.getReason());
            }
        }
    }

    private void activatePersonAccountIfNeeded(Person person) {
        if (Objects.nonNull(person.getDeletedDate())) {
            person.setDeletedDate(null);
            personRepository.save(person);
            chatAdministrationService.reactivateUser(person.getId());
        }
    }

    private JsonPersonProfile getJsonPersonProfileFromEntity(Person person) {
        var jsonPersonProfile = JsonPersonProfile
                .builder()
                .id(person.getId())
                .uuid(person.getUuid())
                .createdAt(person.getCreatedAt())
                .displayName(person.getDisplayName())
                .isVerifiedAccount(person.isVerifiedAccount())
                .roles(person.getRoles())
                .build();

        var avatar = person.getAvatar();
        if (avatar != null) {
            Set<SizedImage> sizedImageSet = avatar.getSizedImages();

            Map<String, List<JsonSizedImage>> sizedImages = sizedImageSet.stream()
                    .map(sizedImage -> JsonSizedImage
                            .builder()
                            .imageSizeType(sizedImage.getImageSizeType())
                            .size(sizedImage.getSize())
                            .path(sizedImage.getPath())
                            .createdAt(sizedImage.getCreatedAt())
                            .mimeType(sizedImage.getMimeType())
                            .build()
                    ).collect(groupingBy(image -> image.getMimeType().replace("image/", "")));

            jsonPersonProfile.setAvatar(JsonMediaFile.builder()
                    .id(avatar.getId())
                    .fileType(avatar.getFileType())
                    .mimeType(avatar.getMimeType())
                    .ownerId(avatar.getOwnerId())
                    .size(avatar.getSize())
                    .sizedImages(sizedImages)
                    .path(avatar.getPath())
                    .createdAt(avatar.getCreatedAt())
                    .build());
        }
        return jsonPersonProfile;
    }
}
