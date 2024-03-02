package iq.earthlink.social.userfeedaggregatorservice.config;

import io.micrometer.core.aop.TimedAspect;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.composite.CompositeMeterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan(basePackages = {
        "iq.earthlink.social.exception",
        "iq.earthlink.social.security",
        "iq.earthlink.social.util"
})
public class CommonConfig {
    @Bean
    public MeterRegistry getMeterRegistry() {
        return new CompositeMeterRegistry();
    }

    @Bean
    public TimedAspect timedAspect(MeterRegistry registry) {
        return new TimedAspect(registry);
    }
}
