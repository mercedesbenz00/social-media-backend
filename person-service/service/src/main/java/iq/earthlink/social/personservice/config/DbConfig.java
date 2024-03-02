package iq.earthlink.social.personservice.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import java.util.Optional;

@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
@EnableJpaRepositories(basePackages = {
        "iq.earthlink.social.personservice.person",
        "iq.earthlink.social.personservice.security.repository",
        "iq.earthlink.social.personservice.authentication",
        "iq.earthlink.social.common.file",
})
@EntityScan(basePackages = {
        "iq.earthlink.social.personservice",
        "iq.earthlink.social.personservice.security.model",
        "iq.earthlink.social.common.file"
})
@EnableTransactionManagement
public class DbConfig {

  @Bean
  public AuditorAware<Long> auditorProvider() {
    return Optional::empty;
  }

}

