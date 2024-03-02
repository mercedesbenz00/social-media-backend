package iq.earthlink.social.postservice.post.notificationsettings;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.Optional;

public interface PostNotificationSettingsRepository extends JpaRepository<PostNotificationSettings, Long> {
    Page<PostNotificationSettings> findByPersonId(Long personId, Pageable page);

    Page<PostNotificationSettings> findByPersonIdAndPostIdIn(Long personId, Collection<Long> postIds, Pageable page);

    Optional<PostNotificationSettings> findByPersonIdAndPostId(Long personId, Long postId);
}
