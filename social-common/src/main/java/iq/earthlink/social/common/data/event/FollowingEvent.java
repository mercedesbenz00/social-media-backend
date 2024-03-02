package iq.earthlink.social.common.data.event;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Builder
public class FollowingEvent implements HasPersonId, Serializable {
    @Override
    @JsonIgnore
    public Long getPersonId() {
        return getSubscribedToId();
    }

    public enum FollowingEventType {
        FOLLOW, // User follows another user
        UNFOLLOW; // User unfollows another user
    }

    public static final String FOLLOWING_GROUP_ID = "followingEventGroup";
    public static final String FOLLOWING_TOPIC = "followingEventTopic";
    public static final String FOLLOWING_EXCHANGE = "followingEventExchange";

    @NotNull
    private Long subscriberId;
    @NotNull
    private Long subscribedToId;

    @NotNull
    private FollowingEventType type;
}
