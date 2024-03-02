package iq.earthlink.social.groupservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LocalizationConfig {

    @Bean
    public AcceptHeaderResolver localeResolver(AcceptHeaderResolver acceptHeaderResolver) {
        return acceptHeaderResolver;
    }
}
