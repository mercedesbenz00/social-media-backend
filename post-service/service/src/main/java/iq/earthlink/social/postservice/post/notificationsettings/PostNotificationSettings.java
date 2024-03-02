package iq.earthlink.social.postservice.post.notificationsettings;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"person_id", "post_id"}))
@EntityListeners(AuditingEntityListener.class)
public class PostNotificationSettings {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "user_post_notification_settings_seq_gen")
    @SequenceGenerator(name = "user_post_notification_settings_seq_gen", sequenceName = "user_post_notification_settings_seq_gen", allocationSize = 1)
    private Long id;

    @Column(name = "person_id")
    private Long personId;

    @Column(name = "post_id")
    private Long postId;

    @Column(name = "is_muted")
    private boolean isMuted;

    public PostNotificationSettings(Long personId, Long postId) {
        this.personId = personId;
        this.postId = postId;
    }
}

