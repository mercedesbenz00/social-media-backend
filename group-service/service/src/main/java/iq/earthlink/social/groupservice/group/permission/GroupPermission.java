package iq.earthlink.social.groupservice.group.permission;

import iq.earthlink.social.groupservice.group.GroupMemberStatus;
import iq.earthlink.social.groupservice.group.UserGroup;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import javax.validation.constraints.NotNull;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Table(
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_group_id", "personId", "permission"})
    }
)
public class GroupPermission {

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "group_perm_seq")
  @SequenceGenerator(name = "group_perm_seq", sequenceName = "group_perm_seq", allocationSize = 1)
  private Long id;

  @ManyToOne
  @NotNull
  @JoinColumn(name = "user_group_id")
  private UserGroup userGroup;

  @NotNull
  private Long personId;

  @NotNull
  private Long authorId;

  @NotNull
  @Enumerated(EnumType.STRING)
  private GroupMemberStatus permission;
}
