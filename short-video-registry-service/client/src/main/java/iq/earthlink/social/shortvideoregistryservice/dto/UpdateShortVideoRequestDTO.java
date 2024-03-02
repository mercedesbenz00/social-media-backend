package iq.earthlink.social.shortvideoregistryservice.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import iq.earthlink.social.classes.enumeration.PrivacyLevel;
import lombok.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateShortVideoRequestDTO {
    private String title;
    private PrivacyLevel privacyLevel;
    private Set<Long> selectedGroups = new HashSet<>();
    private Set<Long> selectedUsers = new HashSet<>();
    private Boolean commentsAllowed;
    private Set<ShortVideoCategoryDTO> categories = new HashSet<>();

    @JsonIgnore
    public ShortVideoConfigurationDTO getConfiguration() {
        return ShortVideoConfigurationDTO.builder()
                .privacyLevel(this.getPrivacyLevel())
                .selectedGroups(this.getSelectedGroups() != null ? new ArrayList<>(this.getSelectedGroups()) : null)
                .selectedUsers(this.getSelectedUsers() != null ? new ArrayList<>(this.getSelectedUsers()) : null)
                .commentsAllowed(this.getCommentsAllowed())
                .build();
    }
}
