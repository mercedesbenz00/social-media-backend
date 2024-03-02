package iq.earthlink.social.personservice.person.impl.repository;

import iq.earthlink.social.personservice.person.model.FollowerNotificationSettings;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface NotificationsSettingsRepository extends JpaRepository<FollowerNotificationSettings, Long> {

    Page<FollowerNotificationSettings> findAllByPersonId(Long personId, Pageable page);

    Page<FollowerNotificationSettings> findByPersonIdAndFollowingIdIn(Long personId, Collection<Long> followingId, Pageable pageable);

    FollowerNotificationSettings findByPersonIdAndFollowingId(Long personId, Long followingId);

    List<FollowerNotificationSettings> findAllByFollowingIdAndIsMutedTrue(Long followingId);
}
