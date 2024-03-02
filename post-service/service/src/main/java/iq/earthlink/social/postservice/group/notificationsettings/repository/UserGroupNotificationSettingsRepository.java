package iq.earthlink.social.postservice.group.notificationsettings.repository;

import iq.earthlink.social.postservice.group.notificationsettings.model.UserGroupNotificationSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface UserGroupNotificationSettingsRepository extends JpaRepository<UserGroupNotificationSettings, Long> {
    @Query("SELECT ns FROM UserGroupNotificationSettings ns " +
            "WHERE ns.person.personId = :personId AND ns.userGroup.groupId = :userGroupId")
    Optional<UserGroupNotificationSettings> findByPersonIdAndUserGroupId(Long personId, Long userGroupId);
}