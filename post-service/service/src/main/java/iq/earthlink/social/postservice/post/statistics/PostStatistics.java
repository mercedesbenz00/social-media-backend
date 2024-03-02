package iq.earthlink.social.postservice.post.statistics;

import iq.earthlink.social.postservice.post.model.Post;
import lombok.*;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.Date;

@Entity
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"post_id"}))
@EntityListeners(AuditingEntityListener.class)
@Cacheable
@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class PostStatistics {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "post_stat_seq_gen")
    @SequenceGenerator(name = "post_stat_seq_gen", sequenceName = "post_stat_seq_gen", allocationSize = 1)
    private Long id;

    @NotNull
    @OneToOne
    private Post post;

    @Column(columnDefinition = "decimal default 0.0")
    private double score;

    @Column(columnDefinition = "int8 not null default 0")
    private long commentsCount;

    @Column(columnDefinition = "int8 not null default 0")
    private long commentsUpvotesCount;

    @Column(columnDefinition = "int8 not null default 0")
    private long commentsDownvotesCount;

    @Column(columnDefinition = "int8 not null default 0")
    private long upvotesCount;

    @Column(columnDefinition = "int8 not null default 0")
    private long downvotesCount;

    /**
     * When somebody voted/commented the post
     */
    private Date lastActivityAt;

}
