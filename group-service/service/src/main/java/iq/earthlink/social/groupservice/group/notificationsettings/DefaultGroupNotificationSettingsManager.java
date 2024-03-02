package iq.earthlink.social.groupservice.group.notificationsettings;

import iq.earthlink.social.common.audit.AuditMarker;
import iq.earthlink.social.common.audit.AuditMessage;
import iq.earthlink.social.common.audit.EventAction;
import iq.earthlink.social.common.util.CommonConstants;
import iq.earthlink.social.exception.NotFoundException;
import iq.earthlink.social.groupservice.event.KafkaProducerService;
import iq.earthlink.social.groupservice.group.GroupManagerUtils;
import iq.earthlink.social.groupservice.group.UserGroup;
import iq.earthlink.social.groupservice.group.rest.JsonGroupNotificationSettings;
import iq.earthlink.social.groupservice.group.rest.UserGroupNotificationSettingsDTO;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dozer.Mapper;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DefaultGroupNotificationSettingsManager implements GroupNotificationSettingsManager {

    private static final Logger LOGGER = LogManager.getLogger(DefaultGroupNotificationSettingsManager.class);

    private final UserGroupNotificationSettingsRepository repository;
    private final GroupManagerUtils groupManagerUtils;
    private final KafkaProducerService kafkaProducerService;
    private final Mapper mapper;

    @NotNull
    @Override
    public Page<UserGroupNotificationSettingsDTO> findGroupNotificationSettings(@NotNull Long personId, List<Long> groupIds, @NotNull Pageable page) {
        if (CollectionUtils.isEmpty(groupIds))
            return repository.findByPersonId(personId, page).map(userGroupNotificationSettings ->
                    UserGroupNotificationSettingsDTO.builder()
                            .groupId(userGroupNotificationSettings.getGroupId())
                            .isMuted(userGroupNotificationSettings.isMuted())
                            .build());
        else
            return repository.findByPersonIdAndGroupIdIn(personId, groupIds, page).map(userGroupNotificationSettings ->
                    UserGroupNotificationSettingsDTO.builder()
                            .groupId(userGroupNotificationSettings.getGroupId())
                            .isMuted(userGroupNotificationSettings.isMuted())
                            .build());
    }

    @NotNull
    @Override
    public UserGroupNotificationSettingsDTO findGroupNotificationSettingsByGroupId(Long personId, Long groupId) {
        UserGroupNotificationSettings settings = repository.findByPersonIdAndGroupId(personId, groupId)
                .orElseThrow(() -> new NotFoundException("error.not.found.group.notification.settings", personId, groupId));

        return mapper.map(settings, UserGroupNotificationSettingsDTO.class);
    }

    @Override
    public UserGroupNotificationSettingsDTO setGroupNotificationSettings(Long personId, Long groupId, JsonGroupNotificationSettings request) {
        UserGroup group = groupManagerUtils.getGroup(groupId);
        UserGroupNotificationSettings userGroupNotificationSettings = repository.findByPersonIdAndGroupId(personId, groupId)
                .orElse(new UserGroupNotificationSettings(personId, groupId));

        userGroupNotificationSettings.setMuted(request.getIsMuted());
        repository.save(userGroupNotificationSettings);

        kafkaProducerService.sendMessage(CommonConstants.USER_GROUP_NOTIFICATION_SETTINGS_SET, userGroupNotificationSettings);

        String logMessage = String.format("Notification settings for the group \"%s\" were updated by \"%s\"", group.getName(), personId);
        LOGGER.info(AuditMarker.getMarker(), AuditMessage.create(EventAction.CHANGE_GROUP_SETTINGS, logMessage, personId, groupId));

        return mapper.map(userGroupNotificationSettings, UserGroupNotificationSettingsDTO.class);
    }

    @Override
    public List<UserGroupNotificationSettings> getAllGroupNotificationSettings(){
        return repository.findAll();
    }
}
