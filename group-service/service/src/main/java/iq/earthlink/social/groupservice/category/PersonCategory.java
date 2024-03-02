package iq.earthlink.social.groupservice.category;

import lombok.Data;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

@Data
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(
        indexes = {
                @Index(name = "person_id_idx", columnList = "personId", unique = true),
        }
)
@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class PersonCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "person_cat_seq_gen")
    @SequenceGenerator(name = "person_cat_seq_gen", sequenceName = "person_cat_seq_gen", allocationSize = 1)
    private Long id;

    @NotNull
    private Long personId;

    @ManyToMany
    private Set<Category> categories = new HashSet<>();

    @NotNull
    @CreatedDate
    private Date createdAt;
}
