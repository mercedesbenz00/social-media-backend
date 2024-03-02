package iq.earthlink.social.postservice.group.notificationsettings;

import iq.earthlink.social.groupservice.group.rest.JsonGroupMemberWithNotificationSettings;
import iq.earthlink.social.postservice.group.member.repository.GroupMemberRepository;
import iq.earthlink.social.postservice.group.notificationsettings.dto.UserGroupNotificationSettingsEventDTO;
import iq.earthlink.social.postservice.group.notificationsettings.model.UserGroupNotificationSettings;
import iq.earthlink.social.postservice.group.notificationsettings.repository.UserGroupNotificationSettingsRepository;
import iq.earthlink.social.postservice.group.repository.GroupRepository;
import iq.earthlink.social.postservice.person.repository.PersonRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DefaultGroupNotificationSettingsManager implements GroupNotificationSettingsManager {
    private final UserGroupNotificationSettingsRepository repository;
    private final GroupRepository groupRepository;
    private final PersonRepository personRepository;
    private final GroupMemberRepository groupMemberRepository;

    @Override
    public void setGroupNotificationSettings(UserGroupNotificationSettingsEventDTO notificationSettingsEventDTO) {
        var notificationSettingsOptional = repository.findByPersonIdAndUserGroupId(notificationSettingsEventDTO.getPersonId(), notificationSettingsEventDTO.getGroupId());
        if (notificationSettingsOptional.isEmpty()) {
            var group = groupRepository.getByGroupId(notificationSettingsEventDTO.getGroupId());
            var person = personRepository.getPersonByPersonId(notificationSettingsEventDTO.getPersonId());
            if (group.isPresent() && person.isPresent()) {
                var newUserGroupNotificationSettings = UserGroupNotificationSettings
                        .builder()
                        .userGroup(group.get())
                        .person(person.get())
                        .isMuted(notificationSettingsEventDTO.isMuted())
                        .build();
                repository.save(newUserGroupNotificationSettings);
            }
        } else {
            var notificationSettings = notificationSettingsOptional.get();
            notificationSettings.setMuted(notificationSettingsEventDTO.isMuted());
            repository.save(notificationSettings);
        }
    }

    @Override
    public List<JsonGroupMemberWithNotificationSettings> getGroupMembersWithSettings(Long userGroupId) {
        var jsonGroupMemberWithNotificationSettings = new ArrayList<JsonGroupMemberWithNotificationSettings>();

        var groupMemberIds = groupMemberRepository.getAllMemberIdsByGroupId(userGroupId);
        groupMemberIds.forEach(groupMemberId -> {
            var userGroupNotificationSettings = repository.findByPersonIdAndUserGroupId(groupMemberId, userGroupId);
            var jsonGroupMemberWithNotificationSetting = new JsonGroupMemberWithNotificationSettings();

            jsonGroupMemberWithNotificationSetting.setGroupId(userGroupId);
            jsonGroupMemberWithNotificationSetting.setPersonId(groupMemberId);

            if (userGroupNotificationSettings.isPresent()) {
                jsonGroupMemberWithNotificationSetting.setMuted(userGroupNotificationSettings.get().isMuted());
            } else {
                jsonGroupMemberWithNotificationSetting.setMuted(false);
            }
            jsonGroupMemberWithNotificationSettings.add(jsonGroupMemberWithNotificationSetting);
        });

        return jsonGroupMemberWithNotificationSettings;
    }
}
