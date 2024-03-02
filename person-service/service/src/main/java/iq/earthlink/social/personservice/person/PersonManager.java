package iq.earthlink.social.personservice.person;

import iq.earthlink.social.classes.enumeration.TimeInterval;
import iq.earthlink.social.personservice.dto.JsonPersonReported;
import iq.earthlink.social.personservice.dto.PersonDTO;
import iq.earthlink.social.personservice.exception.PersonNotFoundException;
import iq.earthlink.social.personservice.person.model.Person;
import iq.earthlink.social.personservice.person.rest.*;
import lombok.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

/**
 * The interface provides methods for managing {@link Person} information.
 */
public interface PersonManager {

    JsonPerson getPersonById(@Nonnull Long currentUserId, @Nonnull Long personId) throws PersonNotFoundException;

    /**
     * Returns person found by id.
     *
     * @param personId the person id
     * @throws PersonNotFoundException if person is not found
     */
    @Nonnull
    Person getPersonByIdInternal(@Nonnull Long personId) throws PersonNotFoundException;

    /**
     * Returns person found by username.
     *
     * @param username the person username
     * @throws PersonNotFoundException if person is not found
     */
    @Nonnull
    Person getPersonByUsername(@Nonnull String username) throws PersonNotFoundException;

    /**
     * Returns person found by uuid.
     *
     * @param uuid the person uuid
     * @throws PersonNotFoundException if person is not found
     */
    @Nonnull
    Person getPersonByUuid(@Nonnull UUID uuid) throws PersonNotFoundException;

    @Transactional
    @Nonnull
    PersonDTO getPersonDtoByUuid(@Nonnull UUID uuid) throws PersonNotFoundException;

    /**
     * Returns persons collection limited with pagination params.
     *
     * @param page the pagination params
     */
    @Nonnull
    Page<Person> findPersons(@Nonnull PersonSearchCriteria criteria, @Nonnull Pageable page);

    Page<Person> findPersonsInGroups(@Nonnull String authorizationHeader, @Nonnull PersonSearchCriteria criteria,
                                     @Nonnull List<Long> groupIds, @Nonnull Pageable page);

    /**
     * Returns reported persons limited with pagination params.
     *
     * @param page the pagination params
     */
    @Nonnull
    Page<JsonPersonReported> findPersonsWithComplaints(@Nonnull String authorizationHeader, @Nonnull PersonComplaintSearchCriteria criteria, @Nonnull Pageable page);

    /**
     * Updates person information.
     *
     * @param personId   the person identifier
     * @param updateData the person update data
     * @throws PersonNotFoundException if person not found by id
     */
    @Nonnull
    Person updatePerson(@Nonnull Long personId, boolean isAdminUpdating, @Nonnull PersonData updateData)
            throws PersonNotFoundException;

    /**
     * Updates person state (for onboarding).
     *
     * @param personId   the person identifier
     * @param updateData the person update data
     * @throws PersonNotFoundException if person not found by id
     */
    @Nonnull
    Person onboardPerson(@Nonnull Long personId, @Nonnull PersonData updateData)
            throws PersonNotFoundException;

    /**
     * Returns found person by matched email, otherwise null.
     *
     * @param email the searched email
     */
    @Nullable
    Person findByEmail(@Nullable String email);

    /**
     * Checks if username exists.
     *
     * @param username the searched username
     */
    void checkUsername(@Nullable String username);

    void inviteFriend(@Nonnull Person person, JsonInviteFriendRequest request);

    /**
     * De-activates the person's profile.
     *
     * @param person current user
     */
    void deactivateProfile(@Nonnull Person person, String force);

    /**
     * Returns user statistics.
     *
     * @param fromDate,     String
     * @param timeInterval, enum - Time interval for which statistical results are calculated: DAY, MONTH, or YEAR. Default value - MONTH.
     */
    UserStats getUserStats(String fromDate, TimeInterval timeInterval);

    JsonPersonProfile getPersonByPersonIdInternal(Long personId);

    Page<JsonPerson> findPersons(@Nonnull String authorizationHeader, @Nonnull PersonSearchCriteria criteria,
                                 List<Long> groupIds, @Nonnull Pageable page);

    Page<JsonPersonProfile> findPersonsByAdmin(@NonNull String authorizationHeader, @NonNull PersonSearchCriteria criteria,
                                               List<Long> groupIds, @NonNull Pageable page);

    void changeEmailRequest(@Nonnull Person person, @Nonnull String email);

    void updateEmail(@Nonnull JsonUpdateEmailRequest data);
}
