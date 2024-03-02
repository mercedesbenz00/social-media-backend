package iq.earthlink.social.postservice.post.vote;

import iq.earthlink.social.postservice.post.comment.Comment;
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
public class CommentVotePK implements Serializable {

    @Column(insertable = false, updatable = false, nullable = false)
    private Long personId;

    @ManyToOne
    @JoinColumn(insertable = false, updatable = false)
    private Comment comment;

    public CommentVotePK() {
    }

    public CommentVotePK(Long personId, Comment comment) {
        this.personId = personId;
        this.comment = comment;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) return false;
        CommentVotePK that = (CommentVotePK) o;
        return Objects.equals(personId, that.personId)
                && Objects.equals(comment, that.comment);
    }

    @Override
    public int hashCode() {
        return Objects.hash(personId, comment);
    }
}
