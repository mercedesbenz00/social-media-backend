package iq.earthlink.social.groupservice.group;

import com.rabbitmq.client.Channel;
import lombok.Data;
import lombok.ToString;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Data
@Component
public class GroupStatistics {

    private long userGroupId;
    private long membersDelta = 0;
    private long publishedPostsDelta = 0;
    @ToString.Exclude
    private Map<Channel, Set<Long>> channelTagsMap = new HashMap<>();
}
