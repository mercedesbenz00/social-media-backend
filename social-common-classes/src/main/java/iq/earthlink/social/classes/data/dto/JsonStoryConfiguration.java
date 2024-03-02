package iq.earthlink.social.classes.data.dto;

import iq.earthlink.social.classes.enumeration.StoryAccessType;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import java.util.Set;

@Data
@NoArgsConstructor
public class JsonStoryConfiguration {

    @NotNull
//    @ApiModelProperty(value = "User story access type", example = "'PUBLIC', 'ALL_FOLLOWERS', or 'SELECTED_FOLLOWERS'")
    private StoryAccessType accessType;

    public void setAccessType(String accessType) {
        this.accessType = StoryAccessType.valueOf(accessType.toUpperCase());
    }

    public void setAccessType(StoryAccessType accessType) {
        this.accessType = accessType;
    }

//    @ApiModelProperty(value = "The allowed user ids to view stories of current user")
    private Set<Long> allowedFollowersIds;
}
