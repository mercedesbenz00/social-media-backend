package iq.earthlink.social.personservice.controller.rest.person.v1;

import io.swagger.annotations.*;
import iq.earthlink.social.exception.BadRequestException;
import iq.earthlink.social.exception.ForbiddenException;
import iq.earthlink.social.personservice.authentication.enumeration.AuthMethod;
import iq.earthlink.social.personservice.authentication.manager.AuthenticationManager;
import iq.earthlink.social.personservice.authentication.manager.CaptchaService;
import iq.earthlink.social.personservice.config.ServerAuthProperties;
import iq.earthlink.social.personservice.controller.CurrentUser;
import iq.earthlink.social.personservice.dto.JsonPersonReported;
import iq.earthlink.social.personservice.dto.PersonDTO;
import iq.earthlink.social.personservice.dto.PersonStatus;
import iq.earthlink.social.personservice.person.PersonComplaintSearchCriteria;
import iq.earthlink.social.personservice.person.PersonManager;
import iq.earthlink.social.personservice.person.PersonSearchCriteria;
import iq.earthlink.social.personservice.person.RegistrationData;
import iq.earthlink.social.personservice.person.model.Person;
import iq.earthlink.social.personservice.person.model.PersonComplaint;
import iq.earthlink.social.personservice.person.rest.*;
import iq.earthlink.social.personservice.security.SecurityProvider;
import org.apache.commons.lang3.StringUtils;
import org.dozer.Mapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * The API controller responsible for managing {@link Person} resource.
 */
@Api(tags = "Person Api", value = "PersonApi")
@RestController
@RequestMapping(value = "/api/v1/persons", produces = MediaType.APPLICATION_JSON_VALUE)
@Validated
public class PersonApi {

    private static final Logger LOGGER = LoggerFactory.getLogger(PersonApi.class);

    private final PersonManager personManager;
    private final SecurityProvider securityProvider;
    private final AuthenticationManager authenticationManager;
    private final CaptchaService captchaService;
    private final ServerAuthProperties authProperties;
    private final Mapper mapper;
    private final RedisTemplate<String, Long> migrationFlag;
    private static final String BEARER = "Bearer ";

    public PersonApi(PersonManager personManager,
                     SecurityProvider securityProvider,
                     AuthenticationManager authenticationManager,
                     CaptchaService captchaService,
                     ServerAuthProperties authProperties,
                     Mapper mapper,
                     RedisTemplate<String, Long> migrationFlag) {
        this.personManager = personManager;
        this.securityProvider = securityProvider;
        this.authenticationManager = authenticationManager;
        this.captchaService = captchaService;
        this.authProperties = authProperties;
        this.mapper = mapper;
        this.migrationFlag = migrationFlag;
    }

    @ApiOperation("Trigger to push all persons to kafka")
    @GetMapping("/migration")
    public void triggerPersonsEvents(@CurrentUser PersonDTO personDTO) {
        if (personDTO.isAdmin()) {
            LOGGER.debug("Triggered member migration by user: {}", personDTO.getDisplayName());
            migrationFlag.opsForValue().set("personsMigrationFlag", 0L);
        }
    }

    /**
     * @deprecated
     */
    @ApiOperation(
            value = "Returns the person information retrieved by person id",
            response = JsonPerson.class)
    @ApiResponses({
            @ApiResponse(code = 404, message = "If the person is not found"),
            @ApiResponse(code = 500, message = "If any unexpected server error")
    })
    @GetMapping("/{personId}")
    @Deprecated(forRemoval = true)
    public ResponseEntity<JsonPerson> getPerson(@PathVariable Long personId) {
        LOGGER.debug("Received request get person info for person: {}", personId);

        return ResponseEntity.status(HttpStatus.OK)
                .body(mapper.map(personManager.getPersonByIdInternal(personId), JsonPerson.class));
    }

    @ApiOperation(
            value = "Returns the person information retrieved by person id",
            response = JsonPerson.class)
    @ApiResponses({
            @ApiResponse(code = 404, message = "If the person is not found"),
            @ApiResponse(code = 500, message = "If any unexpected server error")
    })
    @GetMapping("/by-id/{personId}")
    public ResponseEntity<JsonPerson> getPersonById(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable Long personId) {
        LOGGER.debug("Received request to get person info for person by ID: {}", personId);
        Long currentUserId = securityProvider.getPersonIdFromAuthorization(authorizationHeader);
        //TODO: create separate API for admin if required with full profile
        return ResponseEntity.status(HttpStatus.OK).body(personManager.getPersonById(currentUserId, personId));
    }

    @ApiOperation(
            value = "Returns the person information retrieved by username",
            response = JsonPerson.class)
    @ApiResponses({
            @ApiResponse(code = 404, message = "If the person is not found"),
            @ApiResponse(code = 500, message = "If any unexpected server error")
    })
    @GetMapping(value = "/by-name/{username:.+}")
    public ResponseEntity<JsonPerson> getPersonByUsername(@PathVariable String username) {
        LOGGER.debug("Received request to get person info for person by username: {}", username);
        JsonPerson person = mapper.map(personManager.getPersonByUsername(username), JsonPerson.class);
        //TODO: create separate API for admin if required with full profile
        return ResponseEntity.status(HttpStatus.OK).body(person);
    }

    @ApiOperation("Searches persons by provided criteria")
    @GetMapping
    public Page<JsonPerson> findPersons(
            @RequestHeader("Authorization") String authorizationHeader,
            @ApiParam("The query param allows find persons matched by: display name and/or first/last name")
            @RequestParam(name = "query", required = false) String query,
            @RequestParam(name = "displayNameQuery", required = false) String displayNameQuery,
            @RequestParam(name = "personIds", required = false) Long[] personIds,
            @RequestParam(name = "personIdsToExclude", required = false) Long[] personIdsToExclude,
            @RequestParam(name = "groupIds", required = false) List<Long> groupIds,
            @RequestParam(name = "followingsFirst", defaultValue = "false") Boolean followingsFirst,
            Pageable pageable,
            @CurrentUser PersonDTO person) {
        LOGGER.debug("Received 'find persons' request from person: {}", person.getId());

        PersonSearchCriteria criteria = PersonSearchCriteria.builder()
                .query(query)
                .currentPersonId(person.getId())
                .displayNameQuery(displayNameQuery)
                .followingsFirst(followingsFirst)
                .personIds(personIds)
                .personIdsToExclude(personIdsToExclude)
                .showDeleted(false)
                .build();

        return personManager.findPersons(authorizationHeader, criteria, groupIds, pageable);
    }

    @ApiOperation("For admin only: find persons with detailed info by provided criteria")
    @GetMapping("/info")
    public Page<JsonPersonProfile> findPersonsInfo(
            @RequestHeader("Authorization") String authorizationHeader,
            @ApiParam("The query param allows find persons matched by: display name and/or first/last name")
            @RequestParam(name = "query", required = false) String query,
            @RequestParam(name = "displayNameQuery", required = false) String displayNameQuery,
            @RequestParam(name = "personIds", required = false) Long[] personIds,
            @RequestParam(name = "personIdsToExclude", required = false) Long[] personIdsToExclude,
            @RequestParam(name = "groupIds", required = false) List<Long> groupIds,
            @RequestParam(name = "followingsFirst", defaultValue = "false") Boolean followingsFirst,
            Pageable pageable) {
        Long personId = securityProvider.getPersonIdFromAuthorization(authorizationHeader);
        Person person = personManager.getPersonByIdInternal(personId);
        LOGGER.debug("Received 'find persons info' request from person: {}", person.getId());

        if (person.isAdmin()) {
            PersonSearchCriteria criteria = PersonSearchCriteria.builder()
                    .query(query)
                    .currentPersonId(person.getId())
                    .displayNameQuery(displayNameQuery)
                    .followingsFirst(followingsFirst)
                    .personIds(personIds)
                    .personIdsToExclude(personIdsToExclude)
                    .showDeleted(true)
                    .build();
            return personManager.findPersonsByAdmin(authorizationHeader, criteria, groupIds, pageable);
        } else {
            throw new ForbiddenException("error.operation.not.permitted");
        }
    }

    @ApiOperation("Returns the list of the comments which has complaints")
    @GetMapping("/search/with-complaints")
    public Page<JsonPersonReported> getPersonsWithComplaints(
            @RequestHeader("Authorization") String authorizationHeader,
            @RequestParam(required = false, defaultValue = "PENDING") PersonComplaint.PersonComplaintState complaintState,
            @RequestParam(required = false, defaultValue = "ACTIVE") PersonStatus personStatus,
            @RequestParam(name = "query", required = false) String query,
            @RequestParam(name = "personIds", required = false) Long[] personIds,
            @RequestParam(name = "fromDate", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate fromDate,
            @RequestParam(name = "toDate", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate toDate,
            Pageable page,
            @CurrentUser PersonDTO currentUser) {
        LOGGER.debug("Received 'find persons with complaints' request from person: {}", currentUser.getId());

        PersonComplaintSearchCriteria criteria = PersonComplaintSearchCriteria.builder()
                .query(query)
                .currentUser(currentUser)
                .personIds(personIds)
                .complaintState(complaintState)
                .personStatus(personStatus)
                .fromDate(fromDate)
                .toDate(toDate)
                .build();

        return personManager.findPersonsWithComplaints(authorizationHeader, criteria, page);
    }

    @ApiOperation(value = "Updates person information")
    @ApiResponses({
            @ApiResponse(code = 200, message = "If the person data successfully updated"),
            @ApiResponse(code = 404, message = "If person not found"),
            @ApiResponse(code = 400, message = "If invalid data provided"),
            @ApiResponse(code = 401, message = "If user is not authorized"),
            @ApiResponse(code = 500, message = "If any unexpected error occurred"),
    })
    @PutMapping("/{personId}")
    public JsonPersonProfile updatePerson(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable Long personId,
            @RequestParam(required = false) boolean onboarding,
            @RequestBody @Valid JsonPersonData payload) {
        LOGGER.debug("Received 'update person' request for person: {} with payload: {}",
                personId, payload);
        Long currentUserId = securityProvider.getPersonIdFromAuthorization(authorizationHeader);
        Person person = personManager.getPersonByIdInternal(currentUserId);
        if (person.isAdmin() || Objects.equals(person.getId(), personId)) {
            if (onboarding) {
                return mapper.map(personManager.onboardPerson(personId, payload), JsonPersonProfile.class);
            } else {
                return mapper.map(personManager.updatePerson(personId, person.isAdmin(), payload), JsonPersonProfile.class);
            }
        } else {
            throw new ForbiddenException("error.operation.not.permitted");
        }
    }

    @ApiOperation("Registers new person")
    @ApiResponses({
            @ApiResponse(code = 200, message = "If user registered successfully"),
            @ApiResponse(code = 400, message = "If invalid registration data provided"),
            @ApiResponse(code = 409, message = "If email already registered"),
            @ApiResponse(code = 500, message = "If any unexpected error occurred")
    })
    @PostMapping("/register")
    public JsonPersonProfile register(@RequestBody @Valid JsonRegistrationData data) {
        LOGGER.debug("Received 'register' request with data: {}", data.getEmail());
        if (StringUtils.equalsIgnoreCase(AuthMethod.CAPTCHA.name(), authProperties.getMethod())) {
            captchaService.processCaptchaResponse(data.getCaptchaResponse(), data.getSiteKey());
        }
        return mapper.map(authenticationManager.register(data), JsonPersonProfile.class);
    }

    @ApiOperation("Confirm email")
    @GetMapping("/email/confirm")
    public void confirmEmail(@RequestParam(name = "email") String email, @RequestParam(name = "code") String code) {
        LOGGER.debug("Received confirmation request for email: {}", email);

        authenticationManager.confirmEmail(email, code);
    }

    @ApiOperation("Resend confirmation email")
    @PostMapping("/email/resend")
    public void resendConfirmEmail(@RequestBody @Valid JsonResendEmailRequest data) {
        LOGGER.debug("Received 'email verification' request for email: {}", data.getEmail());

        if (StringUtils.equalsIgnoreCase(AuthMethod.CAPTCHA.name(), authProperties.getMethod())) {
            captchaService.processCaptchaResponse(data.getCaptchaResponse(), data.getSiteKey());
        }

        authenticationManager.resendConfirmationEmail(data.getEmail());
    }

    @ApiOperation("Authenticate the user")
    @ApiResponses({
            @ApiResponse(code = 200, message = "If user logged in successfully"),
            @ApiResponse(code = 404, message = "If invalid login provided"),
            @ApiResponse(code = 423, message = "If user account locked"),
            @ApiResponse(code = 500, message = "If any unexpected error occurred")
    })
    @PostMapping("/login")
    public ResponseEntity<JsonAuthentication> login(@RequestBody @Valid JsonLoginRequest data) {
        LOGGER.debug("Received 'login' request with data: {}", data.getEmail());

        return ResponseEntity.ok(authenticationManager.authenticateByEmail(data));
    }

    @ApiOperation("Authenticate the user with Facebook")
    @PostMapping("/login/facebook")
    public ResponseEntity<JsonAuthentication> facebook(@RequestBody @Valid JsonSSORequest data) {
        LOGGER.debug("Received 'Facebook login' request with token: {}", data.getAccessToken());

        return ResponseEntity.ok(authenticationManager.authenticateWithFacebook(data));
    }

    @ApiOperation("Authenticate the user with Google")
    @PostMapping("/login/google")
    public ResponseEntity<JsonAuthentication> google(@RequestBody @Valid JsonSSORequest data) {
        LOGGER.debug("Received 'Google login' request with token: {}", data.getAccessToken());

        return ResponseEntity.ok(authenticationManager.authenticateWithGoogle(data));
    }

    @ApiOperation("Authenticate the user with Apple")
    @PostMapping("/login/apple")
    public ResponseEntity<JsonAuthentication> apple(@RequestBody @Valid JsonSSORequest data) {
        LOGGER.debug("Received 'Facebook login' request with token: {}", data.getAccessToken());

        return ResponseEntity.ok(authenticationManager.authenticateWithApple(data));
    }

    @ApiOperation("Refresh the expired jwt authentication")
    @GetMapping("/token/refresh")
    public ResponseEntity<Map<String, Object>> refreshToken(@RequestHeader("Authorization") String authorizationHeader) {
        if (authorizationHeader != null && authorizationHeader.startsWith(BEARER)) {
            return ResponseEntity.ok(authenticationManager.refreshToken(authorizationHeader.substring(7)));
        } else {
            throw new BadRequestException("error.refresh.token.is.missing");
        }
    }

    @ApiOperation(value = "Returns the current authenticated user profile", response = JsonPerson.class)
    @GetMapping("/profile")
    public ResponseEntity<JsonPersonProfile> getProfile(@RequestHeader("Authorization") String authorizationHeader) {
        Long personId = securityProvider.getPersonIdFromAuthorization(authorizationHeader);
        Person person = personManager.getPersonByIdInternal(personId);
        return ResponseEntity.status(HttpStatus.OK)
                .body(mapper.map(person, JsonPersonProfile.class));
    }

    @ApiOperation(value = "Check if username already exist")
    @ApiResponses({
            @ApiResponse(code = 200, message = "If username is not taken and valid"),
            @ApiResponse(code = 400, message = "If username is empty"),
            @ApiResponse(code = 409, message = "If username is already registered"),
            @ApiResponse(code = 500, message = "If any unexpected error occurred")
    })
    @GetMapping("/check-username")
    public void checkUsername(@RequestParam @Pattern(regexp = RegistrationData.USERNAME_REGEX, message = RegistrationData.USERNAME_PATTERN_ERROR)
                              @Size(min = 3, max = 25) String username) {
        personManager.checkUsername(username);
    }

    @ApiOperation("Changes the current user password")
    @ApiResponses({
            @ApiResponse(code = 200, message = "If password successfully changed"),
            @ApiResponse(code = 400, message = "If invalid data provided"),
            @ApiResponse(code = 401, message = "If user is not authenticated"),
            @ApiResponse(code = 500, message = "If any unexpected error occurred")
    })
    @PostMapping("/change-password")
    public void changePassword(
            @RequestHeader("Authorization") String authorizationHeader,
            @RequestBody @Valid JsonChangePasswordRequest request
    ) {
        Long personId = securityProvider.getPersonIdFromAuthorization(authorizationHeader);
        Person person = personManager.getPersonByIdInternal(personId);
        authenticationManager.changePassword(person, request);
    }

    @PostMapping(value = "/forgot-password")
    @ApiOperation("Forgot password (sends mail with verification code)")
    public void forgotPassword(@RequestBody @Valid JsonForgotPasswordRequest request
    ) {
        if (StringUtils.equalsIgnoreCase(AuthMethod.CAPTCHA.name(), authProperties.getMethod())) {
            captchaService.processCaptchaResponse(request.getCaptchaResponse(), request.getSiteKey());
        }
        authenticationManager.forgotPassword(request);
    }

    @PostMapping(value = "/reset-password")
    @ApiOperation("Reset Forgot password.")
    public void resetPassword(
            @RequestBody @Valid JsonResetPasswordRequest request
    ) {
        authenticationManager.resetPassword(request);
    }

    @PostMapping(value = "/invite-friend")
    @ApiOperation("Invite friend (sends invitation email with link to the application)")
    public void inviteFriend(
            @RequestHeader("Authorization") String authorizationHeader,
            @RequestBody @Valid JsonInviteFriendRequest request) {
        if (StringUtils.equalsIgnoreCase(AuthMethod.CAPTCHA.name(), authProperties.getMethod())) {
            captchaService.processCaptchaResponse(request.getCaptchaResponse(), request.getSiteKey());
        }

        Long personId = securityProvider.getPersonIdFromAuthorization(authorizationHeader);
        Person person = personManager.getPersonByIdInternal(personId);
        personManager.inviteFriend(person, request);
    }

    @ApiOperation("Create request to change email")
    @PutMapping("/email/request")
    public void changeEmailRequest(@RequestBody @Valid JsonUpdateEmailRequest data,
                                   @RequestHeader("Authorization") String authorizationHeader) {
        LOGGER.debug("Received request to change email");
        Long personId = securityProvider.getPersonIdFromAuthorization(authorizationHeader);
        Person person = personManager.getPersonByIdInternal(personId);
        if (StringUtils.equalsIgnoreCase(AuthMethod.CAPTCHA.name(), authProperties.getMethod())) {
            captchaService.processCaptchaResponse(data.getCaptchaResponse(), data.getSiteKey());
        }
        personManager.changeEmailRequest(person, data.getNewEmail());
    }

    @ApiOperation("Update email")
    @PutMapping("/email/update")
    public void updateEmail(@RequestBody @Valid JsonUpdateEmailRequest data) {
        LOGGER.debug("Received 'update email' request from '{}' to '{}'", data.getOldEmail(), data.getNewEmail());

        personManager.updateEmail(data);
    }

    @GetMapping("/logout")
    public void logout(
            @ApiParam(hidden = true) @RequestHeader("Authorization") String authorization) {
        if (!Objects.isNull(authorization) && authorization.startsWith(BEARER)) {
            String token = authorization.substring(7);
            securityProvider.blockToken(token);
        }
    }

    @DeleteMapping
    @ApiOperation("Deactivate User Profile.")
    public void deactivateProfile(
            @ApiParam(hidden = true) @RequestHeader("Authorization") String authorization,
            @RequestParam(name = "force", required = false) String force) {
        if (!Objects.isNull(authorization) && authorization.startsWith(BEARER)) {
            String token = authorization.substring(7);

            Long personId = securityProvider.getPersonIdFromAuthorization(authorization);
            Person person = personManager.getPersonByIdInternal(personId);

            securityProvider.blockToken(token);
            personManager.deactivateProfile(person, force);
        }
    }
}
