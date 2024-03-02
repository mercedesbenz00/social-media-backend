package iq.earthlink.social.groupservice.group;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
@EntityListeners(AuditingEntityListener.class)
@Cacheable
@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class UserGroupStats {

  public static final String SCORE_CONST = "score";
  public static final String MEMBERS_COUNT_CONST = "membersCount";
  public static final String PUBLISHED_POSTS_COUNT_CONST = "publishedPostsCount";

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "user_group_stats_gen")
  @SequenceGenerator(name = "user_group_stats_gen", sequenceName = "user_group_stats_gen", allocationSize = 1)
  private Long id;

  @OneToOne
  private UserGroup group;

  @Column(columnDefinition = "int8 not null default 0")
  private long publishedPostsCount;

  @Column(columnDefinition = "int8 not null default 0")
  private long membersCount;

  @Column(columnDefinition="int8 not null default 0")
  private long score;

}
