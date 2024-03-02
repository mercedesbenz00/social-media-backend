package iq.earthlink.social.personservice.person.model;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.Length;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.Date;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class PersonComplaint {

    public enum PersonComplaintState {
        PENDING,
        REJECTED, // Complaint is rejected
        USER_BANNED_GROUP, // User is banned from group
        USER_BANNED; // User is banned
    }

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "person_complaint_seq_gen")
    @SequenceGenerator(name = "person_complaint_seq_gen", sequenceName = "person_complaint_seq_gen", allocationSize = 1)
    private Long id;

    @ManyToOne
    @NotNull
    @JoinColumn(name = "person_id")
    private Person person;

    private Long userGroupId;

    @NotNull
    @CreatedDate
    private Date createdAt;

    @NotNull
    @ManyToOne
    private Person owner;

    private Long reasonId;

    @Length(max = 1000)
    @Column(length = 1000)
    private String reason;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "char(20) default 'PENDING'")
    private PersonComplaintState state;

    /**
     * The person identifier who resolved this complaint.
     */
    private Long resolverId;

    @Length(max = 1000)
    @Column(length = 1000)
    private String resolvingText;

    private Date resolvingDate;
}
