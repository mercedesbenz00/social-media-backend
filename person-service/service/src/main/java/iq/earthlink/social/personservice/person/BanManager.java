package iq.earthlink.social.personservice.person;

import iq.earthlink.social.personservice.person.model.Person;
import iq.earthlink.social.personservice.person.model.PersonBan;
import iq.earthlink.social.personservice.person.model.PersonGroupBan;
import iq.earthlink.social.personservice.person.rest.JsonPersonBanRequest;
import lombok.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import javax.annotation.Nonnull;
import java.util.List;

public interface BanManager {

    /**
     * Creates new ban record.
     *
     * @param currentUser  the user who is issuing the ban
     * @param data person ban data - banned person, reason, period in days
     */
    @Nonnull
    PersonBan createBan(
            @Nonnull String authorizationHeader,
            @Nonnull Person currentUser,
            @Nonnull PersonBanData data);

    /**
     * Creates new group ban record.
     *
     * @param currentUser  the user who is issuing the group ban
     * @param data person ban data - banned person, reason, period in days, groupIds
     */
    @Nonnull
    List<PersonGroupBan> createGroupBans(
            String authorizationHeader,
            @Nonnull Person currentUser,
            @Nonnull PersonBanData data);

    /**
     * Removes person ban record.
     *
     * @param currentUser the person removing the ban
     * @param banId       the ban ID to remove
     */
    void removeBan(String authorizationHeader, @Nonnull Person currentUser, @Nonnull Long banId);

    /**
     * Removes group ban record.
     *
     * @param currentUser the person removing the group ban
     * @param groupBanId  the group ban ID to remove
     */
    void removeGroupBan(String authorizationHeader, @Nonnull Person currentUser, @Nonnull Long groupBanId);

    /**
     * Finds bans created by the owner person.
     *
     * @param criteria for searching a person by text
     * @param page     the pagination params
     */
    Page<PersonBan> findBans(@Nonnull BanSearchCriteria criteria, @Nonnull Person currentUser, @Nonnull Pageable page);

    /**
     * Finds group bans.
     *
     * @param criteria for searching a person by text, person ID, or group ID
     * @param page     the pagination params
     */
    Page<PersonGroupBan> findGroupBans(String authorizationHeader, @Nonnull BanSearchCriteria criteria,
                                       @Nonnull Person currentUser, @Nonnull Pageable page);

    /**
     * Updates the ban record.
     *
     * @param banId       the id of the ban
     * @param currentUser the person requesting the update
     * @param request     the data of the updated ban
     */
    @Nonnull
    PersonBan updateBan(
            String authorizationHeader,
            @Nonnull Long banId,
            @Nonnull Person currentUser,
            @Nonnull JsonPersonBanRequest request);

    /**
     * Updates the group ban record.
     *
     * @param banId       the id of the ban
     * @param currentUser the person requesting the update
     * @param request     the data of the updated ban
     */
    @Nonnull
    PersonGroupBan updateGroupBan(
            String authorizationHeader,
            @Nonnull Long banId,
            @Nonnull Person currentUser,
            @Nonnull JsonPersonBanRequest request);

    /**
     * Finds person active bans.
     *
     * @param personId the id of the person
     */
    @Nonnull
    List<PersonBan> getActiveBans(@NonNull Long personId);

    /**
     * Finds person active group bans.
     *
     * @param personId the id of the person
     * @param groupId the id of the user group
     */
    @Nonnull
    List<PersonGroupBan> getActiveGroupBans(Long personId, Long groupId);

    /**
     * Creates new group ban record by complaint.
     *  @param authorizationHeader authorization header, String
     * @param currentUser  the user who is issuing the ban
     * @param reason       the owner's comment why this ban applied
     * @param complaintId  complaint ID
     * @param periodInDays the number of days the ban should be active
     * @param resolveAll indicator to update all PENDING person complaints in group with state 'USER_BANNED_GROUP'
     */
    @Nonnull
    PersonGroupBan banPersonInGroupByComplaint(String authorizationHeader, Person currentUser, String reason, Long complaintId,
                                               Integer periodInDays, Boolean resolveAll);
}

