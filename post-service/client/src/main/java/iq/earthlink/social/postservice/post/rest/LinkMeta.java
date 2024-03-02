package iq.earthlink.social.postservice.post.rest;

import io.swagger.annotations.ApiModelProperty;
import iq.earthlink.social.classes.enumeration.PostLinkType;
import lombok.Data;

@Data
public class LinkMeta {
    @ApiModelProperty("Post link type")
    private PostLinkType type;

    @ApiModelProperty("Depends on link type: for YOUTUBE link type it will be youtube video ID")
    private String mediaId;

    @ApiModelProperty("For general links")
    private String title;

    @ApiModelProperty("For general links")
    private String description;

    @ApiModelProperty("For general links")
    private String thumbnail;

    @ApiModelProperty("For general links")
    private String url;
}
