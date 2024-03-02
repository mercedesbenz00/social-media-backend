package iq.earthlink.social.postservice.post;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.hibernate.validator.constraints.Range;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Data
@ConfigurationProperties("social.postservice.statistics")
@Component
@Validated
@NoArgsConstructor
public class PostStatisticsProperties {

    @NonNull
    @Range(min = 1, max = 10000)
    private String activityPopularDaysCount;

    @NonNull
    @Range(min = 1, max = 10000)
    private String activityTrendingDaysCount;
}
