package iq.earthlink.social.groupservice.group.rest;

import io.swagger.annotations.ApiModelProperty;
import iq.earthlink.social.groupservice.group.GroupMemberStatus;
import iq.earthlink.social.groupservice.group.rest.enumeration.ApprovalState;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JsonMemberPermission {
    private Long id;
    private Long personId;
    private Long groupId;

    @ApiModelProperty(value = "The status which person has in the group", example = "ADMIN")
    private List<GroupMemberStatus> statuses;

    @ApiModelProperty(value = "The approval state which person has in the group", example = "PENDING")
    private ApprovalState state;
}
