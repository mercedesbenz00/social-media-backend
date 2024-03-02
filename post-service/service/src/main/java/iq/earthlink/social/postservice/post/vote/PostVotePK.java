package iq.earthlink.social.postservice.post.vote;

import iq.earthlink.social.postservice.post.model.Post;
import lombok.Getter;
import org.hibernate.Hibernate;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
@Getter
public class PostVotePK implements Serializable {

  @Column(insertable = false, updatable = false, nullable = false)
  private Long personId;

  @ManyToOne
  @JoinColumn(insertable = false, updatable = false)
  private Post post;

  public PostVotePK() {}

  public PostVotePK(Long personId, Post post) {
    this.personId = personId;
    this.post = post;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) return false;
    PostVotePK that = (PostVotePK) o;
    return Objects.equals(personId, that.personId)
            && Objects.equals(post, that.post);
  }

  @Override
  public int hashCode() {
    return Objects.hash(personId, post);
  }
}
