package iq.earthlink.social.shortvideoregistryservice.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import iq.earthlink.social.classes.enumeration.PrivacyLevel;
import lombok.*;

import java.sql.Timestamp;
import java.util.*;

/**
 * ShortVideo DTO
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShortVideoDTO {
    private UUID id;
    private String title;
    private String bucket;
    private String url;
    private String thumbnailUrl;
    private Long authorId;
    private PrivacyLevel privacyLevel;
    private Set<Long> selectedGroups = new HashSet<>();
    private Set<Long> selectedUsers = new HashSet<>();
    private Boolean commentsAllowed;
    private Set<ShortVideoCategoryDTO> categories = new HashSet<>();
    private Map<String, Object> metadata;
    private ShortVideoStatsDTO stats;
    private Timestamp createdAt;
    private Timestamp updatedAt;

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