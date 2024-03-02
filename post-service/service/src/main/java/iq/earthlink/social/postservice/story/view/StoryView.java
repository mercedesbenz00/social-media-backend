package iq.earthlink.social.postservice.story.view;

import com.fasterxml.jackson.annotation.JsonIgnore;
import iq.earthlink.social.postservice.story.model.Story;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
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
    @Index(name = "story_view_unique_idx", columnList = "story_id, viewerId", unique = true)
})
@EntityListeners(AuditingEntityListener.class)
public class StoryView {

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "story_view_seq_gen")
  @SequenceGenerator(name = "story_view_seq_gen", sequenceName = "story_view_seq_gen", allocationSize = 1)
  private Long id;

  @JsonIgnore
  @NotNull
  @ManyToOne
  private Story story;

  @NotNull
  private Long viewerId;

  @NotNull
  @CreatedDate
  private Date createdAt;

  @LastModifiedDate
  @NotNull
  private Date updatedAt;

}
