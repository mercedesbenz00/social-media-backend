package iq.earthlink.social.groupservice.group.dto;

import iq.earthlink.social.groupservice.group.GroupMemberStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GroupMemberEventDTO {
    private Long personId;
    private Long groupId;
    private Date createdAt;
    private List<GroupMemberStatus> permissions;
    private GroupMemberEventType eventType;
}
