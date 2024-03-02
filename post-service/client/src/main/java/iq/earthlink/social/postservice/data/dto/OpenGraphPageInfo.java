package iq.earthlink.social.postservice.data.dto;

import io.swagger.annotations.ApiModel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ApiModel(description = "Extracted open graph information for a webpage")
public class OpenGraphPageInfo {
    private String title;
    private String imageUrl;
}
