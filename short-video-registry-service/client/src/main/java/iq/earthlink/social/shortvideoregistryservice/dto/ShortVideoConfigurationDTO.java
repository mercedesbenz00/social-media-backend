package iq.earthlink.social.shortvideoregistryservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import iq.earthlink.social.classes.enumeration.PrivacyLevel;
import lombok.*;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Objects;

/**
 * ShortVideoConfigurationDTO
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShortVideoConfigurationDTO {

    /**
     * The owner of the short video configuration
     */
    @NotNull
    @Schema(name = "personId", description = "The owner of the short video configuration", required = true)
    @JsonProperty("personId")
    private Long personId = null;

    /**
     * Defines if commenting allowed for the short video
     */
    @Schema(name = "commentsAllowed", example = "true", description = "Defines if commenting allowed for the short video", required = false)
    @JsonProperty("commentsAllowed")
    private Boolean commentsAllowed;

    /**
     * Get privacyLevel
     */
    @NotNull
    @Valid
    @Schema(name = "privacyLevel", required = true)
    @JsonProperty("privacyLevel")
    private PrivacyLevel privacyLevel;

    /**
     * required if privacyLevel SELECTED_USERS
     */
    @JsonProperty("selectedUsers")
    @Schema(name = "selectedUsers", example = "[1,2]", description = "required if privacyLevel SELECTED_USERS", required = false)
    @Valid
    private List<Long> selectedUsers = null;

    /**
     * required if privacyLevel SELECTED_GROUPS
     */
    @Schema(name = "selectedGroups", example = "[1,2]", description = "required if privacyLevel SELECTED_GROUPS", required = false)
    @JsonProperty("selectedGroups")
    @Valid
    private List<Long> selectedGroups = null;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ShortVideoConfigurationDTO shortVideoConfiguration = (ShortVideoConfigurationDTO) o;
        return Objects.equals(this.personId, shortVideoConfiguration.personId) &&
                Objects.equals(this.commentsAllowed, shortVideoConfiguration.commentsAllowed) &&
                Objects.equals(this.privacyLevel, shortVideoConfiguration.privacyLevel) &&
                Objects.equals(this.selectedUsers, shortVideoConfiguration.selectedUsers) &&
                Objects.equals(this.selectedGroups, shortVideoConfiguration.selectedGroups);
    }

    @Override
    public int hashCode() {
        return Objects.hash(personId, commentsAllowed, privacyLevel, selectedUsers, selectedGroups);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class ShortVideoConfigurationDTO {\n");
        sb.append("    personId: ").append(toIndentedString(personId)).append("\n");
        sb.append("    commentsAllowed: ").append(toIndentedString(commentsAllowed)).append("\n");
        sb.append("    privacyLevel: ").append(toIndentedString(privacyLevel)).append("\n");
        sb.append("    selectedUsers: ").append(toIndentedString(selectedUsers)).append("\n");
        sb.append("    selectedGroups: ").append(toIndentedString(selectedGroups)).append("\n");
        sb.append("}");
        return sb.toString();
    }

    /**
     * Convert the given object to string with each line indented by 4 spaces
     * (except the first line).
     */
    private String toIndentedString(Object o) {
        if (o == null) {
            return "null";
        }
        return o.toString().replace("\n", "\n    ");
    }
}


