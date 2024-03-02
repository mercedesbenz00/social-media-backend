package iq.earthlink.social.personservice.person.impl;


import iq.earthlink.social.exception.NotFoundException;
import iq.earthlink.social.personservice.person.FollowManager;
import iq.earthlink.social.personservice.person.FollowSearchCriteria;
import iq.earthlink.social.personservice.person.NotificationsSettingsManager;
import iq.earthlink.social.personservice.person.impl.repository.NotificationsSettingsRepository;
import iq.earthlink.social.personservice.person.model.FollowerNotificationSettings;
import iq.earthlink.social.personservice.person.rest.JsonFollowerNotificationSettings;
import org.dozer.Mapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;

import static iq.earthlink.social.common.util.CommonConstants.ERROR_CHECK_NOT_NULL;
import static iq.earthlink.social.common.util.Preconditions.checkNotNull;


@Service
public class DefaultNotificationsSettingsManager implements NotificationsSettingsManager {

    private final NotificationsSettingsRepository repository;
    private final FollowManager followManager;
    private final Mapper mapper;


    private static final String PERSON_ID = "personId";
    private static final String FOLLOWING_ID = "followingId";

    public DefaultNotificationsSettingsManager(
            NotificationsSettingsRepository repository,
            FollowManager followManager, Mapper mapper) {
        this.repository = repository;
        this.followManager = followManager;
        this.mapper = mapper;
    }


    /**
     * @param personId the person id to get the notification settings for
     * @param page     the pagination params
     * @return the list of followers notification settings
     */
    @Override
    public Page<JsonFollowerNotificationSettings> getNotificationsSettings(Long personId, List<Long> followingIds, Pageable page) {
        checkNotNull(personId, ERROR_CHECK_NOT_NULL, PERSON_ID);
        checkNotNull(page, ERROR_CHECK_NOT_NULL, "page");

        if (CollectionUtils.isEmpty(followingIds))
            return repository.findAllByPersonId(personId, page)
                    .map(f -> mapper.map(f, JsonFollowerNotificationSettings.class));
        else
            return repository.findByPersonIdAndFollowingIdIn(personId, followingIds, page)
                    .map(f -> mapper.map(f, JsonFollowerNotificationSettings.class));
    }

    @Override
    public List<Long> getPersonIdsWhoMutedFollowingId(Long followingId) {
        checkNotNull(followingId, ERROR_CHECK_NOT_NULL, FOLLOWING_ID);
        return repository.findAllByFollowingIdAndIsMutedTrue(followingId).stream()
                .map(FollowerNotificationSettings::getPersonId)
                .toList();
    }

    @Override
    public JsonFollowerNotificationSettings getNotificationSettingsByFollowerId(Long personId, Long followingId) {
        checkNotNull(personId, ERROR_CHECK_NOT_NULL, PERSON_ID);
        checkNotNull(followingId, ERROR_CHECK_NOT_NULL, FOLLOWING_ID);

        var settings = repository.findByPersonIdAndFollowingId(personId, followingId);
        if (settings == null) {
            throw new NotFoundException("error.follow-settings.not.found", followingId);
        }
        return mapper.map(settings, JsonFollowerNotificationSettings.class);
    }

    @Override
    public JsonFollowerNotificationSettings setNotificationsSettings(Long personId, Long followingId, JsonFollowerNotificationSettings jsonFollowerNotificationSettings) {
        checkNotNull(personId, ERROR_CHECK_NOT_NULL, PERSON_ID);
        checkNotNull(followingId, ERROR_CHECK_NOT_NULL, FOLLOWING_ID);
        checkNotNull(jsonFollowerNotificationSettings, ERROR_CHECK_NOT_NULL, "jsonFollowerNotificationSettings");

        FollowSearchCriteria criteria = FollowSearchCriteria.builder()
                .personId(personId)
                .followingIds(List.of(followingId).toArray(Long[]::new))
                .build();

        var isFollowing = !followManager.findFollowedPersons(criteria, Pageable.unpaged()).isEmpty();

        if (!isFollowing) {
            throw new NotFoundException("error.follower.not.following.person", followingId);
        }

        var followerNotificationSettings = repository.findByPersonIdAndFollowingId(personId, followingId);
        if (followerNotificationSettings == null) {
            followerNotificationSettings = new FollowerNotificationSettings();
            followerNotificationSettings.setPersonId(personId);
            followerNotificationSettings.setFollowingId(followingId);
        }
        followerNotificationSettings.setIsMuted(jsonFollowerNotificationSettings.getIsMuted());
        repository.save(followerNotificationSettings);
        return mapper.map(followerNotificationSettings, JsonFollowerNotificationSettings.class);
    }
}