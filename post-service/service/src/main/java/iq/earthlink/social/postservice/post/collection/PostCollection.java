package iq.earthlink.social.postservice.post.collection;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;
import org.hibernate.annotations.CacheConcurrencyStrategy;
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
@EqualsAndHashCode(of = "id")
@EntityListeners(AuditingEntityListener.class)
@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class PostCollection {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "post_collection_seq_gen")
    @SequenceGenerator(name = "post_collection_seq_gen", sequenceName = "post_collection_seq_gen", allocationSize = 1)
    private Long id;

    @NotNull
    @Column(nullable = false)
    private Long ownerId;

    @JsonIgnore
    private boolean isPublic;

    @NotNull
    @Length(max = 255)
    private String name;

    @NotNull
    @CreatedDate
    private Date createdAt;
}
