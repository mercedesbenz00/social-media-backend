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
@Table(uniqueConstraints=@UniqueConstraint(columnNames={"person_id","blocked_person_id"}))
@EntityListeners(AuditingEntityListener.class)
public class PersonBlock {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "person_block_seq_gen")
    @SequenceGenerator(name = "person_block_seq_gen", sequenceName = "person_block_seq_gen", allocationSize = 1)
    private Long id;

    @ManyToOne
    @NotNull
    @JoinColumn(name = "person_id")
    private Person person;

    @ManyToOne
    @NotNull
    @JoinColumn(name="blocked_person_id")
    private Person blockedPerson;

    @NotNull
    @CreatedDate
    private Date createdAt;
}
