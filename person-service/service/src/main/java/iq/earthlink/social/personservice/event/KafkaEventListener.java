package iq.earthlink.social.personservice.event;

import iq.earthlink.social.classes.data.dto.JsonImageProcessResult;
import iq.earthlink.social.classes.enumeration.RegistrationState;
import iq.earthlink.social.common.util.CommonConstants;
import iq.earthlink.social.notificationservice.data.dto.JsonPushTokenAction;
import iq.earthlink.social.personservice.dto.*;
import iq.earthlink.social.personservice.person.PersonMediaService;
import iq.earthlink.social.personservice.person.impl.repository.PersonGroupsRepository;
import iq.earthlink.social.personservice.person.impl.repository.PersonRepository;
import iq.earthlink.social.personservice.person.model.Person;
import iq.earthlink.social.personservice.person.model.PersonGroup;
import iq.earthlink.social.personservice.post.dto.PostEventDTO;
import iq.earthlink.social.personservice.post.dto.PostEventType;
import iq.earthlink.social.personservice.post.model.ProcessedPostPerson;
import iq.earthlink.social.personservice.post.repository.ProcessedPostPersonRepository;
import iq.earthlink.social.personservice.service.ChatAdministrationService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class KafkaEventListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(KafkaEventListener.class);

    private final ChatAdministrationService chatAdministrationService;
    private final PersonMediaService personMediaService;
    private final PersonRepository personRepository;
    private final PersonGroupsRepository personGroupsRepository;
    private final ProcessedPostPersonRepository processedPostPersonRepository;

    @KafkaListener(topics = "PushTokenAction", groupId = "group-id",
            containerFactory = "kafkaMetaDataListenerContainerFactory")
    public void shortVideoDataListener(@Payload JsonPushTokenAction action) {
        chatAdministrationService.updateUserPushNotificationToken(action);
    }

    @KafkaListener(topics = CommonConstants.MEDIA_PROCESSED, groupId = "person-service-image-processing",
            containerFactory = "imageServiceListenerContainerFactory")
    public void processedImagesListener(@Payload JsonImageProcessResult result) {
        if (result.getServiceName().equals("person-service")) {
            personMediaService.uploadSizedImages(result);
        }
    }

    @KafkaListener(topics = CommonConstants.INTEREST_GROUP_ONBOARD_STATE, groupId = "person-service-group-interest-onboard",
            containerFactory = "groupInterestOnboardListenerContainerFactory")
    public void groupInterestOnboardListener(@Payload GroupInterestOnboardDTO onboardDTO) {
        var person = personRepository.findById(onboardDTO.getPersonId());
        if (person.isPresent()) {
            var personEntity = person.get();
            var state = onboardDTO.getState();
            if (GroupInterestOnboardState.INTERESTS_PROVIDED.equals(state) && personEntity.getState().canBeChangedTo(RegistrationState.INTERESTS_PROVIDED)) {
                personEntity.setState(RegistrationState.INTERESTS_PROVIDED);
            }

            if (GroupInterestOnboardState.GROUPS_PROVIDED.equals(state) && personEntity.getState().canBeChangedTo(RegistrationState.REGISTRATION_COMPLETED)) {
                personEntity.setState(RegistrationState.REGISTRATION_COMPLETED);
            }
            personRepository.save(personEntity);
        }
    }

    @KafkaListener(topics = CommonConstants.FOLLOWING_EVENT, groupId = "person-service-following-event", containerFactory = "followingEventListenerContainerFactory")
    @Transactional
    public void followingEventListener(@Payload FollowingEventDTO followingEventDTO) {
        LOGGER.info("Received following {} event for the person with id: {}", followingEventDTO.getEventType(), followingEventDTO.getFollowedId());
        long delta = FollowingEventType.FOLLOW.equals(followingEventDTO.getEventType()) ? 1L : -1L;
        if (Objects.nonNull(followingEventDTO.getFollowerId())) {
            Optional<Person> optionalFollower = personRepository.findById(followingEventDTO.getFollowerId());
            optionalFollower.ifPresent(follower ->
                    personRepository.updateUserFollowingCount(follower.getId(), delta));
        }

        if (Objects.nonNull(followingEventDTO.getFollowedId())) {
            Optional<Person> optionalFollowed = personRepository.findById(followingEventDTO.getFollowedId());
            optionalFollowed.ifPresent(followed ->
                    personRepository.updateUserFollowersCount(followed.getId(), delta));
        }
    }

    @KafkaListener(topics = CommonConstants.GROUP_MEMBER_EVENT, groupId = "person-service-group-member-event", containerFactory = "groupMemberEventListenerContainerFactory")
    @Transactional
    public void groupMemberEventListener(@Payload GroupMemberEventDTO groupMemberEventDTO) {
        LOGGER.info("Received group member {} event for the person with id: {}", groupMemberEventDTO.getEventType(), groupMemberEventDTO.getPersonId());

        if (groupMemberEventDTO.getPersonId() != null && groupMemberEventDTO.getGroupId() != null) {
            GroupMemberEventType eventType = groupMemberEventDTO.getEventType();
            long delta = GroupMemberEventType.JOINED.equals(eventType) ? 1L : -1L;
            var person = personRepository.findById(groupMemberEventDTO.getPersonId());
            if (person.isPresent()) {
                var personGroup = personGroupsRepository.getByPersonIdAndGroupId(groupMemberEventDTO.getPersonId(), groupMemberEventDTO.getGroupId());
                if (Objects.equals(GroupMemberEventType.LEFT, eventType) && personGroup.isPresent()) {
                    personGroupsRepository.delete(personGroup.get());
                    personRepository.updateUserGroupsCount(groupMemberEventDTO.getPersonId(), delta);
                }
                if (Objects.equals(GroupMemberEventType.JOINED, eventType) && personGroup.isEmpty()) {
                    personGroupsRepository.save(PersonGroup
                            .builder()
                            .personId(groupMemberEventDTO.getPersonId())
                            .groupId(groupMemberEventDTO.getGroupId())
                            .build());
                    personRepository.updateUserGroupsCount(groupMemberEventDTO.getPersonId(), delta);
                }
            }
        }
    }

    @KafkaListener(topics = CommonConstants.POST_EVENT, groupId = "person-service-post-event",
            containerFactory = "postEventListenerContainerFactory")
    @Transactional
    public void postEventListener(@Payload PostEventDTO postEventDTO) {
        LOGGER.info("Received post {} event for the person with id: {}", postEventDTO.getEventType(), postEventDTO.getPersonId());

        if (postEventDTO.getPersonId() == null) {
            return;
        }

        PostEventType eventType = postEventDTO.getEventType();
        Optional<ProcessedPostPerson> optionalProcessedPostPerson =
                processedPostPersonRepository.findByPostIdAndPersonId(postEventDTO.getPostId(), postEventDTO.getPersonId());

        if (PostEventType.POST_PUBLISHED.equals(eventType) && optionalProcessedPostPerson.isEmpty()) {
            ProcessedPostPerson processedPostGroup = ProcessedPostPerson.builder()
                    .postId(postEventDTO.getPostId())
                    .personId(postEventDTO.getPersonId())
                    .build();

            processedPostPersonRepository.save(processedPostGroup);
            personRepository.updatePostCount(postEventDTO.getPersonId(), 1L);
        } else if (PostEventType.POST_UNPUBLISHED.equals(eventType) && optionalProcessedPostPerson.isPresent()) {
            processedPostPersonRepository.delete(optionalProcessedPostPerson.get());
            personRepository.updatePostCount(postEventDTO.getPersonId(), -1L);
        }
    }
}
