package iq.earthlink.social.postservice.post.rest;

import io.swagger.annotations.ApiModelProperty;
import iq.earthlink.social.classes.data.dto.JsonMediaFile;
import iq.earthlink.social.classes.enumeration.PostState;
import iq.earthlink.social.classes.enumeration.PostType;
import iq.earthlink.social.groupservice.group.rest.enumeration.AccessType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class JsonPost {
  private Long id;
  private UUID postUuid;
  private String content;
  @ApiModelProperty(value = "The timestamp when post has been created", dataType = "Long.class", example = "123352312352")
  private Date createdAt;
  @ApiModelProperty(value = "The timestamp when post has been published", dataType = "Long.class", example = "123352312352")
  private Date publishedAt;
  private Date lastActivityAt;
  private Long authorId;
  private UUID authorUuid;
  private String authorDisplayName;
  private Long userGroupId;
  private AccessType userGroupType;
  private PostType postType;
  private JsonPost repostedFrom;
  private PostState state;
  private String stateDisplayName;
  private boolean commentsAllowed;
  private JsonVoteCount totalVotes;
  private double score;
  private List<JsonMediaFile> files;
  private boolean pinned;
  private Long commentsCount;
  private Map<String, String> linkMeta;
}
