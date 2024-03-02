package iq.earthlink.social.personservice.person.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CacheConcurrencyStrategy;
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
@Table(uniqueConstraints=@UniqueConstraint(columnNames={"subscriber_id","subscribed_to_id"}))
@EntityListeners(AuditingEntityListener.class)
@Cacheable
@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class Following {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "following_seq_gen")
    @SequenceGenerator(name = "following_seq_gen", sequenceName = "following_seq_gen", allocationSize = 1)
    private Long id;

    @ManyToOne
    @NotNull
    @JoinColumn(name="subscriber_id")
    private Person subscriber;

    @ManyToOne
    @NotNull
    @JoinColumn(name="subscribed_to_id")
    private Person subscribedTo;

    @NotNull
    @CreatedDate
    private Date createdAt;
}
