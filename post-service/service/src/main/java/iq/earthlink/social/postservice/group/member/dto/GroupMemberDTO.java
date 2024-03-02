package iq.earthlink.social.postservice.group.member.dto;

import iq.earthlink.social.postservice.group.member.enumeration.Permission;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupMemberDTO {
    private Long personId;
    private Long groupId;
    private List<Permission> permissions;
    private Date createdAt;
}
