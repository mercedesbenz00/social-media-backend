package iq.earthlink.social.postservice.post.vote;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.validation.constraints.NotNull;
import java.util.Date;
import java.util.Objects;

@Entity
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class PostVote {

  @EmbeddedId
  @NotNull
  private PostVotePK id;

  @CreatedDate
  private Date createdAt;

  @NotNull
  private int voteType;

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    PostVote postVote = (PostVote) o;
    return voteType == postVote.voteType && Objects.equals(id, postVote.id) && Objects.equals(createdAt, postVote.createdAt);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, createdAt, voteType);
  }
}
