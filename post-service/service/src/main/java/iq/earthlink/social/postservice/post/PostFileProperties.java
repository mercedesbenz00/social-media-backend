package iq.earthlink.social.postservice.post;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.Range;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Data
@ConfigurationProperties("social.postservice.files")
@Component
@Validated
@NoArgsConstructor
public class PostFileProperties {

    @Range(min = 1, max = 10000)
    private String imageMaxCount;

    @Range(min = 1, max = 10000)
    private String imageMaxSize;

    @Range(min = 1, max = 10000)
    private String videoMaxCount;

    @Range(min = 1, max = 10000)
    private String videoMaxSize;

}
