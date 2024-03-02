package iq.earthlink.social.posteventprocessorservice.event;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Data
@ConfigurationProperties("social.event-weight")
@Component
@Validated
@NoArgsConstructor
public class EventTypeProperties {

    private Long postViewed;

    private Long postLiked;

    private Long postCommented;

}
