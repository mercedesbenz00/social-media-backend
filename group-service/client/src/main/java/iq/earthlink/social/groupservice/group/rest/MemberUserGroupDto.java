package iq.earthlink.social.groupservice.group.rest;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import iq.earthlink.social.groupservice.group.rest.enumeration.ApprovalState;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ApiModel(description = "Member User Group DTO")
@FieldNameConstants
@Builder
public class MemberUserGroupDto {

  @ApiModelProperty("User group object")
  private UserGroupDto group;

  @ApiModelProperty(value = "The timestamp when member has joined the group", dataType = "Long.class", example = "1234352343123")
  private Date memberSince;

  @ApiModelProperty(value = "The timestamp when member has visited the group", dataType = "Long.class", example = "1234352343123")
  private Date visitedAt;

  @ApiModelProperty(value = "Number of posts that member has published in the group")
  private long publishedPostsCount;

  @ApiModelProperty(value = "The joining state of the member")
  private ApprovalState memberState;
}
