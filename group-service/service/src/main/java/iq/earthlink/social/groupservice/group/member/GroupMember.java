package iq.earthlink.social.groupservice.group.member;

import iq.earthlink.social.groupservice.group.UserGroup;
import iq.earthlink.social.groupservice.group.rest.enumeration.ApprovalState;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.Date;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(indexes = {
    @Index(name = "group_member_unique_idx", columnList = "group_id, personId", unique = true)
})
@EntityListeners(AuditingEntityListener.class)
@Cacheable
@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class GroupMember {

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "group_member_seq")
  @SequenceGenerator(name = "group_member_seq", sequenceName = "group_member_seq", allocationSize = 1)
  private Long id;

  @NotNull
  private Long personId;

  private String displayName;

  @NotNull
  @ManyToOne
  private UserGroup group;

  @NotNull
  @CreatedDate
  private Date createdAt;

  @LastModifiedDate
  private Date updatedAt;

//  @NotNull
  private Date visitedAt;

  @NotNull
  @Enumerated(EnumType.STRING)
  private ApprovalState state;

  private long publishedPostsCount;
}
