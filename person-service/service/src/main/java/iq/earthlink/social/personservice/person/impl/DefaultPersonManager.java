package iq.earthlink.social.personservice.person.impl;

import iq.earthlink.social.classes.data.dto.JsonMediaFile;
import iq.earthlink.social.classes.data.dto.JsonSizedImage;
import iq.earthlink.social.classes.data.event.EmailEvent;
import iq.earthlink.social.classes.enumeration.*;
import iq.earthlink.social.common.data.event.GroupMemberActivityEvent;
import iq.earthlink.social.common.file.SizedImage;
import iq.earthlink.social.common.util.CommonConstants;
import iq.earthlink.social.common.util.CommonUtil;
import iq.earthlink.social.common.util.DateUtil;
import iq.earthlink.social.exception.*;
import iq.earthlink.social.groupservice.group.rest.MembersRestService;
import iq.earthlink.social.personservice.dto.JsonPersonReported;
import iq.earthlink.social.personservice.dto.PersonDTO;
import iq.earthlink.social.personservice.exception.PersonNotFoundException;
import iq.earthlink.social.personservice.outbox.service.EmailOutboxService;
import iq.earthlink.social.personservice.person.*;
import iq.earthlink.social.personservice.person.impl.repository.ChangeEmailRequestRepository;
import iq.earthlink.social.personservice.person.impl.repository.ComplaintRepository;
import iq.earthlink.social.personservice.person.impl.repository.FollowRepository;
import iq.earthlink.social.personservice.person.impl.repository.PersonRepository;
import iq.earthlink.social.personservice.person.model.ChangeEmailRequest;
import iq.earthlink.social.personservice.person.model.Person;
import iq.earthlink.social.personservice.person.rest.*;
import iq.earthlink.social.personservice.service.ChatAdministrationService;
import iq.earthlink.social.personservice.service.KafkaProducerService;
import iq.earthlink.social.personservice.util.CommonProperties;
import iq.earthlink.social.personservice.util.EnvironmentUtil;
import iq.earthlink.social.personservice.util.ProfileUtil;
import iq.earthlink.social.postservice.post.rest.ComplaintStatsDTO;
import iq.earthlink.social.postservice.rest.PostRestService;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.dozer.Mapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.UUID;
import java.util.*;

import static iq.earthlink.social.common.util.CommonConstants.ERROR_CHECK_NOT_NULL;
import static iq.earthlink.social.common.util.Preconditions.checkNotNull;
import static iq.earthlink.social.personservice.util.Constants.SPACE;
import static iq.earthlink.social.personservice.util.Constants.*;
import static java.util.stream.Collectors.groupingBy;
import static org.apache.commons.lang3.ObjectUtils.firstNonNull;
import static org.apache.commons.lang3.StringUtils.*;

/**
 * The default implementation of {@link PersonManager}.
 */
@Service("personManager")
@RequiredArgsConstructor
@Validated
public class DefaultPersonManager implements PersonManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultPersonManager.class);
    private final PostRestService postRestService;
    private final PersonRepository personRepository;
    private final ChangeEmailRequestRepository changeEmailRequestRepository;
    private final FollowRepository followRepository;
    private final ComplaintRepository complaintRepository;
    private final ChatAdministrationService chatAdministrationService;
    private final RabbitTemplate rabbitTemplate;
    private final EnvironmentUtil environmentUtil;
    private final ProfileUtil profileUtil;
    private final CommonProperties commonProperties;
    private final MembersRestService membersRestService;
    private final Mapper mapper;
    private final KafkaProducerService kafkaProducerService;
    private final RedisTemplate<String, Long> migrationFlag;
    private final EmailOutboxService emailOutboxService;

    @Transactional
    @Nonnull
    @Override
    public JsonPerson getPersonById(@Nonnull Long currentUserId, @Nonnull Long personId) {
        var person = mapper.map(getPersonByIdInternal(personId), JsonPerson.class);
        var followersList = followRepository.findFollowers(FollowSearchCriteria
                .builder()
                .personId(personId)
                .followerIds(new Long[]{currentUserId})
                .build(), Pageable.unpaged());
        if (followersList.hasContent()) {
            person.setFollowing(true);
        }
        return person;
    }

    @Transactional
    @Nonnull
    @Override
    public Person getPersonByIdInternal(@Nonnull Long personId) throws PersonNotFoundException {
        LOGGER.trace("Getting person with id: {}", personId);

        return personRepository.findById(personId)
                .orElseThrow(() -> new NotFoundException("error.person.not.found.byId", personId));

    }

    @Transactional
    @Nonnull
    @Override
    public Person getPersonByUsername(@Nonnull String username) throws PersonNotFoundException {
        LOGGER.trace("Getting person with id: {}", username);

        return personRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new NotFoundException("error.person.not.found.by.username", username));
    }

    @Transactional
    @Nonnull
    @Override
    public Person getPersonByUuid(@Nonnull UUID uuid) throws PersonNotFoundException {
        LOGGER.trace("Getting person with uuid: {}", uuid);

        return personRepository.findByUuid(uuid)
                .orElseThrow(() -> new NotFoundException("error.person.not.found.by.uuid", uuid));
    }

    @Transactional
    @Nonnull
    @Override
    public PersonDTO getPersonDtoByUuid(@Nonnull UUID uuid) throws PersonNotFoundException {
        LOGGER.trace("Getting personDTO with uuid: {}", uuid);

        Person person = personRepository.findByUuid(uuid)
                .orElseThrow(() -> new NotFoundException("error.person.not.found.by.uuid", uuid));
        return mapper.map(person, PersonDTO.class);
    }

    @Nonnull
    @Override
    public Page<Person> findPersons(@Nonnull PersonSearchCriteria criteria, @Nonnull Pageable page) {
        checkNotNull(criteria, ERROR_CHECK_NOT_NULL, "criteria");
        checkNotNull(page, ERROR_CHECK_NOT_NULL, "page");

        LOGGER.trace("Getting persons with pagination: {}", page);

        updateCriteria(criteria);

        if (criteria.isFollowingsFirst()) {
            // Current person will be excluded from the result:
            return personRepository.findPersonsWithFollowingsFirst(criteria, followRepository.getFollowerIds(criteria.getCurrentPersonId()),
                    followRepository.getSubscribedToIds(criteria.getCurrentPersonId()), page);
        } else {
            return page.getSort().isSorted() ? personRepository.findPersons(criteria, page)
                    : personRepository.findPersonsOrderedBySimilarity(criteria, page);
        }
    }

    @Override
    public Page<Person> findPersonsInGroups(@Nonnull String authorizationHeader, @NonNull PersonSearchCriteria criteria,
                                            @NonNull List<Long> groupIds, @NonNull Pageable page) {

        Set<Long> groupMembers = membersRestService.getAllMembersInGroups(authorizationHeader, groupIds);

        if (CollectionUtils.isNotEmpty(groupMembers)) {
            criteria.setPersonIds(groupMembers.toArray(Long[]::new));
            return findPersons(criteria, page);
        }

        return Page.empty(page);
    }

    @NonNull
    @Override
    @Valid
    public Page<JsonPersonReported> findPersonsWithComplaints(
            @Nonnull String authorizationHeader,
            @Nonnull PersonComplaintSearchCriteria criteria,
            @Nonnull Pageable page) {
        checkNotNull(authorizationHeader, ERROR_CHECK_NOT_NULL, "authorizationHeader");
        checkNotNull(criteria.getCurrentUser(), ERROR_CHECK_NOT_NULL, "currentUser");
        checkNotNull(criteria.getComplaintState(), ERROR_CHECK_NOT_NULL, "complaintState");
        checkNotNull(criteria.getPersonStatus(), ERROR_CHECK_NOT_NULL, "personStatus");

        if (!criteria.getCurrentUser().isAdmin()) {
            throw new ForbiddenException("error.operation.not.permitted");
        }

        if (Objects.isNull(criteria.getToDate())) {
            criteria.setToDate(LocalDate.now());
        }
        if (Objects.isNull(criteria.getFromDate()) || criteria.getFromDate().isAfter(criteria.getToDate())) {
            criteria.setFromDate(criteria.getToDate().minusDays(30));
        }

        updateCriteria(criteria);

        return complaintRepository.findPersonsWithComplaints(criteria, page)
                .map(person -> {
                    var reportedPerson = mapper.map(person, JsonPersonReported.class);
                    var personComplaintStats = complaintRepository.getPersonComplainStats(person.getId(), criteria.getComplaintState());

                    if (Objects.nonNull(personComplaintStats)) {
                        reportedPerson.setComplaintCount(personComplaintStats.getComplaintCount());
                        reportedPerson.setLastComplaintDate(personComplaintStats.getLastComplaintDate());
                    }

                    try {
                        ComplaintStatsDTO postAndCommentComplaintStats = postRestService.getPersonComplaintStats(authorizationHeader, person.getId());
                        reportedPerson.setCommentComplaintCount(postAndCommentComplaintStats.getCommentComplaintsCount());
                        reportedPerson.setPostComplaintCount(postAndCommentComplaintStats.getPostComplaintsCount());
                    } catch (Exception e) {
                        LOGGER.error("Failed to fetch post and comments complaints count for the person with id: {}", person.getId());
                    }

                    return reportedPerson;
                });
    }

    @Transactional
    @Nonnull
    @Override
    public Person updatePerson(@Nonnull Long personId, boolean isAdminUpdating, @Nonnull PersonData updateData)
            throws PersonNotFoundException {
        LOGGER.info("Updating person: {} with new data: {}", personId, updateData);

        Person person = updatePersonInfo(personId, updateData);

        if (Objects.nonNull(updateData.getIsVerifiedAccount())) {
            if (isAdminUpdating)
                person.setVerifiedAccount(updateData.getIsVerifiedAccount());
            else
                throw new ForbiddenException("error.operation.not.permitted");
        }

        try {
            person = personRepository.saveAndFlush(person);
        } catch (DataIntegrityViolationException ex) {
            throw new NotUniqueException("error.person.username.is.not.unique");
        }

        if (isNotEmpty(person.getDisplayName()) && rabbitTemplate != null) {
            GroupMemberActivityEvent.send(rabbitTemplate, new GroupMemberActivityEvent(personId, person.getDisplayName(),
                    GroupEventType.MEMBER_DISPLAY_NAME_UPDATED.name()));
        }

        kafkaProducerService.sendMessageOnFailureThrowError(CommonConstants.PERSON_UPDATED, getJsonPersonProfileFromEntity(person));
        return person;
    }

    @Transactional
    @Nonnull
    @Override
    public Person onboardPerson(@Nonnull Long personId, @Nonnull PersonData updateData) throws PersonNotFoundException {
        Person person = updatePersonInfo(personId, updateData);
        profileUtil.generateDisplayName(person);
        if (profileUtil.isPersonInfoProvided(person) && person.getState().canBeChangedTo(RegistrationState.INFO_PROVIDED)) {
            person.setRegistrationCompleted(true);
            person.setState(RegistrationState.INFO_PROVIDED);
            kafkaProducerService.sendMessageOnFailureThrowError(CommonConstants.PERSON_CREATED, getJsonPersonProfileFromEntity(person));
        }
        LOGGER.info("Updating person: {} with new state: {}", personId, person.getState().getDisplayName());

        try {
            person = personRepository.saveAndFlush(person);
        } catch (DataIntegrityViolationException ex) {
            throw new NotUniqueException("error.person.username.is.not.unique");
        }
        return person;
    }

    @Nullable
    @Override
    public Person findByEmail(@Nullable String email) {
        if (isEmpty(email)) {
            return null;
        }

        return personRepository.findByEmailIgnoreCase(email);
    }

    @Override
    public void checkUsername(@Nullable String username) {
        if (isBlank(username)) {
            throw new BadRequestException("error.person.username.is.empty");
        }

        if (personRepository.findByUsernameIgnoreCase(username).isPresent()) {
            throw new NotUniqueException("error.person.username.already.exists");
        }
    }

    @Override
    public void inviteFriend(@Nonnull Person person, JsonInviteFriendRequest request) {
        Map<String, Object> emailTemplateModel = new HashMap<>();
        // Invitee name:
        emailTemplateModel.put(NAME, request.getName());
        // Inviter email:
        emailTemplateModel.put(EMAIL, person.getEmail());
        // Inviter full name:
        emailTemplateModel.put(INVITER, joinWith(SPACE, person.getFirstName(), person.getLastName()));

        // Link to the application:
        emailTemplateModel.put(URL, commonProperties.getWebUrl());
        emailOutboxService.sendEmail(EmailEvent.builder()
                .recipientEmail(request.getEmail())
                .emailType(EmailType.INVITE_FRIEND)
                .templateModel(emailTemplateModel)
                .build());
    }

    @Transactional
    @Override
    public void deactivateProfile(@Nonnull Person person, String force) {
        Person personModel = getPersonByIdInternal(person.getId());
        if (force != null && !environmentUtil.isProduction()) {
            removeUser(person);
        } else {
            personModel.setDeletedDate(new Date());
            personRepository.save(personModel);
            chatAdministrationService.deactivateUser(personModel.getId());
        }
    }

    @Transactional
    @Scheduled(cron = "${social.job.cleanup.cron}")
    public void clearInactiveProfiles() {
        LOGGER.info("Running a scheduled job to remove inactive profiles");
        Date clearBeforeDate = DateUtils.addDays(new Date(), -1 * commonProperties.getDeleteAfterDays());
        Iterable<Person> persons = personRepository.findInactiveProfiles(clearBeforeDate);
        for (Person person : persons) {
            removeUser(person);
        }
    }

    @Transactional
    @Scheduled(cron = "${social.job.syncFollowerStats}")
    public void syncFollowersStat() {
        personRepository.syncPersonsFollowingCount();
        personRepository.syncPersonsFollowersCount();
    }

    //todo: remove after deploying to prod env
    @Transactional
    @Scheduled(cron = "${social.job.migrateUsers}")
    public void pushAllUsers() {
        LOGGER.info("Running scheduler to push all users to kafka topic");
        try {
            Long flag = migrationFlag.opsForValue().get("personsMigrationFlag");
            if (Objects.isNull(flag) || flag == 0) {
                var persons = getAllUsers();
                if (!org.springframework.util.CollectionUtils.isEmpty(persons)) {
                    persons.forEach(person -> kafkaProducerService.sendMessageOnFailureThrowError(CommonConstants.PERSON_CREATED, person));
                }
                migrationFlag.opsForValue().set("personsMigrationFlag", 1L);
            }
        } catch (Exception ex) {
            LOGGER.error("Error while pushing all users to kafka: {}", ex.getMessage());
        }
    }

    @Transactional
    public void removeUser(@NotNull Person person) {
        LOGGER.info("Permanently removing the user with email: {}", person.getEmail());
        person.setFirstName(null);
        person.setLastName(null);
        person.setEmail(null);
        person.setBirthDate(null);
        person.setGender(null);
        person.setDisplayName(commonProperties.getDefaultDisplayName());
        person.setUsername(null);
        rabbitTemplate.convertAndSend(PERSON_DELETE_EVENT, "", person.getId());
        kafkaProducerService.sendMessageOnFailureThrowError(CommonConstants.PERSON_UPDATED, getJsonPersonProfileFromEntity(person));
        personRepository.save(person);
    }

    @Override
    public UserStats getUserStats(String fromDate, TimeInterval timeInterval) {
        if (timeInterval == null) {
            timeInterval = TimeInterval.MONTH;
        }

        Timestamp timestamp = StringUtils.isEmpty(fromDate) ? null : Timestamp.valueOf(DateUtil.getDateFromString(fromDate).atStartOfDay());
        long allUsersCount = personRepository.getAllUsersCount();
        long newUsersCount = personRepository.getNewUsersCount(timestamp);

        UserStats stats = UserStats.builder()
                .allUsersCount(allUsersCount)
                .newUsersCount(newUsersCount)
                .fromDate(timestamp)
                .timeInterval(timeInterval)
                .build();

        switch (timeInterval) {
            case DAY -> {
                List<ActivatedUsers> activatedUsersPerDay = personRepository.getActivatedUsersPerDay(timestamp);
                stats.setActivatedUsers(activatedUsersPerDay);
            }
            case YEAR -> {
                List<ActivatedUsers> activatedUsersPerYear = personRepository.getActivatedUsersPerYear(timestamp);
                stats.setActivatedUsers(activatedUsersPerYear);
            }
            default -> {
                List<ActivatedUsers> activatedUsersPerMonth = personRepository.getActivatedUsersPerMonth(timestamp);
                stats.setActivatedUsers(activatedUsersPerMonth);
            }
        }
        return stats;
    }

    private List<JsonPersonProfile> getAllUsers() {
        return personRepository.findAll().stream().map(this::getJsonPersonProfileFromEntity).toList();
    }

    @Transactional
    public JsonPersonProfile getPersonByPersonIdInternal(Long personId) {
        return getJsonPersonProfileFromEntity(getPersonByIdInternal(personId));
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

    @Override
    public Page<JsonPerson> findPersons(@NonNull String authorizationHeader, @NonNull PersonSearchCriteria criteria,
                                        List<Long> groupIds, @NonNull Pageable page) {
        return CollectionUtils.isNotEmpty(groupIds) ?
                findPersonsInGroups(authorizationHeader, criteria, groupIds, page).map(p -> mapper.map(p, JsonPerson.class))
                : findPersons(criteria, page).map(p -> mapper.map(p, JsonPerson.class));
    }

    @Override
    public Page<JsonPersonProfile> findPersonsByAdmin(@NonNull String authorizationHeader, @NonNull PersonSearchCriteria criteria,
                                                      List<Long> groupIds, @NonNull Pageable page) {
        return CollectionUtils.isNotEmpty(groupIds) ?
                findPersonsInGroups(authorizationHeader, criteria, groupIds, page).map(p -> mapper.map(p, JsonPersonProfile.class))
                : findPersons(criteria, page).map(p -> mapper.map(p, JsonPersonProfile.class));
    }

    @Transactional
    @Override
    public void changeEmailRequest(@NonNull Person person, @NonNull String email) {
        //Check if requested email exists:
        Person personWithRequestedEmail = personRepository.findByEmailIgnoreCase(email);

        if (personWithRequestedEmail == null) {
            // Requested email is available - create record in database and send email:
            Date createdAt = new Date();
            Date expiresAt = DateUtils.addHours(createdAt, commonProperties.getChangeEmailRequestExpirationInHours());

            ChangeEmailRequest request = ChangeEmailRequest.builder()
                    .person(person)
                    .newEmail(email)
                    .oldEmail(person.getEmail())
                    .token(CommonUtil.getRandomNumberString())
                    .state(RequestState.ACTIVE)
                    .expiresAt(expiresAt)
                    .createdAt(createdAt)
                    .build();

            changeEmailRequestRepository.save(request);

            // Send email to the new email address:
            Map<String, Object> emailTemplateModel = new HashMap<>();
            String updateEmailURL = CommonUtil.generateURL(commonProperties.getWebUrl(), "/email/update",
                    Map.of(TOKEN, request.getToken(), NEW_EMAIL, email, OLD_EMAIL, person.getEmail()));
            emailTemplateModel.put(NAME, person.getFirstLastName());
            emailTemplateModel.put(EMAIL, email);
            emailTemplateModel.put("updateEmailURL", updateEmailURL);
            emailOutboxService.sendEmail(EmailEvent.builder()
                    .recipientEmail(email)
                    .emailType(EmailType.CHANGE_EMAIL_REQUEST)
                    .templateModel(emailTemplateModel)
                    .build());
        } else {
            LOGGER.warn("Requested email {} is already taken", email);
        }
    }

    @Transactional
    @Override
    public void updateEmail(@NonNull JsonUpdateEmailRequest data) {
        checkNotNull(data.getNewEmail(), ERROR_CHECK_NOT_NULL, "newEmail");
        checkNotNull(data.getOldEmail(), ERROR_CHECK_NOT_NULL, "oldEmail");
        checkNotNull(data.getToken(), ERROR_CHECK_NOT_NULL, "token");

        if (isBlank(data.getOldEmail()) || isBlank(data.getNewEmail())) {
            throw new BadRequestException("error.email.not.blank");
        }

        Person person = personRepository.findByEmailIgnoreCase(data.getOldEmail());
        if (person == null) {
            throw new NotFoundException("error.user.with.email.not.found");
        }

        List<ChangeEmailRequest> requests = changeEmailRequestRepository.findByPersonAndNewEmailAndTokenAndState
                (
                        person,
                        data.getNewEmail(),
                        data.getToken(),
                        RequestState.ACTIVE
                );

        if (CollectionUtils.isEmpty(requests)) {
            throw new ForbiddenException("error.change.email.request.expired.or.not.exist");
        }

        try {
            Optional<ChangeEmailRequest> request = requests.stream().min((e1, e2) -> e2.getCreatedAt().compareTo(e1.getCreatedAt()));

            if (request.isPresent()) {
                ChangeEmailRequest requestToUpdate = request.get();

                if (requestToUpdate.getExpiresAt().before(new Date())) {
                    LOGGER.warn("Cannot update email - change email request is expired.");
                    requestToUpdate.setState(RequestState.EXPIRED);
                } else {
                    // Update person:
                    person.setEmail(requestToUpdate.getNewEmail());
                    personRepository.save(person);

                    // Send email to the old email address:
                    String oldEmail = data.getOldEmail();
                    Map<String, Object> emailTemplateModel = new HashMap<>();
                    emailTemplateModel.put(NAME, person.getFirstLastName());
                    emailTemplateModel.put(EMAIL, data.getNewEmail());
                    emailOutboxService.sendEmail(EmailEvent.builder()
                            .recipientEmail(oldEmail)
                            .emailType(EmailType.UPDATE_EMAIL)
                            .templateModel(emailTemplateModel)
                            .build());

                    requestToUpdate.setState(RequestState.PROCESSED);
                }
                // Update request:
                changeEmailRequestRepository.saveAndFlush(requestToUpdate);
            }
        } catch (DataIntegrityViolationException ex) {
            throw new NotUniqueException("error.person.email.is.not.unique");
        } catch (Exception exception) {
            throw new RestApiException(HttpStatus.INTERNAL_SERVER_ERROR, "error.update.person", exception.getMessage());
        }
    }

    /**
     * Updates person search criteria to set patterns to be used in find persons query
     */
    private void updateCriteria(PersonSearchCriteria criteria) {

        // First name, last name, display name, or full name pattern from 'query' parameter:
        String query = criteria.getQuery();
        if (Objects.nonNull(query)) {
            criteria.setQuery("%" + query.toLowerCase().trim().replaceAll("\\s+", "%") + "%");
        } else {
            criteria.setQuery("%");
        }

        // Display name pattern from 'displayNameQuery' parameter:
        String displayNameQuery = criteria.getDisplayNameQuery();
        if (Objects.nonNull(displayNameQuery)) {
            criteria.setDisplayNameQuery("%" + displayNameQuery.toLowerCase().trim().replaceAll("\\s+", "%") + "%");
        } else {
            criteria.setDisplayNameQuery("%");
        }
    }

    private void updateCriteria(PersonComplaintSearchCriteria criteria) {
        String query = criteria.getQuery();
        if (Objects.nonNull(query)) {
            criteria.setQuery("%" + query.toLowerCase().trim().replaceAll("\\s+", "%") + "%");
        } else {
            criteria.setQuery("%");
        }
    }

    @NonNull
    private Person updatePersonInfo(@NonNull Long personId, @NonNull PersonData updateData) {
        String displayName = normalizeSpace(updateData.getDisplayName());
        profileUtil.checkDisplayName(displayName);
        profileUtil.checkCity(updateData.getCityId());
        profileUtil.checkAge(updateData.getBirthDate());
        profileUtil.checkBio(updateData.getBio());

        Person person = getPersonByIdInternal(personId);

        person.setFirstName(firstNonNull(normalizeSpace(updateData.getFirstName()), person.getFirstName()));
        person.setLastName(firstNonNull(normalizeSpace(updateData.getLastName()), person.getLastName()));
        person.setGender(firstNonNull(updateData.getGender(), person.getGender()));
        person.setBirthDate(firstNonNull(updateData.getBirthDate(), person.getBirthDate()));
        person.setDisplayName(firstNonNull(displayName, person.getDisplayName()));
        person.setCityId(firstNonNull(updateData.getCityId(), person.getCityId()));
        person.setBio(firstNonNull(updateData.getBio(), person.getBio()));

        if (isNotEmpty(updateData.getUsername())) {
            person.setUsername(updateData.getUsername());
        }
        return person;
    }
}
