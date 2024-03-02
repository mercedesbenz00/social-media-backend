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
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(indexes = {
    @Index(name = "person_ban_expired_at_idx",  columnList="expiredAt", unique = false),
    @Index(name = "person_ban_unique_idx", columnList = "author_id,banned_person_id", unique = true)
})
@EntityListeners(AuditingEntityListener.class)
public class PersonBan {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "person_ban_seq_gen")
    @SequenceGenerator(name = "person_ban_seq_gen", sequenceName = "person_ban_seq_gen", allocationSize = 1)
    private Long id;

    @NotNull
    @CreatedDate
    private Date createdAt;

    @ManyToOne
    @NotNull
    private Person author;

    @ManyToOne
    @NotNull
    private Person bannedPerson;

    @NotNull
    private Date expiredAt;

    @Length(max = 1000)
    private String reason;

    private Long reasonId;
}
