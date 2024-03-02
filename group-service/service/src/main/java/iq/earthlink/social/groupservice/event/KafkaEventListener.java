package iq.earthlink.social.groupservice.event;

import iq.earthlink.social.classes.data.dto.JsonImageProcessResult;
import iq.earthlink.social.common.util.CommonConstants;
import iq.earthlink.social.groupservice.group.GroupMediaService;
import iq.earthlink.social.groupservice.group.GroupStatisticsRepository;
import iq.earthlink.social.groupservice.group.member.GroupMember;
import iq.earthlink.social.groupservice.group.member.GroupMemberRepository;
import iq.earthlink.social.groupservice.person.PersonManager;
import iq.earthlink.social.groupservice.person.dto.PersonDTO;
import iq.earthlink.social.groupservice.person.dto.PersonEventDTO;
import iq.earthlink.social.groupservice.person.repository.PersonRepository;
import iq.earthlink.social.groupservice.post.dto.PostEventDTO;
import iq.earthlink.social.groupservice.post.dto.PostEventType;
import iq.earthlink.social.groupservice.post.model.ProcessedPostGroup;
import iq.earthlink.social.groupservice.post.repository.ProcessedPostGroupRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static iq.earthlink.social.common.util.CommonConstants.GROUP_SERVICE;

@Component
@RequiredArgsConstructor
public class KafkaEventListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(KafkaEventListener.class);
    private final GroupMediaService groupMediaService;
    private final PersonManager personManager;
    private final GroupMemberRepository groupMemberRepository;
    private final GroupStatisticsRepository groupStatisticsRepository;
    private final PersonRepository personRepository;
    private final ProcessedPostGroupRepository processedPostGroupRepository;


    @KafkaListener(topics = CommonConstants.MEDIA_PROCESSED, groupId = "group-service-image-processing",
            containerFactory = "imageServiceListenerContainerFactory")
    public void processedImagesListener(@Payload JsonImageProcessResult result) {
        LOGGER.info("Received result from media service for entity with id: {}", result.getEntityId());
        if (result.getServiceName().equals(GROUP_SERVICE)) {
            groupMediaService.uploadSizedImages(result);
        }
    }

    @KafkaListener(topics = CommonConstants.PERSON_CREATED, groupId = "group-service-person-created",
            containerFactory = "personServiceListenerContainerFactory")
    public void personCreated(@Payload PersonEventDTO person) {
        LOGGER.info("Received person created event for person with uuid: {}", person.getUuid());
        personManager.savePerson(PersonDTO
                .builder()
                .uuid(person.getUuid())
                .personId(person.getId())
                .displayName(person.getDisplayName())
                .avatar(person.getAvatar())
                .roles(person.getRoles())
                .isVerifiedAccount(person.isVerifiedAccount())
                .createdAt(person.getCreatedAt())
                .build());

    }

    @KafkaListener(topics = CommonConstants.PERSON_UPDATED, groupId = "group-service-person-updated",
            containerFactory = "personServiceListenerContainerFactory")
    public void personUpdated(@Payload PersonEventDTO person) {
        LOGGER.info("Received person updated event for person with uuid: {}", person.getUuid());
        personManager.updatePerson(PersonDTO
                .builder()
                .uuid(person.getUuid())
                .personId(person.getId())
                .displayName(person.getDisplayName())
                .avatar(person.getAvatar())
                .roles(person.getRoles())
                .isVerifiedAccount(person.isVerifiedAccount())
                .createdAt(person.getCreatedAt())
                .build());
    }

    @KafkaListener(topics = CommonConstants.POST_EVENT, groupId = "group-service-post-event",
            containerFactory = "postEventListenerContainerFactory")
    @Transactional
    public void postEventListener(@Payload PostEventDTO postEventDTO) {
        LOGGER.info("Received post {} event for the person with id: {}", postEventDTO.getEventType(), postEventDTO.getPersonId());
        if (postEventDTO.getPersonId() == null || postEventDTO.getGroupId() == null) {
            return;
        }

        PostEventType eventType = postEventDTO.getEventType();
        Optional<ProcessedPostGroup> optionalProcessedPostGroup =
                processedPostGroupRepository.findByPostIdAndGroupId(postEventDTO.getPostId(), postEventDTO.getGroupId());

        if (PostEventType.POST_PUBLISHED.equals(eventType) && optionalProcessedPostGroup.isEmpty()) {
            ProcessedPostGroup processedPostGroup = ProcessedPostGroup.builder()
                    .postId(postEventDTO.getPostId())
                    .groupId(postEventDTO.getGroupId())
                    .build();

            processedPostGroupRepository.save(processedPostGroup);

            updateGroupMemberPostsCount(postEventDTO, 1L);
            updateGroupPostsCount(postEventDTO, 1L);
        } else if (PostEventType.POST_UNPUBLISHED.equals(eventType) && optionalProcessedPostGroup.isPresent()) {
            processedPostGroupRepository.delete(optionalProcessedPostGroup.get());

            updateGroupMemberPostsCount(postEventDTO, -1L);
            updateGroupPostsCount(postEventDTO, -1L);
        }
    }

    private void updateGroupMemberPostsCount(PostEventDTO postEventDTO, long delta) {
        Long personId = postEventDTO.getPersonId();
        Long groupId = postEventDTO.getGroupId();
        GroupMember member = groupMemberRepository.findActiveMember(groupId, personId);
        if (member != null) {
            groupMemberRepository.updatePostCount(personId, groupId, delta);
        }
    }

    private void updateGroupPostsCount(PostEventDTO postEventDTO, long delta) {
        var person = personRepository.getPersonByPersonId(postEventDTO.getPersonId());
        if (person.isPresent()) {
            groupStatisticsRepository.updatePostCount(postEventDTO.getGroupId(), delta);
        }
    }
}
