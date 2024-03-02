package iq.earthlink.social.personservice.authentication.manager;

import iq.earthlink.social.classes.enumeration.RegistrationState;
import iq.earthlink.social.common.filestorage.CompositeFileStorageProvider;
import iq.earthlink.social.common.filestorage.minio.MinioFileStorage;
import iq.earthlink.social.exception.*;
import iq.earthlink.social.personservice.authentication.dto.SSOUserModel;
import iq.earthlink.social.personservice.authentication.enumeration.ProviderName;
import iq.earthlink.social.personservice.authentication.model.ResourceProvider;
import iq.earthlink.social.personservice.authentication.repository.ResourceProviderRepository;
import iq.earthlink.social.personservice.authentication.util.AuthUtil;
import iq.earthlink.social.personservice.config.ServerAuthProperties;
import iq.earthlink.social.personservice.data.Gender;
import iq.earthlink.social.personservice.person.BanManager;
import iq.earthlink.social.personservice.person.impl.repository.PersonRepository;
import iq.earthlink.social.personservice.person.model.Person;
import iq.earthlink.social.personservice.person.model.PersonBan;
import iq.earthlink.social.personservice.person.rest.*;
import iq.earthlink.social.personservice.security.DefaultSecurityProvider;
import iq.earthlink.social.personservice.security.SecurityProvider;
import iq.earthlink.social.personservice.security.model.Role;
import iq.earthlink.social.personservice.service.AppleService;
import iq.earthlink.social.personservice.service.FacebookService;
import iq.earthlink.social.personservice.service.GoogleService;
import iq.earthlink.social.personservice.service.KafkaProducerService;
import iq.earthlink.social.personservice.util.CommonProperties;
import iq.earthlink.social.personservice.util.ProfileUtil;
import org.apache.commons.lang3.time.DateUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.dao.DataIntegrityViolationException;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import static iq.earthlink.social.personservice.util.Constants.USER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.*;


class DefaultAuthenticationManagerTest {

    private static final String EMAIL = "user@mail.com";
    private static final String PASSWORD = "Passw0rd";
    private static final String FIRST_NAME = "UserName";
    private static final String LAST_NAME = "UserLastName";
    private static final String ENCODED_PASSWORD = "encoded_password";
    private static final String HTTP_LOCALHOST = "http://localhost";
    private static final String SOME_TOKEN = "some token";
    private static final String OLD_PASSWORD = "OldPassw0rd";
    private static final String NEW_ENCODED_PASSWORD = "new_encoded_password";
    private static final String TOKEN = "token";
    private static final String REFRESH_TOKEN = "refresh token";
    private static final String BIRTH_DAY = "10/10/2000";
    private static final String ERROR_PERSON_WRONG_EMAIL_OR_PASSWORD = "error.person.wrong.email.or.password";
    private static final String ERROR_USER_WITH_EMAIL_NOT_FOUND = "error.user.with.email.not.found";


    @InjectMocks
    private DefaultAuthenticationManager authenticationManager;
    @Mock
    private AuthUtil authUtil;
    @Mock
    private  CommonProperties commonProperties;
    @Mock
    private RabbitTemplate rabbitTemplate;
    @MockBean
    private MinioFileStorage minioFileStorage;
    @MockBean
    private CompositeFileStorageProvider compositeFileStorageProvider;
    @InjectMocks
    @Spy
    private ProfileUtil profileUtil = new ProfileUtil();
    @Mock
    private DefaultSecurityProvider securityProvider;
    @Mock
    private PersonRepository personRepository;
    @Mock
    private ServerAuthProperties serverAuthProperties;
    @Mock
    private BanManager banManager;
    @Mock
    private AppleService appleService;
    @Mock
    private GoogleService googleService;
    @Mock
    private FacebookService facebookService;
    @Mock
    private ResourceProviderRepository resourceProviderRepository;
    @Mock
    private KafkaProducerService kafkaProducerService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    void whenOnlyEmailAndPassword_registrationNotCompleted() {
        //given
        JsonRegistrationData data = JsonRegistrationData
                .builder()
                .email(EMAIL)
                .password(PASSWORD)
                .confirmPassword(PASSWORD)
                .build();

        Person savedPerson = Person
                .builder()
                .email(EMAIL)
                .password(ENCODED_PASSWORD)
                .build();

        Role userRole = new Role();
        userRole.setId(1L);
        userRole.setCode("USER");

        ArgumentCaptor<Person> personCaptor = ArgumentCaptor.forClass(Person.class);
        given(securityProvider.encode(any(String.class))).willReturn(ENCODED_PASSWORD);
        given(securityProvider.getRoles(USER)).willReturn(Set.of(userRole));
        given(personRepository.findByUsernameIgnoreCase(any(String.class))).willReturn(Optional.empty());
        given(commonProperties.getWebUrl()).willReturn(HTTP_LOCALHOST);

        given(personRepository.saveAndFlush(any(Person.class))).willReturn(savedPerson);

        //when
        authenticationManager.register(data);
        //then
        then(personRepository).should().saveAndFlush(personCaptor.capture());
        then(authUtil).should().sendConfirmationEmail(any(Person.class));
        assertThat(personCaptor.getValue().getPassword()).isEqualTo(savedPerson.getPassword());
        assertThat(personCaptor.getValue().getEmail()).isEqualTo(savedPerson.getEmail());

        assertFalse(personCaptor.getValue().isRegistrationCompleted());
    }

    @Test
    void whenAllUserProfileAttributes_registrationCompleted() throws ParseException {
        //given
        JsonRegistrationData data = JsonRegistrationData
                .builder()
                .email(EMAIL)
                .password(PASSWORD)
                .confirmPassword(PASSWORD)
                .firstName(FIRST_NAME)
                .lastName(LAST_NAME)
                .gender(Gender.MALE)
                .birthDate(new SimpleDateFormat( "yyyyMMdd" ).parse( "19900520" ))
                .build();

        Person savedPerson = Person
                .builder()
                .email(EMAIL)
                .password(ENCODED_PASSWORD)
                .firstName(FIRST_NAME)
                .lastName(LAST_NAME)
                .gender(Gender.MALE)
                .birthDate(new SimpleDateFormat( "yyyyMMdd" ).parse( "19900520" ))
                .build();

        Role userRole = new Role();
        userRole.setId(1L);
        userRole.setCode("USER");

        ArgumentCaptor<Person> personCaptor = ArgumentCaptor.forClass(Person.class);
        given(securityProvider.encode(any(String.class))).willReturn(ENCODED_PASSWORD);
        given(securityProvider.getRoles(USER)).willReturn(Set.of(userRole));
        given(personRepository.findByUsernameIgnoreCase(any(String.class))).willReturn(Optional.empty());
        given(commonProperties.getWebUrl()).willReturn(HTTP_LOCALHOST);
        given(commonProperties.getMinAge()).willReturn(18);

        given(personRepository.saveAndFlush(any(Person.class))).willReturn(savedPerson);

        //when
        authenticationManager.register(data);
        //then
        then(personRepository).should().saveAndFlush(personCaptor.capture());
        then(authUtil).should().sendConfirmationEmail(any(Person.class));
        assertThat(personCaptor.getValue().getPassword()).isEqualTo(savedPerson.getPassword());
        assertThat(personCaptor.getValue().getEmail()).isEqualTo(savedPerson.getEmail());
    }

    @Test
    void whenDbReturnsDataIntegrityViolationException_throwNotUniqueException() {
        //given
        JsonRegistrationData data = JsonRegistrationData
                .builder()
                .email(EMAIL)
                .password(PASSWORD)
                .confirmPassword(PASSWORD)
                .build();

        Role userRole = new Role();
        userRole.setId(1L);
        userRole.setCode("USER");

        given(securityProvider.encode(any(String.class))).willReturn(ENCODED_PASSWORD);
        given(securityProvider.getRoles(USER)).willReturn(Set.of(userRole));
        given(personRepository.findByUsernameIgnoreCase(any(String.class))).willReturn(Optional.empty());
        given(commonProperties.getWebUrl()).willReturn(HTTP_LOCALHOST);

        given(personRepository.saveAndFlush(any(Person.class))).willThrow(DataIntegrityViolationException.class);

        //when
        //then
        assertThatThrownBy(() -> authenticationManager.register(data))
                .isInstanceOf(NotUniqueException.class)
                .hasMessageContaining("error.email.already.registered");
    }

    @Test
    void whenNoPersonWithThisEmail_throwNotFoundException() {
        //given
        JsonLoginRequest data = new JsonLoginRequest();
        data.setEmail(EMAIL);
        data.setPassword(PASSWORD);

        //when
        //then
        assertThatThrownBy(() -> authenticationManager.authenticateByEmail(data))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining(ERROR_PERSON_WRONG_EMAIL_OR_PASSWORD);
    }

    @Test
    void whenPersonIsLocked_throwAccountLockedException() {
        //given
        JsonLoginRequest data = new JsonLoginRequest();
        data.setEmail(EMAIL);
        data.setPassword(PASSWORD);

        Person person = Person
                .builder()
                .email(EMAIL)
                .password(ENCODED_PASSWORD)
                .accountNonLocked(false)
                .lockTime(new Date())
                .build();

        given(personRepository.findByEmailIgnoreCase(data.getEmail())).willReturn(person);
        given(serverAuthProperties.getLockTimeDuration()).willReturn(1);

        //when
        //then
        assertThatThrownBy(() -> authenticationManager.authenticateByEmail(data))
                .isInstanceOf(AccountLockedException.class)
                .hasMessageContaining("error.user.account.locked");
    }

    @Test
    void whenPersonPasswordIsNull_throwNotFoundException() {
        //given
        JsonLoginRequest data = new JsonLoginRequest();
        data.setEmail(EMAIL);
        data.setPassword(PASSWORD);

        Person person = Person
                .builder()
                .email(EMAIL)
                .password(null)
                .build();

        given(personRepository.findByEmailIgnoreCase(data.getEmail())).willReturn(person);
        given(serverAuthProperties.getMaxLoginAttempts()).willReturn(3);

        //when
        //then
        assertThatThrownBy(() -> authenticationManager.authenticateByEmail(data))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining(ERROR_PERSON_WRONG_EMAIL_OR_PASSWORD);
    }

    @Test
    void whenPersonPasswordsNotMach_throwNotFoundException() {
        //given
        JsonLoginRequest data = new JsonLoginRequest();
        data.setEmail(EMAIL);
        data.setPassword(PASSWORD);

        Person person = Person
                .builder()
                .email(EMAIL)
                .password(ENCODED_PASSWORD)
                .build();

        given(personRepository.findByEmailIgnoreCase(data.getEmail())).willReturn(person);
        given(serverAuthProperties.getMaxLoginAttempts()).willReturn(3);
        given(securityProvider.matchPassword(any(), any())).willReturn(false);

        //when
        //then
        assertThatThrownBy(() -> authenticationManager.authenticateByEmail(data))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining(ERROR_PERSON_WRONG_EMAIL_OR_PASSWORD);
    }

    @Test
    void whenPersonPasswordsNotMachAndTooManyFailedAttempts_throwAccountLockedException() {
        //given
        JsonLoginRequest data = new JsonLoginRequest();
        data.setEmail(EMAIL);
        data.setPassword(PASSWORD);

        Person person = Person
                .builder()
                .email(EMAIL)
                .password(ENCODED_PASSWORD)
                .failedAttempt(3)
                .build();

        given(personRepository.findByEmailIgnoreCase(data.getEmail())).willReturn(person);
        given(serverAuthProperties.getMaxLoginAttempts()).willReturn(3);
        given(securityProvider.matchPassword(any(), any())).willReturn(false);

        //when
        //then
        assertThatThrownBy(() -> authenticationManager.authenticateByEmail(data))
                .isInstanceOf(AccountLockedException.class)
                .hasMessageContaining("error.user.account.locked");
    }

    @Test
    void whenPersonIsNotConfirmed_throwForbiddenException() {
        //given
        JsonLoginRequest data = new JsonLoginRequest();
        data.setEmail(EMAIL);
        data.setPassword(PASSWORD);

        Person person = Person
                .builder()
                .email(EMAIL)
                .password(ENCODED_PASSWORD)
                .isConfirmed(false)
                .build();

        given(personRepository.findByEmailIgnoreCase(data.getEmail())).willReturn(person);
        given(securityProvider.matchPassword(any(), any())).willReturn(true);


        //when
        //then
        assertThatThrownBy(() -> authenticationManager.authenticateByEmail(data))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("error.email.not.confirmed");
    }

    @Test
    void whenPersonIsBanned_throwForbiddenException() {
        //given
        JsonLoginRequest data = new JsonLoginRequest();
        data.setEmail(EMAIL);
        data.setPassword(PASSWORD);

        Person person = Person
                .builder()
                .email(EMAIL)
                .password(ENCODED_PASSWORD)
                .isConfirmed(true)
                .build();

        PersonBan personBan = new PersonBan();
        Date expiredDate = DateUtils.addDays(new Date(), 3);
        personBan.setId(person.getId());
        personBan.setExpiredAt(expiredDate);
        List<PersonBan> personBans = Collections.singletonList(personBan);

        given(personRepository.findByEmailIgnoreCase(data.getEmail())).willReturn(person);
        given(securityProvider.matchPassword(any(), any())).willReturn(true);
        given(banManager.getActiveBans(person.getId())).willReturn(personBans);

        //when
        //then
        assertThatThrownBy(() -> authenticationManager.authenticateByEmail(data))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("error.person.is.banned.reason");
    }

    @Test
    void whenProvidedValidData_returnJsonAuthentication() {
        //given
        JsonLoginRequest data = new JsonLoginRequest();
        data.setEmail(EMAIL);
        data.setPassword(PASSWORD);

        Person person = Person
                .builder()
                .email(EMAIL)
                .password(ENCODED_PASSWORD)
                .isConfirmed(true)
                .build();

        String token = SOME_TOKEN;
        String refreshToken = "some refresh token";
        Date date = new Date(new Date().getTime() + (1000 * 60 * 60));

        given(personRepository.findByEmailIgnoreCase(data.getEmail())).willReturn(person);
        given(securityProvider.matchPassword(any(), any())).willReturn(true);
        given(securityProvider.generateToken(any())).willReturn(token);
        given(securityProvider.generateRefreshToken(any())).willReturn(refreshToken);
        given(securityProvider.getExpireTime(any())).willReturn(date);

        //when
        JsonAuthentication jsonAuthentication = authenticationManager.authenticateByEmail(data);

        //then
        then(securityProvider).should().generateToken(any(Person.class));
        then(securityProvider).should().generateRefreshToken(any(Person.class));
        then(securityProvider).should().getExpireTime(any());

        assertEquals(jsonAuthentication.getToken(), token);
        assertEquals(jsonAuthentication.getRefreshToken(), refreshToken);
        assertEquals(jsonAuthentication.getExpireTime(), date);
    }

    @Test
    void whenChangePasswordForNotExistingPerson_throwNotFoundException() {
        //given
        Person person = Person
                .builder()
                .email(EMAIL)
                .password(ENCODED_PASSWORD)
                .build();

        JsonChangePasswordRequest data = new JsonChangePasswordRequest();
        data.setOldPassword(OLD_PASSWORD);
        data.setPassword(PASSWORD);
        data.setConfirmPassword(PASSWORD);

        given(personRepository.findById(person.getId())).willReturn(Optional.empty());

        //when
        //then
        assertThatThrownBy(() -> authenticationManager.changePassword(person, data))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("error.person.not.found.byId");
    }

    @Test
    void whenChangePasswordAndProvideWrongOldPassword_throwBadRequestException() {
        //given
        Person person = Person
                .builder()
                .email(EMAIL)
                .password(ENCODED_PASSWORD)
                .build();

        JsonChangePasswordRequest data = new JsonChangePasswordRequest();
        data.setOldPassword(OLD_PASSWORD);
        data.setPassword(PASSWORD);
        data.setConfirmPassword(PASSWORD);

        given(personRepository.findById(person.getId())).willReturn(Optional.of(person));
        given(securityProvider.matchPassword(any(), any())).willReturn(false);

        //when
        //then
        assertThatThrownBy(() -> authenticationManager.changePassword(person, data))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("error.old.password.is.wrong");
    }

    @Test
    void whenChangePasswordAndProvideWrongConfirmPassword_throwBadRequestException() {
        //given
        Person person = Person
                .builder()
                .email(EMAIL)
                .password(ENCODED_PASSWORD)
                .build();

        JsonChangePasswordRequest data = new JsonChangePasswordRequest();
        data.setOldPassword(OLD_PASSWORD);
        data.setPassword(PASSWORD);
        data.setConfirmPassword("WrongPassw0rd");

        given(personRepository.findById(person.getId())).willReturn(Optional.of(person));
        given(securityProvider.matchPassword(any(), any())).willReturn(true);

        //when
        //then
        assertThatThrownBy(() -> authenticationManager.changePassword(person, data))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("error.password.confirmPassword.should.match");
    }

    @Test
    void whenChangePasswordAndProvideValidData_passwordChanged() {
        //given
        Person person = Person
                .builder()
                .email(EMAIL)
                .password(ENCODED_PASSWORD)
                .build();

        JsonChangePasswordRequest data = new JsonChangePasswordRequest();
        data.setOldPassword(OLD_PASSWORD);
        data.setPassword(PASSWORD);
        data.setConfirmPassword(PASSWORD);

        given(personRepository.findById(person.getId())).willReturn(Optional.of(person));
        given(securityProvider.matchPassword(any(), any())).willReturn(true);
        given(securityProvider.encode(data.getPassword())).willReturn(NEW_ENCODED_PASSWORD);
        ArgumentCaptor<Person> personCaptor = ArgumentCaptor.forClass(Person.class);

        //when
        authenticationManager.changePassword(person, data);

        //then
        then(personRepository).should().save(personCaptor.capture());
        assertThat(personCaptor.getValue().getPassword()).isEqualTo(NEW_ENCODED_PASSWORD);
        assertThat(personCaptor.getValue().getEmail()).isEqualTo(person.getEmail());
    }

    @Test
    void whenForgotPasswordAndEmailDoesntExistInDB_throwNotFoundException() {
        //given
        JsonForgotPasswordRequest data = new JsonForgotPasswordRequest();
        data.setEmail(EMAIL);
        data.setCaptchaResponse("Captcha response");
        data.setSiteKey("Captcha site key");

        given(personRepository.findByEmailIgnoreCase(any())).willReturn(null);

        //when
        //then
        assertThatThrownBy(() -> authenticationManager.forgotPassword(data))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining(ERROR_USER_WITH_EMAIL_NOT_FOUND);
    }

    @Test
    void whenForgotPasswordProvideValidData_sendResetPasswordEmail() {
        //given
        Person person = Person
                .builder()
                .email(EMAIL)
                .password(ENCODED_PASSWORD)
                .build();

        JsonForgotPasswordRequest data = new JsonForgotPasswordRequest();
        data.setEmail(EMAIL);
        data.setCaptchaResponse("Captcha response");
        data.setSiteKey("Captcha site key");

        given(personRepository.findByEmailIgnoreCase(any())).willReturn(person);
        given(commonProperties.getCodeExpirationInMinutes()).willReturn(60L);
        ArgumentCaptor<Person> personCaptor = ArgumentCaptor.forClass(Person.class);

        //when
        authenticationManager.forgotPassword(data);

        //then
        then(personRepository).should().save(personCaptor.capture());
        then(authUtil).should().sendResetPasswordEmail(any(), any());
        assertThat(personCaptor.getValue().getPassword()).isEqualTo(person.getPassword());
        assertThat(personCaptor.getValue().getEmail()).isEqualTo(person.getEmail());
        assertNotNull(personCaptor.getValue().getResetCode());
    }

    @Test
    void whenResetPasswordAndEmailDoesntExistInDB_throwNotFoundException() {
        //given
        JsonResetPasswordRequest data = new JsonResetPasswordRequest();
        data.setEmail(EMAIL);
        data.setPassword(PASSWORD);
        data.setConfirmationPassword(PASSWORD);
        data.setCode(123);

        given(personRepository.findByEmailIgnoreCase(any())).willReturn(null);

        //when
        //then
        assertThatThrownBy(() -> authenticationManager.resetPassword(data))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining(ERROR_USER_WITH_EMAIL_NOT_FOUND);
    }

    @Test
    void whenResetPasswordAndProveWrongResetCode_throwBadRequestException() {
        //given
        Person person = Person
                .builder()
                .email(EMAIL)
                .password(ENCODED_PASSWORD)
                .resetCode(321)
                .build();

        JsonResetPasswordRequest data = new JsonResetPasswordRequest();
        data.setEmail(EMAIL);
        data.setPassword(PASSWORD);
        data.setConfirmationPassword(PASSWORD);
        data.setCode(123);

        given(personRepository.findByEmailIgnoreCase(any())).willReturn(person);

        //when
        //then
        assertThatThrownBy(() -> authenticationManager.resetPassword(data))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("error.password.reset.session.expired");
    }

    @Test
    void whenResetPasswordAndProveExpiredResetCode_throwBadRequestException() {
        //given
        Person person = Person
                .builder()
                .email(EMAIL)
                .password(ENCODED_PASSWORD)
                .resetCode(123)
                .resetCodeExpireAt(new Date(new Date().getTime() - (1000 * 60 * 60 * 24)))
                .build();

        JsonResetPasswordRequest data = new JsonResetPasswordRequest();
        data.setEmail(EMAIL);
        data.setPassword(PASSWORD);
        data.setConfirmationPassword(PASSWORD);
        data.setCode(123);

        given(personRepository.findByEmailIgnoreCase(any())).willReturn(person);

        //when
        //then
        assertThatThrownBy(() -> authenticationManager.resetPassword(data))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("error.password.reset.session.expired");
    }

    @Test
    void whenResetPasswordButPasswordsNotMach_throwBadRequestException() {
        //given
        Person person = Person
                .builder()
                .email(EMAIL)
                .password(ENCODED_PASSWORD)
                .resetCode(123)
                .resetCodeExpireAt(new Date(new Date().getTime() + (1000 * 60 * 60 * 24)))
                .build();

        JsonResetPasswordRequest data = new JsonResetPasswordRequest();
        data.setEmail(EMAIL);
        data.setPassword(PASSWORD);
        data.setConfirmationPassword("WrongPassw0rd");
        data.setCode(123);

        given(personRepository.findByEmailIgnoreCase(any())).willReturn(person);

        //when
        //then
        assertThatThrownBy(() -> authenticationManager.resetPassword(data))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("error.password.confirmPassword.should.match");
    }

    @Test
    void whenResetPasswordWithValidData_resetPassword() {
        //given
        Person person = Person
                .builder()
                .email(EMAIL)
                .password(ENCODED_PASSWORD)
                .resetCode(123)
                .resetCodeExpireAt(new Date(new Date().getTime() + (1000 * 60 * 60 * 24)))
                .build();

        JsonResetPasswordRequest data = new JsonResetPasswordRequest();
        data.setEmail(EMAIL);
        data.setPassword(PASSWORD);
        data.setConfirmationPassword(PASSWORD);
        data.setCode(123);

        given(personRepository.findByEmailIgnoreCase(any())).willReturn(person);
        ArgumentCaptor<Person> personCaptor = ArgumentCaptor.forClass(Person.class);
        given(securityProvider.encode(data.getPassword())).willReturn(NEW_ENCODED_PASSWORD);

        //when
        authenticationManager.resetPassword(data);

        //then
        then(personRepository).should().save(personCaptor.capture());
        assertThat(personCaptor.getValue().getPassword()).isEqualTo(NEW_ENCODED_PASSWORD);
        assertThat(personCaptor.getValue().getEmail()).isEqualTo(person.getEmail());
        assertNull(personCaptor.getValue().getResetCode());
        assertNull(personCaptor.getValue().getResetCodeExpireAt());
    }

    @Test
    void whenConfirmEmailWithNullCode_throwRestApiException() {
        //given
        String code = null;

        //when
        //then
        assertThatThrownBy(() -> authenticationManager.confirmEmail(EMAIL, code))
                .isInstanceOf(RestApiException.class)
                .hasMessageContaining("error.check.not.null");
    }

    @Test
    void whenConfirmEmailWithBlankEmail_throwBadRequestException() {
        //given
        String email = "";
        String code = "123";

        //when
        //then
        assertThatThrownBy(() -> authenticationManager.confirmEmail(email, code))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("error.email.not.blank");
    }

    @Test
    void whenConfirmEmailWithNotExistingPerson_throwNotFoundException() {
        //given
        String code = "123";

        given(personRepository.findByEmailIgnoreCase(any())).willReturn(null);

        //when
        //then
        assertThatThrownBy(() -> authenticationManager.confirmEmail(EMAIL, code))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining(ERROR_USER_WITH_EMAIL_NOT_FOUND);
    }

    @Test
    void whenConfirmEmailWithWrongCode_throwBadRequestException() {
        //given
        String code = "123";
        Person person = Person
                .builder()
                .email(EMAIL)
                .password(ENCODED_PASSWORD)
                .confirmCode("321")
                .build();

        given(personRepository.findByEmailIgnoreCase(any())).willReturn(person);

        //when
        //then
        assertThatThrownBy(() -> authenticationManager.confirmEmail(EMAIL, code))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("error.email.or.code.wrong");
    }

    @Test
    void whenConfirmEmailWithValidData_confirmEmail() {
        //given
        String code = "123";
        Person person = Person
                .builder()
                .email(EMAIL)
                .password(ENCODED_PASSWORD)
                .confirmCode("123")
                .build();

        given(personRepository.findByEmailIgnoreCase(any())).willReturn(person);
        ArgumentCaptor<Person> personCaptor = ArgumentCaptor.forClass(Person.class);

        //when
        authenticationManager.confirmEmail(EMAIL, code);

        //then
        then(personRepository).should().saveAndFlush(personCaptor.capture());
        assertThat(personCaptor.getValue().getPassword()).isEqualTo(person.getPassword());
        assertThat(personCaptor.getValue().getEmail()).isEqualTo(person.getEmail());
        assertThat(personCaptor.getValue().getState()).isEqualTo(RegistrationState.EMAIL_CONFIRMED);
        assertTrue(personCaptor.getValue().isConfirmed());
        assertNull(personCaptor.getValue().getConfirmCode());
    }

    @Test
    void whenResendConfirmationEmailForNotExistingPerson_throwNotFoundException() {
        //given
        given(personRepository.findByEmailIgnoreCase(any())).willReturn(null);

        //when
        //then
        assertThatThrownBy(() -> authenticationManager.resendConfirmationEmail(EMAIL))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining(ERROR_USER_WITH_EMAIL_NOT_FOUND);
    }

    @Test
    void whenResendConfirmationEmailForAlreadyConfirmedPerson_throwNotFoundException() {
        //given
        Person person = Person
                .builder()
                .email(EMAIL)
                .password(ENCODED_PASSWORD)
                .isConfirmed(true)
                .build();

        given(personRepository.findByEmailIgnoreCase(any())).willReturn(person);

        //when
        //then
        assertThatThrownBy(() -> authenticationManager.resendConfirmationEmail(EMAIL))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining(ERROR_USER_WITH_EMAIL_NOT_FOUND);
    }

    @Test
    void whenResendConfirmationEmailWithValidData_resendConfirmationEmail() {
        //given
        String confirmCode = "-1";

        Person person = Person
                .builder()
                .email(EMAIL)
                .password(ENCODED_PASSWORD)
                .isConfirmed(false)
                .confirmCode(confirmCode)
                .build();

        given(personRepository.findByEmailIgnoreCase(any())).willReturn(person);
        ArgumentCaptor<Person> personCaptor = ArgumentCaptor.forClass(Person.class);

        //when
        authenticationManager.resendConfirmationEmail(EMAIL);

        //then
        then(personRepository).should().save(personCaptor.capture());
        assertThat(personCaptor.getValue().getPassword()).isEqualTo(person.getPassword());
        assertThat(personCaptor.getValue().getEmail()).isEqualTo(person.getEmail());
        assertNotNull(personCaptor.getValue().getConfirmCode());
        assertNotEquals(personCaptor.getValue().getConfirmCode(), confirmCode);
    }

    @Test
    void whenRefreshTokenWithNotValidToken_throwInvalidTokenRequestException() {
        //given
        String refreshToken = "not valid token";

        given(securityProvider.isValidToken(refreshToken)).willReturn(false);

        //when
        //then
        assertThatThrownBy(() -> authenticationManager.refreshToken(refreshToken))
                .isInstanceOf(InvalidTokenRequestException.class)
                .hasMessageContaining("error.refresh.token.invalid");
    }

    @Test
    void whenRefreshTokenWithIncorrectTokenType_throwInvalidTokenRequestException() {
        //given
        String token = "not refresh token";

        given(securityProvider.isValidToken(token)).willReturn(true);
        given(securityProvider.getTypeFromJWT(token)).willReturn(TOKEN);

        //when
        //then
        assertThatThrownBy(() -> authenticationManager.refreshToken(token))
                .isInstanceOf(InvalidTokenRequestException.class)
                .hasMessageContaining("error.refresh.token.invalid");
    }

    @Test
    void whenRefreshTokenForNotExistingPerson_throwNotFoundException() {
        //given
        String refreshToken = REFRESH_TOKEN;
        UUID uuid = UUID.randomUUID();

        given(securityProvider.isValidToken(refreshToken)).willReturn(true);
        given(securityProvider.getTypeFromJWT(refreshToken)).willReturn("refreshToken");
        given(securityProvider.getSubjectFromJWT(refreshToken)).willReturn(String.valueOf(uuid));
        given(personRepository.findByUuid(uuid)).willReturn(Optional.empty());

        //when
        //then
        assertThatThrownBy(() -> authenticationManager.refreshToken(refreshToken))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void whenRefreshTokenWithForNotExistingPerson_throwNotFoundException() {
        //given
        String token = SOME_TOKEN;
        String refreshToken = REFRESH_TOKEN;
        Date expireTime = new Date(new Date().getTime() + (1000 * 60 * 60));
        UUID uuid = UUID.randomUUID();
        Person person = Person
                .builder()
                .email(EMAIL)
                .password(ENCODED_PASSWORD)
                .build();

        given(securityProvider.isValidToken(refreshToken)).willReturn(true);
        given(securityProvider.getTypeFromJWT(refreshToken)).willReturn("refreshToken");
        given(securityProvider.getSubjectFromJWT(refreshToken)).willReturn(String.valueOf(uuid));
        given(personRepository.findByUuid(uuid)).willReturn(Optional.of(person));
        given(securityProvider.generateToken(any())).willReturn(token);
        given(securityProvider.generateRefreshToken(any())).willReturn(refreshToken);
        given(securityProvider.getExpireTime(any())).willReturn(expireTime);

        //when
        Map<String, Object> tokens = authenticationManager.refreshToken(refreshToken);

        //then
        then(securityProvider).should().blockToken(refreshToken);
        assertEquals(tokens.get(SecurityProvider.TOKEN), token);
        assertEquals(tokens.get(SecurityProvider.REFRESH_TOKEN), refreshToken);
        assertEquals(tokens.get(SecurityProvider.EXPIRE_TIME), expireTime);
    }

    @Test
    void whenAuthenticateWithFacebookAndProviderIsPresent_returnJsonAuthentication() {
        //given
        JsonSSORequest data = new JsonSSORequest();
        data.setAccessToken(TOKEN);
        data.setFirstName(FIRST_NAME);
        data.setLastName(LAST_NAME);
        data.setClientId("123");

        SSOUserModel ssoUserModel = new SSOUserModel();
        ssoUserModel.setId("123");
        ssoUserModel.setFirstName(FIRST_NAME);
        ssoUserModel.setLastName(LAST_NAME);
        ssoUserModel.setEmail(EMAIL);
        ssoUserModel.setGender("male");
        ssoUserModel.setBirthday(BIRTH_DAY);
        ssoUserModel.setProviderName(ProviderName.FACEBOOK);

        Person person = Person
                .builder()
                .email(EMAIL)
                .password(ENCODED_PASSWORD)
                .failedAttempt(2)
                .build();

        ResourceProvider resourceProvider = ResourceProvider.builder()
                .id(1L)
                .providerId("234")
                .providerName(ProviderName.FACEBOOK)
                .person(person)
                .createdAt(new Date())
                .build();

        String token = SOME_TOKEN;
        String refreshToken = REFRESH_TOKEN;
        Date expireTime = new Date(new Date().getTime() + (1000 * 60 * 60));

        given(facebookService.getUserDetails(any(), any())).willReturn(ssoUserModel);
        given(resourceProviderRepository.findByProviderIdAndProviderName(any(), any())).willReturn(Optional.ofNullable(resourceProvider));
        given(securityProvider.generateToken(any())).willReturn(token);
        given(securityProvider.generateRefreshToken(any())).willReturn(refreshToken);
        given(securityProvider.getExpireTime(any())).willReturn(expireTime);

        //when
        JsonAuthentication jsonAuthentication = authenticationManager.authenticateWithFacebook(data);

        //then
        assertEquals(token, jsonAuthentication.getToken());
        assertEquals(refreshToken, jsonAuthentication.getRefreshToken());
        assertEquals(expireTime, jsonAuthentication.getExpireTime());
        assertEquals(0, person.getFailedAttempt());
    }

    @Test
    void whenAuthenticateWithFacebookAndProviderIsNotPresentAndEmailIsNull_returnJsonAuthentication() {
        //given
        JsonSSORequest data = new JsonSSORequest();
        data.setAccessToken(TOKEN);
        data.setFirstName(FIRST_NAME);
        data.setLastName(LAST_NAME);
        data.setClientId("123");

        SSOUserModel ssoUserModel = new SSOUserModel();
        ssoUserModel.setId("123");
        ssoUserModel.setFirstName(FIRST_NAME);
        ssoUserModel.setLastName(LAST_NAME);
        ssoUserModel.setEmail(null);
        ssoUserModel.setGender("male");
        ssoUserModel.setBirthday(BIRTH_DAY);
        ssoUserModel.setProviderName(ProviderName.FACEBOOK);

        Role role = new Role();
        role.setId(345L);
        role.setCode(USER);

        String token = SOME_TOKEN;
        String refreshToken = REFRESH_TOKEN;
        Date expireTime = new Date(new Date().getTime() + (1000 * 60 * 60));

        given(facebookService.getUserDetails(any(), any())).willReturn(ssoUserModel);
        given(resourceProviderRepository.findByProviderIdAndProviderName(any(), any())).willReturn(Optional.empty());
        given(securityProvider.getRoles(USER)).willReturn(new HashSet<>(Collections.singletonList(role)));
        given(securityProvider.generateToken(any())).willReturn(token);
        given(securityProvider.generateRefreshToken(any())).willReturn(refreshToken);
        given(securityProvider.getExpireTime(any())).willReturn(expireTime);
        ArgumentCaptor<Person> personCaptor = ArgumentCaptor.forClass(Person.class);
        ArgumentCaptor<ResourceProvider> resourceProviderCaptor = ArgumentCaptor.forClass(ResourceProvider.class);

        //when
        JsonAuthentication jsonAuthentication = authenticationManager.authenticateWithFacebook(data);

        //then
        then(personRepository).should().save(personCaptor.capture());
        assertEquals(token, jsonAuthentication.getToken());
        assertEquals(refreshToken, jsonAuthentication.getRefreshToken());
        assertEquals(expireTime, jsonAuthentication.getExpireTime());
        assertNull(personCaptor.getValue().getEmail());
        assertEquals(FIRST_NAME, personCaptor.getValue().getFirstName());
        assertEquals(LAST_NAME, personCaptor.getValue().getLastName());
        assertTrue(personCaptor.getValue().isRegistrationCompleted());

        then(resourceProviderRepository).should().save(resourceProviderCaptor.capture());
        assertEquals(resourceProviderCaptor.getValue().getPerson().getId(), personCaptor.getValue().getId());
    }

    @Test
    void whenAuthenticateWithFacebookAndProviderIsNotPresentAndEmailIsNotNull_returnJsonAuthentication() {
        //given
        JsonSSORequest data = new JsonSSORequest();
        data.setAccessToken(TOKEN);
        data.setFirstName(FIRST_NAME);
        data.setLastName(LAST_NAME);
        data.setClientId("123");

        SSOUserModel ssoUserModel = new SSOUserModel();
        ssoUserModel.setId("123");
        ssoUserModel.setFirstName(FIRST_NAME);
        ssoUserModel.setLastName(LAST_NAME);
        ssoUserModel.setEmail(EMAIL);
        ssoUserModel.setGender("male");
        ssoUserModel.setBirthday(BIRTH_DAY);
        ssoUserModel.setProviderName(ProviderName.FACEBOOK);

        Role role = new Role();
        role.setId(345L);
        role.setCode(USER);

        String token = SOME_TOKEN;
        String refreshToken = REFRESH_TOKEN;
        Date expireTime = new Date(new Date().getTime() + (1000 * 60 * 60));

        given(facebookService.getUserDetails(any(), any())).willReturn(ssoUserModel);
        given(resourceProviderRepository.findByProviderIdAndProviderName(any(), any())).willReturn(Optional.empty());
        given(securityProvider.getRoles(USER)).willReturn(new HashSet<>(Collections.singletonList(role)));
        given(securityProvider.generateToken(any())).willReturn(token);
        given(securityProvider.generateRefreshToken(any())).willReturn(refreshToken);
        given(securityProvider.getExpireTime(any())).willReturn(expireTime);
        ArgumentCaptor<Person> personCaptor = ArgumentCaptor.forClass(Person.class);
        ArgumentCaptor<ResourceProvider> resourceProviderCaptor = ArgumentCaptor.forClass(ResourceProvider.class);

        //when
        JsonAuthentication jsonAuthentication = authenticationManager.authenticateWithFacebook(data);

        //then
        then(personRepository).should().save(personCaptor.capture());
        assertEquals(token, jsonAuthentication.getToken());
        assertEquals(refreshToken, jsonAuthentication.getRefreshToken());
        assertEquals(expireTime, jsonAuthentication.getExpireTime());
        assertEquals(EMAIL, personCaptor.getValue().getEmail());
        assertEquals(FIRST_NAME, personCaptor.getValue().getFirstName());
        assertEquals(LAST_NAME, personCaptor.getValue().getLastName());
        assertTrue(personCaptor.getValue().isRegistrationCompleted());

        then(resourceProviderRepository).should().save(resourceProviderCaptor.capture());
        assertEquals(resourceProviderCaptor.getValue().getPerson().getId(), personCaptor.getValue().getId());

    }
}