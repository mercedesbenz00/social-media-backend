package iq.earthlink.social.groupservice.group.notificationsettings;

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
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"person_id", "group_id"}))
@EntityListeners(AuditingEntityListener.class)
public class UserGroupNotificationSettings {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "user_group_notification_settings_seq_gen")
    @SequenceGenerator(name = "user_group_notification_settings_seq_gen", sequenceName = "user_group_notification_settings_seq_gen", allocationSize = 1)
    private Long id;

    @Column(name = "person_id")
    private Long personId;

    @Column(name = "group_id")
    private Long groupId;

    @Column(name = "is_muted")
    private boolean isMuted;

    public UserGroupNotificationSettings(Long personId, Long groupId) {
        this.personId = personId;
        this.groupId = groupId;
    }
}
