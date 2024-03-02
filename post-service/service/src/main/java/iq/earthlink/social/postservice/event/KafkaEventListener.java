package iq.earthlink.social.postservice.event;

import iq.earthlink.social.classes.data.dto.ContentModerationDto;
import iq.earthlink.social.classes.data.dto.JsonImageProcessResult;
import iq.earthlink.social.classes.enumeration.ContentType;
import iq.earthlink.social.common.util.CommonConstants;
import iq.earthlink.social.postservice.group.AccessType;
import iq.earthlink.social.postservice.group.GroupManager;
import iq.earthlink.social.postservice.group.dto.GroupEventDTO;
import iq.earthlink.social.postservice.group.member.GroupMemberManager;
import iq.earthlink.social.postservice.group.member.dto.GroupMemberEventDTO;
import iq.earthlink.social.postservice.group.member.enumeration.GroupMemberEventType;
import iq.earthlink.social.postservice.group.notificationsettings.GroupNotificationSettingsManager;
import iq.earthlink.social.postservice.group.notificationsettings.dto.UserGroupNotificationSettingsEventDTO;
import iq.earthlink.social.postservice.person.PersonManager;
import iq.earthlink.social.postservice.person.dto.PersonDTO;
import iq.earthlink.social.postservice.person.dto.PersonEventDTO;
import iq.earthlink.social.postservice.post.PostManager;
import iq.earthlink.social.postservice.post.PostMediaService;
import iq.earthlink.social.postservice.post.comment.CommentService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import static iq.earthlink.social.common.util.CommonConstants.*;

@Component
@RequiredArgsConstructor
public class KafkaEventListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(KafkaEventListener.class);
    private final PostMediaService postMediaService;
    private final PostManager postManager;
    private final PersonManager personManager;
    private final GroupManager groupManager;
    private final GroupMemberManager groupMemberManager;
    private final CommentService commentManager;
    private final GroupNotificationSettingsManager groupNotificationSettingsManager;

    @KafkaListener(topics = CommonConstants.DELETE_TOPIC, groupId = "group-id",
            containerFactory = "kafkaMetaDataListenerContainerFactory")
    public void deleteModeratedContent(@Payload ContentModerationDto dto) {
        LOGGER.info("Received an event to delete offensive content");
        try {
            switch (ContentType.fromString(dto.getType())) {
                case POST -> postManager.removePostByModerator(dto);
                case POST_COMMENT -> commentManager.removeCommentByModerator(dto);
                default -> LOGGER.info("Received unsupported type: {}", dto.getType());
            }
        } catch (Exception ex) {
            LOGGER.error("Could not delete offensive content: {}", ex.getMessage());
        }
    }

    @KafkaListener(topics = CommonConstants.MEDIA_PROCESSED, groupId = "post-service-image-processing",
            containerFactory = "imageServiceListenerContainerFactory")
    public void processedImagesListener(@Payload JsonImageProcessResult result) {
        LOGGER.info("Received result from media service for entity with id: {}", result.getEntityId());
        if (result.getServiceName().equals(POST_SERVICE)) {
            Long postId = Long.valueOf(StringUtils.isEmpty(result.getParentEntityId()) ? "0" : result.getParentEntityId());
            postMediaService.uploadSizedImages(postId, result);
        }
        // receiving sized images for the group entity and person entity
        if (result.getServiceName().equals(GROUP_SERVICE) || result.getServiceName().equals(PERSON_SERVICE)) {
            postMediaService.uploadSizedImages(result);
        }
    }

    @KafkaListener(topics = CommonConstants.PERSON_CREATED, groupId = "post-service-person-created",
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

    @KafkaListener(topics = CommonConstants.PERSON_UPDATED, groupId = "post-service-person-updated",
            containerFactory = "personServiceListenerContainerFactory")
    public void personUpdated(@Payload PersonEventDTO person) {
        LOGGER.info("Received person updated event for person with uuid: {}", person.getUuid());
        String currentDisplayName = personManager.getPersonByUuid(person.getUuid()).getDisplayName();
        //TODO: it should be removed after v1 post API will be disabled
        if (!currentDisplayName.equals(person.getDisplayName())) {
            postManager.updatePostsAuthorDisplayName(person.getId(), person.getDisplayName());
        }
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


    @KafkaListener(topics = CommonConstants.GROUP_CREATED, groupId = "post-service-group-created",
            containerFactory = "groupServiceListenerContainerFactory")
    public void groupCreated(@Payload GroupEventDTO groupEventDTO) {
        LOGGER.info("Received group created event for group with id: {}", groupEventDTO.getId());
        groupManager.saveGroup(groupEventDTO);

    }

    @KafkaListener(topics = CommonConstants.GROUP_UPDATED, groupId = "post-service-group-updated",
            containerFactory = "groupServiceListenerContainerFactory")
    public void groupUpdated(@Payload GroupEventDTO groupEventDTO) {
        LOGGER.info("Received group updated event for group with id: {}", groupEventDTO.getId());
        if (groupEventDTO.getAccessType() != null) {
            AccessType currentAccessType = groupManager.getGroupById(groupEventDTO.getId()).getAccessType();
            if (!currentAccessType.equals(groupEventDTO.getAccessType())) {
                postManager.updatePostsUserGroupType(groupEventDTO.getId(), groupEventDTO.getAccessType());
            }
        }
        groupManager.updateGroup(groupEventDTO);
    }

    @KafkaListener(topics = CommonConstants.GROUP_MEMBER_EVENT, groupId = "post-service-group-member-created",
            containerFactory = "groupMemberServiceListenerContainerFactory")
    public void groupMemberCreated(@Payload GroupMemberEventDTO groupMemberEventDTO) {
        LOGGER.info("Received group member {} event for the person with id: {}", groupMemberEventDTO.getEventType(), groupMemberEventDTO.getPersonId());
        if (GroupMemberEventType.JOINED.equals(groupMemberEventDTO.getEventType())) {
            groupMemberManager.saveGroupMember(groupMemberEventDTO);
        }
        if (GroupMemberEventType.LEFT.equals(groupMemberEventDTO.getEventType())) {
            groupMemberManager.deleteGroupMember(groupMemberEventDTO);
        }
    }

    @KafkaListener(topics = CommonConstants.GROUP_MEMBER_ADD_PERMISSION, groupId = "post-service-group-member-add-permission",
            containerFactory = "groupMemberServiceListenerContainerFactory")
    public void groupMemberAddPermission(@Payload GroupMemberEventDTO groupMemberEventDTO) {
        LOGGER.info("Received group member permission added event for person with id: {}", groupMemberEventDTO.getPersonId());
        groupMemberManager.addPermission(groupMemberEventDTO);
    }

    @KafkaListener(topics = CommonConstants.GROUP_MEMBER_DELETE_PERMISSION, groupId = "post-service-group-member-delete-permission",
            containerFactory = "groupMemberServiceListenerContainerFactory")
    public void groupMemberDeletePermission(@Payload GroupMemberEventDTO groupMemberEventDTO) {
        LOGGER.info("Received group member permissions deleted event for person with id: {}", groupMemberEventDTO.getPersonId());
        groupMemberManager.deletePermissions(groupMemberEventDTO);
    }

    @KafkaListener(topics = CommonConstants.USER_GROUP_NOTIFICATION_SETTINGS_SET, groupId = "post-service-notification-settings-created",
            containerFactory = "notificationSettingsServiceListenerContainerFactory")
    public void groupNotificationSettingsCreated(@Payload UserGroupNotificationSettingsEventDTO userGroupNotificationSettingsEventDTO) {
        LOGGER.info("Received group notification settings set event for person with id: {}", userGroupNotificationSettingsEventDTO.getPersonId());
        groupNotificationSettingsManager.setGroupNotificationSettings(userGroupNotificationSettingsEventDTO);
    }
}
