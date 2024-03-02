package iq.earthlink.social.common.data.event;

import lombok.*;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Builder
public class PersonInterestChangedEvent implements Serializable {
    public enum GroupSubscriptionEventType {
        SUBSCRIBE, // User subscribes interest
        UNSUBSCRIBE; // User unsubscribes interest
    }

    public static final String PERSON_SUBSCRIBE_INTEREST_GROUP_ID = "personSubscribeInterestEventGroup";
    public static final String PERSON_SUBSCRIBE_INTEREST_TOPIC = "personSubscribeInterestEventTopic";
    public static final String PERSON_SUBSCRIBE_INTEREST_EXCHANGE = "personSubscribeInterestEventExchange";

    private String personId;
    private Long interestId;
    private int interestCount;
}
