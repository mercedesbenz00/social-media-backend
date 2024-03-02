package iq.earthlink.social.postservice.post.rest;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class PostStatisticsGap {
  private Long postId;
  @ApiModelProperty("Current number of post comments")
  private Long currentCommentsCount;
  @ApiModelProperty("Computed number of post comments")
  private Long computedCommentsCount;

  public PostStatisticsGap(Long postId, Long currentCommentsCount, Long computedCommentsCount) {
    this.postId = postId;
    this.currentCommentsCount = currentCommentsCount;
    this.computedCommentsCount = computedCommentsCount;
  }
}
