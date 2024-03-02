package iq.earthlink.social.postservice.post.comment;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.Range;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Data
@ConfigurationProperties("social.postservice.comments")
@Component
@Validated
@NoArgsConstructor
public class CommentFileProperties {

    @Range(min = 1, max = 10000)
    private String imageMaxSize;

    @Range(min = 1, max = 10000)
    private String videoMaxSize;

}
