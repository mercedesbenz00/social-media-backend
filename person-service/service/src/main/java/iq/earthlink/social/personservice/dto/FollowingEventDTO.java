package iq.earthlink.social.personservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FollowingEventDTO {
    private Long followerId;
    private Long followedId;
    private FollowingEventType eventType;
}
