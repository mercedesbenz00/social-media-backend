package iq.earthlink.social.personservice.person.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"person_id", "following_id"}))
@EntityListeners(AuditingEntityListener.class)
public class FollowerNotificationSettings {


  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "person_notification_settings_seq_gen")
  @SequenceGenerator(name = "person_notification_settings_seq_gen", sequenceName = "person_notification_settings_seq_gen", allocationSize = 1)
  private Long id;

  @Column(name = "person_id")
  private Long personId;

  @Column(name = "following_id")
  private Long followingId;

  private Boolean isMuted;
}