package iq.earthlink.social.postservice.post.rest;

import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReasonRequest {
    @ApiModelProperty("The reason name")
    @NotBlank
    private String name;
    private Map<String, String> localizations;
}
