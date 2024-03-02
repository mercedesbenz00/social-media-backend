package iq.earthlink.social.groupservice.category.dto;

import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CategoryRequest {

    @ApiModelProperty("The name of the category")
    @NotBlank
    private String name;

    private Map<String, String> localizations;

    @ApiModelProperty("The parent category id")
    private Long parentCategoryId;
}
