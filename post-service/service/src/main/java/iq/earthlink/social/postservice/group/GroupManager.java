package iq.earthlink.social.postservice.group;

import iq.earthlink.social.postservice.group.dto.GroupDTO;
import iq.earthlink.social.postservice.group.dto.GroupEventDTO;

import java.util.List;

public interface GroupManager {
    List<GroupDTO> getGroupsByIds(List<Long> groupIds);
    GroupDTO getGroupById(Long groupId);
    void saveGroup(GroupEventDTO groupEventDTO);
    void updateGroup(GroupEventDTO groupEventDTO);

    List<Long> getMyGroupIds(Long personId, List<Long> groupIds);

    boolean hasAccessToGroup(Long personId, boolean isAdmin, Long groupId);

    Long getCount();
    void saveAllGroups(List<GroupEventDTO> groupEventDTOS);
}
