package iq.earthlink.social.common.data.event;

import lombok.*;

import java.io.Serializable;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Builder
public class GroupSubscriptionEvent implements HasPersonId, Serializable {
    public enum GroupSubscriptionEventType {
        SUBSCRIBE, // User subscribes group
        UNSUBSCRIBE; // User unsubscribes group
    }

    public static final String GROUP_SUBSCRIBE_GROUP_ID = "groupSubscribeventGroup";
    public static final String GROUP_SUBSCRIBE_TOPIC = "groupSubscribeEventTopic";
    public static final String GROUP_SUBSCRIBE_EXCHANGE = "groupSubscribeEventExchange";

    private Long personId;

    private Long groupId;

    private GroupSubscriptionEventType type;

    private int personGroupCount;
}
