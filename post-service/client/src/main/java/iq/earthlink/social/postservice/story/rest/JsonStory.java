package iq.earthlink.social.postservice.story.rest;

import io.swagger.annotations.ApiModelProperty;
import iq.earthlink.social.classes.data.dto.JsonMediaFile;
import iq.earthlink.social.classes.enumeration.StoryAccessType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import java.util.Date;
import java.util.Set;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class JsonStory {
  private Long id;
  private Long authorId;
  private JsonMediaFile file;
  private Date createdAt;
  private String personReferences;
  @NotNull
  @ApiModelProperty(value = "User story access type", example = "'PUBLIC', 'ALL_FOLLOWERS', or 'SELECTED_FOLLOWERS'")
  private StoryAccessType accessType;
  @ApiModelProperty(value = "The allowed user ids to view stories of current user")
  private Set<Long> selectedFollowerIds;
}
