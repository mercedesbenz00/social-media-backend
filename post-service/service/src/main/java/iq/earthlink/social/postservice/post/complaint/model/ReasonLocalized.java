package iq.earthlink.social.postservice.post.complaint.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import javax.persistence.*;
import java.io.Serializable;

@Entity
@Data
@Builder
@Table(uniqueConstraints = {@UniqueConstraint(columnNames = {"name", "locale"})})
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(exclude = "reason")
@ToString(exclude = "reason")
@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class ReasonLocalized implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "reason_localized_seq_gen")
    @SequenceGenerator(name = "reason_localized_seq_gen", sequenceName = "reason_localized_seq_gen", allocationSize = 1)
    private Long id;

    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "reason_id")
    private Reason reason;

    private String locale;

    private String name;
}
