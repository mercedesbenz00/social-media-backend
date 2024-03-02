package iq.earthlink.social.personservice.person.impl;

import iq.earthlink.social.classes.data.dto.PersonData;
import iq.earthlink.social.classes.data.event.NotificationEvent;
import iq.earthlink.social.classes.enumeration.ContentType;
import iq.earthlink.social.classes.enumeration.NotificationState;
import iq.earthlink.social.classes.enumeration.NotificationType;
import iq.earthlink.social.common.util.CommonConstants;
import iq.earthlink.social.exception.BadRequestException;
import iq.earthlink.social.exception.NotUniqueException;
import iq.earthlink.social.exception.RestApiException;
import iq.earthlink.social.personservice.dto.FollowingEventDTO;
import iq.earthlink.social.personservice.dto.FollowingEventType;
import iq.earthlink.social.personservice.person.FollowManager;
import iq.earthlink.social.personservice.person.FollowSearchCriteria;
import iq.earthlink.social.personservice.person.impl.repository.FollowRepository;
import iq.earthlink.social.personservice.person.model.Following;
import iq.earthlink.social.personservice.person.model.Person;
import iq.earthlink.social.personservice.person.rest.JsonPerson;
import iq.earthlink.social.personservice.service.KafkaProducerService;
import lombok.RequiredArgsConstructor;
import org.dozer.Mapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Nonnull;
import java.util.*;

import static iq.earthlink.social.common.util.CommonConstants.ERROR_CHECK_NOT_NULL;
import static iq.earthlink.social.common.util.Preconditions.checkNotNull;

@Service
@RequiredArgsConstructor
public class DefaultFollowManager implements FollowManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultFollowManager.class);
    private static final String CRITERIA = "criteria";

    private final FollowRepository repository;
    private final KafkaProducerService kafkaProducerService;
    private final Mapper mapper;

    @Transactional
    @Override
    public Following follow(@Nonnull Person follower, @Nonnull Person followed) {
        checkNotNull(follower, ERROR_CHECK_NOT_NULL, "follower");
        checkNotNull(followed, ERROR_CHECK_NOT_NULL, "followed");

        LOGGER.debug("Establish follow connection between persons: {} and {}",
                follower.getId(), followed.getId());

        if (Objects.equals(follower, followed)) {
            throw new BadRequestException("error.follow.yourself.issue", follower.getId());
        }
        final Following following = new Following();
        following.setSubscriber(follower);
        following.setSubscribedTo(followed);
        following.setCreatedAt(new Date());

        try {
            Following savedFollowing = repository.saveAndFlush(following);
            sendFollowingEvent(follower.getId(), followed.getId(), FollowingEventType.FOLLOW);

            sendNotification(follower, Collections.singletonList(followed.getId()));

            return savedFollowing;
        } catch (DataIntegrityViolationException ex) {
            LOGGER.warn("Already established follow connection between persons: {} and {}", follower.getId(), followed.getId());
            throw new NotUniqueException("error.follower.already.following", follower.getId(), followed.getId());
        } catch (Exception ex) {
            LOGGER.warn("Failed to save new following: {} and {}", follower.getId(), ex);
            throw new RestApiException(HttpStatus.INTERNAL_SERVER_ERROR, "error.follow.save.failed", following);
        }
    }

    @Transactional
    @Override
    public void unfollow(@Nonnull Long followerId, @Nonnull Person followed) {
        checkNotNull(followerId, ERROR_CHECK_NOT_NULL, "followerId");
        checkNotNull(followed, ERROR_CHECK_NOT_NULL, "followed");

        LOGGER.debug("Removing follow connection between persons: {} and {}",
                followerId, followed.getId());

        var isFollowed = repository.findFollowingBySubscribedToIdAndSubscriberIdIn(followed.getId(), List.of(followerId), Pageable.unpaged()).hasContent();
        if (isFollowed) {
            repository.unfollow(followerId, followed.getId());
            sendFollowingEvent(followerId, followed.getId(), FollowingEventType.UNFOLLOW);
        }
    }

    @Nonnull
    @Override
    public Page<Following> findFollowersOld(@Nonnull FollowSearchCriteria criteria, @Nonnull Pageable page) {
        checkNotNull(criteria, ERROR_CHECK_NOT_NULL, CRITERIA);
        checkNotNull(page, ERROR_CHECK_NOT_NULL, "page");

        return repository.findFollowers(criteria, page);
    }

    @Nonnull
    @Override
    public Page<JsonPerson> findFollowers(@Nonnull FollowSearchCriteria criteria, @Nonnull Pageable page) {
        checkNotNull(criteria, ERROR_CHECK_NOT_NULL, CRITERIA);
        checkNotNull(page, ERROR_CHECK_NOT_NULL, "page");

        // Find followers based on search criteria:
        Page<JsonPerson> followers = repository.findFollowers(criteria, page).map(f -> mapper.map(f.getSubscriber(), JsonPerson.class));

        // Find person's subscriptions:
        if (followers.hasContent()) {
            criteria.setFollowingIds(followers.stream().map(JsonPerson::getId).toArray(Long[]::new));
            List<Long> followingsIds = findFollowedPersonsOld(criteria, Pageable.unpaged())
                    .stream().map(f -> f.getSubscribedTo().getId()).toList();

            followers.forEach(follower -> follower.setFollowing(followingsIds.contains(follower.getId())));
        }

        return followers;
    }

    @Nonnull
    @Override
    public Page<Following> findFollowedPersonsOld(@Nonnull FollowSearchCriteria criteria, @Nonnull Pageable page) {
        checkNotNull(criteria, ERROR_CHECK_NOT_NULL, CRITERIA);
        checkNotNull(page, ERROR_CHECK_NOT_NULL, "page");

        return repository.findFollowed(criteria, page);
    }

    @Nonnull
    @Override
    public Page<JsonPerson> findFollowedPersons(@Nonnull FollowSearchCriteria criteria, @Nonnull Pageable page) {
        checkNotNull(criteria, ERROR_CHECK_NOT_NULL, CRITERIA);
        checkNotNull(page, ERROR_CHECK_NOT_NULL, "page");

        // Find persons who current user follow based on search criteria:
        Page<JsonPerson> followings = repository.findFollowed(criteria, page).map(f -> mapper.map(f.getSubscribedTo(), JsonPerson.class));

        // Find who subscribe current person back:
        if (!followings.isEmpty()) {
            criteria.setFollowerIds(followings.stream().map(JsonPerson::getId).toArray(Long[]::new));
        }
        List<Long> followedWhoFollowBackIds = findFollowersOld(criteria, Pageable.unpaged())
                .stream().map(f -> f.getSubscriber().getId()).toList();

        followings.forEach(follower -> follower.setFollowing(followedWhoFollowBackIds.contains(follower.getId())));

        return followings;
    }

    private void sendFollowingEvent(Long followerId, Long followedId, FollowingEventType eventType) {
        kafkaProducerService.sendMessageWithKey(CommonConstants.FOLLOWING_EVENT, followedId.toString(), FollowingEventDTO
                .builder()
                .followerId(followerId)
                .followedId(followedId)
                .eventType(eventType)
                .build());
    }

    private void sendNotification(Person author, List<Long> recipientIds) {
        NotificationEvent event = NotificationEvent
                .builder()
                .eventAuthor(mapper.map(author, PersonData.class))
                .receiverIds(recipientIds)
                .type(NotificationType.USER_FOLLOWED)
                .state(NotificationState.NEW)
                .metadata(Map.of(
                        "routeTo", ContentType.PERSON.name(),
                        "personId", author.getId().toString()
                )).build();

        // Send event for notification:
        kafkaProducerService.sendMessage("PUSH_NOTIFICATION", event);
    }
}
