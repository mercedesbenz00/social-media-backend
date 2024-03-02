package iq.earthlink.social.postservice.post.collection;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.annotations.ApiModel;
import iq.earthlink.social.postservice.post.model.Post;
import lombok.*;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.validator.constraints.Length;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = {"id", "authorId"})
@ApiModel(description = "Post collection of the group")
@EntityListeners(AuditingEntityListener.class)
@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class GroupPostCollection {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "group_post_collection_seq_gen")
    @SequenceGenerator(name = "group_post_collection_seq_gen", sequenceName = "group_post_collection_seq_gen", allocationSize = 1)
    private Long id;

    @NotNull
    @Column(nullable = false)
    private Long authorId;

    @NotNull
    @Column(nullable = false)
    private Long groupId;

    @NotNull
    @Length(max = 255)
    private String name;

    @NotNull
    @Column(nullable = false)
    @CreatedDate
    private Date createdAt;

    @ManyToMany(fetch = FetchType.EAGER)
    private Set<Post> posts = new HashSet<>();

    @JsonIgnore
    private boolean defaultCollection;
}
