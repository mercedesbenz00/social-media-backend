package iq.earthlink.social.personservice.person.model;

import iq.earthlink.social.classes.enumeration.RequestState;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.Date;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class ChangeEmailRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "change_email_request_seq_gen")
    @SequenceGenerator(name = "change_email_request_seq_gen", sequenceName = "change_email_request_seq_gen", allocationSize = 1)
    private Long id;

    @OneToOne(cascade = CascadeType.REMOVE, orphanRemoval = true)
    private Person person;

    @NotNull
    @Column(columnDefinition = "TEXT")
    private String newEmail;

    @NotNull
    @Column(columnDefinition = "TEXT")
    private String oldEmail;

    @NotNull
    @Column(columnDefinition = "TEXT")
    private String token;

    @Enumerated(EnumType.STRING)
    private RequestState state = RequestState.EXPIRED;

    @NotNull
    @CreatedDate
    private Date createdAt;

    private Date expiresAt;
}
