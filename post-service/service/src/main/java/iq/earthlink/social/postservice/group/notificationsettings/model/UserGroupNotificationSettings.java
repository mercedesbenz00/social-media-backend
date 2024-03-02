package iq.earthlink.social.postservice.group.notificationsettings.model;

import iq.earthlink.social.postservice.group.model.UserGroup;
import iq.earthlink.social.postservice.person.model.Person;
import lombok.*;
import org.hibernate.Hibernate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.Objects;

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "person_id")
    @NotNull
    @ToString.Exclude
    private Person person;

    @NotNull
    @JoinColumn(name = "group_id")
    @ManyToOne
    private UserGroup userGroup;

    @Column(name = "is_muted")
    private boolean isMuted;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) return false;
        UserGroupNotificationSettings that = (UserGroupNotificationSettings) o;
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
