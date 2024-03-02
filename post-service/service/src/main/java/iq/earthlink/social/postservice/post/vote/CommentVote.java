package iq.earthlink.social.postservice.post.vote;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
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
@RequiredArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class CommentVote {

    public CommentVote(CommentVotePK id, Date createdAt, int voteType) {
        this.id = id;
        this.createdAt = createdAt;
        this.voteType = voteType;
    }

    @EmbeddedId
    @NotNull
    private CommentVotePK id;

    @CreatedDate
    private Date createdAt;

    @NotNull
    private int voteType;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CommentVote commentVote = (CommentVote) o;
        return voteType == commentVote.voteType && Objects.equals(id, commentVote.id) && Objects.equals(createdAt, commentVote.createdAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, createdAt, voteType);
    }
}
