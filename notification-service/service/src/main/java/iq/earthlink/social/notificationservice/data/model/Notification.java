package iq.earthlink.social.notificationservice.data.model;

import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import iq.earthlink.social.classes.enumeration.NotificationState;
import iq.earthlink.social.classes.enumeration.NotificationType;
import lombok.*;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.Date;
import java.util.Map;

@Entity
@Data
@NoArgsConstructor
@Builder
@AllArgsConstructor
@ToString
@Table(
        indexes = {
                @Index(name = "notification_state_idx", columnList = "state"),
                @Index(name = "notification_topic_idx", columnList = "topic"),
                @Index(name = "notification_receiver_idx", columnList = "receiverId")
        }
)
@TypeDef(name = "jsonb", typeClass = JsonBinaryType.class)
@EntityListeners(AuditingEntityListener.class)
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "notification_seq_gen")
    @SequenceGenerator(name = "notification_seq_gen", sequenceName = "notification_seq_gen", allocationSize = 1)
    private Long id;

    @NotNull
    private Integer batchId;

    private Long authorId;

    @NotNull
    private Long receiverId;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "char(32) default 'NEW'")
    private NotificationState state;

    private String body;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "char(32)")
    private NotificationType topic;

    @NotNull
    @CreatedDate
    private Date createdDate;

    private Date updatedDate;

    @Type(type = "jsonb")
    @Column(name = "meta", columnDefinition = "jsonb")
    private Map<String, String> metadata;
}
