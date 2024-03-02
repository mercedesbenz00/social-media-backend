package iq.earthlink.social.postservice.post.rest;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import iq.earthlink.social.classes.enumeration.PostLinkType;
import iq.earthlink.social.classes.enumeration.PostState;
import iq.earthlink.social.postservice.post.PostData;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@ApiModel("Contains post specific information")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JsonPostData implements PostData {

  @ApiModelProperty(value = "The post title", hidden = true)
  private String title;

  @ApiModelProperty("The post content, required only for 'TEXT' post. May be used for storing post description for the VIDEO, IMAGE posts")
  private String content;

  @ApiModelProperty("The group id for which post belongs. If not specified - then post belongs to the person who creating post")
  private Long userGroupId;

  @ApiModelProperty(value = "The flag that indicates if the comments allowed for the post. Default: true")
  private Boolean commentsAllowed;

  @ApiModelProperty("The post state property can be used only on post update and only by ADMIN user or group moderator/admin")
  private PostState state;

  @ApiModelProperty("The flag that indicates that post should be pinned in the group ")
  private Boolean shouldPin;

  @ApiModelProperty("The flag that indicates that post reshared from the given post Id ")
  private Long repostedFromId;

  @ApiModelProperty("IDs of persons mentioned in the post")
  private List<Long> mentionedPersonIds;

  @ApiModelProperty("LinkMeta object contains information about post link")
  private LinkMeta linkMeta;
}
