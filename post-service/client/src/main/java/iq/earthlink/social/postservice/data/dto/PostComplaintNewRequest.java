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
public class PostComplaintNewRequest {
    @ApiModelProperty("Reason why user thinks it should be moderated (if 'OTHER' as reasonId is selected")
    private String reasonOther;

    @ApiModelProperty("Reason why user thinks it should be moderated")
    private String reasonId;

    @ApiModelProperty("Post id")
    private Long postId;
}
