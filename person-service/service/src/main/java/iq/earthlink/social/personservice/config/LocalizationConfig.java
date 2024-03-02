package iq.earthlink.social.personservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LocalizationConfig {

    @Bean
    public AcceptHeaderResolver localeResolver(AcceptHeaderResolver acceptHeaderResolver){
        return acceptHeaderResolver;
    }
}
