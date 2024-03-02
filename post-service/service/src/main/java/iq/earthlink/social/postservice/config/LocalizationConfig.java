package iq.earthlink.social.postservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LocalizationConfig {

    @Bean
    public AcceptHeaderResolver localeResolver(AcceptHeaderResolver acceptHeaderResolver) {
        return acceptHeaderResolver;
    }
}
