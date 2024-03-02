package iq.earthlink.social.postservice.post;

import com.rabbitmq.client.Channel;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Data
@Component
@NoArgsConstructor
public class PostStatisticsDTO {

    private long postId;
    private long commentsDelta = 0;
    private long commentsUpvotesDelta = 0;
    private long commentsDownvotesDelta = 0;
    private long upvotesDelta = 0;
    private long downvotesDelta = 0;
    private Date lastActivityAt;
    @ToString.Exclude
    private Map<Channel, Set<Long>> channelTagsMap = new HashMap<>();
}
