package iq.earthlink.social.groupservice.group.rest;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.Date;

@Data
public class JsonTag {
    private Long id;

    @ApiModelProperty(value = "Unique tag name", dataType = "String.class")
    private String tag;

    @ApiModelProperty(
            value = "The timestamp when tag was created",
            dataType = "Long.class",
            example = "123123535234")
    private Date createdAt;

    private Long authorId;
}
