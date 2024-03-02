package iq.earthlink.social.postservice.post.rest;

import io.swagger.annotations.ApiModelProperty;
import iq.earthlink.social.classes.data.validation.NewEntityGroup;
import iq.earthlink.social.postservice.post.GroupPostCollectionData;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
public class JsonGroupPostCollectionData implements GroupPostCollectionData {

        @ApiModelProperty("The group id who owns the collection")
        @NotNull
        private Long groupId;

        @ApiModelProperty("The collection name")
        @NotBlank(groups = NewEntityGroup.class)
        private String name;

        @ApiModelProperty("Indicates if the collection should be default")
        private boolean defaultCollection;
}
