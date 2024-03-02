package iq.earthlink.social.postservice.group.dto;


import iq.earthlink.social.classes.data.dto.JsonMediaFile;
import iq.earthlink.social.postservice.group.AccessType;
import iq.earthlink.social.postservice.group.PostingPermission;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupEventDTO {
    private Long id; // groupId from the group service
    private String name;
    private JsonMediaFile avatar;
    private AccessType accessType;
    private PostingPermission postingPermission;
    private Date createdAt;
}
