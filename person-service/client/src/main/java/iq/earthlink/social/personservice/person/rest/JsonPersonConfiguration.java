package iq.earthlink.social.personservice.person.rest;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import iq.earthlink.social.classes.data.dto.JsonStoryConfiguration;
import iq.earthlink.social.classes.enumeration.LocalizationType;
import iq.earthlink.social.classes.enumeration.ThemeType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@ApiModel("Configuration of application for a user")
@Data
@Builder
@JsonInclude(NON_NULL)
@NoArgsConstructor
@AllArgsConstructor
public class JsonPersonConfiguration {

    Long personId;

    @ApiModelProperty("Specifies if user wants to mute sound of notifications")
    private boolean notificationMute;

    @ApiModelProperty("User story configurations")
    private JsonStoryConfiguration story;

    @ApiModelProperty("Localization preference")
    private LocalizationType localization;

    @ApiModelProperty("Theme preference")
    private ThemeType theme;
}
