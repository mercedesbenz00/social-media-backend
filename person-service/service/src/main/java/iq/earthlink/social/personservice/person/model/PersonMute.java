package iq.earthlink.social.personservice.person.model;

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
@Table(uniqueConstraints=@UniqueConstraint(columnNames={"person_id","muted_person_id"}))
@EntityListeners(AuditingEntityListener.class)
public class PersonMute {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "person_mute_seq_gen")
    @SequenceGenerator(name = "person_mute_seq_gen", sequenceName = "person_mute_seq_gen", allocationSize = 1)
    private Long id;

    @ManyToOne
    @NotNull
    @JoinColumn(name = "person_id")
    private Person person;

    @ManyToOne
    @NotNull
    @JoinColumn(name = "muted_person_id")
    private Person mutedPerson;

    @NotNull
    @CreatedDate
    private Date createdAt;
}
