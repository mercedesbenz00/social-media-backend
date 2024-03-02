package iq.earthlink.social.postservice.data.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@ApiModel(description = "Request for creating post complaints")
public class CommentComplaintNewRequest {
    @ApiModelProperty("Reason why user thinks it should be moderated")
    private String reason;

    @ApiModelProperty("Comment id")
    private Long commentId;
}
