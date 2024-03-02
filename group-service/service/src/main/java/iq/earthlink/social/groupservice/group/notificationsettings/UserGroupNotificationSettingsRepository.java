package iq.earthlink.social.groupservice.group.notificationsettings;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.Optional;

public interface UserGroupNotificationSettingsRepository extends JpaRepository<UserGroupNotificationSettings, Long> {
    Page<UserGroupNotificationSettings> findByPersonId(Long personId, Pageable page);

    Page<UserGroupNotificationSettings> findByPersonIdAndGroupIdIn(Long personId, Collection<Long> groupId, Pageable pageable);

    Optional<UserGroupNotificationSettings> findByPersonIdAndGroupId(Long personId, Long groupId);
}