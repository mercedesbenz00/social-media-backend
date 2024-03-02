package iq.earthlink.social.common.data.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GroupVisitEvent implements Serializable {
    public static final String GROUP_VISIT_GROUP_ID = "groupVisitGroup";
    public static final String GROUP_VISIT_TOPIC = "groupVisitTopic";
    public static final String GROUP_VISIT_EXCHANGE = "groupVisitExchange";

    private Long groupId;
    private String visitorId;
    private Date visitDate;
}
