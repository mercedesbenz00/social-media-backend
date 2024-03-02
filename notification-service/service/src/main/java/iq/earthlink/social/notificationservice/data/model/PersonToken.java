package iq.earthlink.social.notificationservice.data.model;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.Date;

@Entity
@Data
@NoArgsConstructor
@Builder
@AllArgsConstructor
@ToString
@EntityListeners(AuditingEntityListener.class)
@Table(
        indexes = {@Index(name = "persontoken_personId_device_idx", columnList = "personId, device", unique = true)}
)
public class PersonToken {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "person_token_seq_gen")
    @SequenceGenerator(name = "person_token_seq_gen", sequenceName = "person_token_seq_gen", allocationSize = 1)
    private Long id;

    @NotEmpty
    private String pushToken;

    @NotNull
    private Long personId;

    @NotNull
    private String device;

    @NotNull
    @CreatedDate
    private Date createdAt;

    @NotNull
    @LastModifiedDate
    private Date updatedAt;
}
