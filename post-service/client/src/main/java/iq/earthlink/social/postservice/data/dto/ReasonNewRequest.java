package iq.earthlink.social.postservice.data.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@ApiModel(description = "Request for creating/updating reasons")
@NoArgsConstructor
public class ReasonNewRequest {

    @ApiModelProperty("id of reason")
    private String id;

    @ApiModelProperty("map with reason messages for different languages")
    private Map<String, String> messages;
}
