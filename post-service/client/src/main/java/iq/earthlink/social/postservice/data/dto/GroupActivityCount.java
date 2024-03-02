package iq.earthlink.social.postservice.data.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class GroupActivityCount {
    private long groupId;
    private long activityCount;
}
