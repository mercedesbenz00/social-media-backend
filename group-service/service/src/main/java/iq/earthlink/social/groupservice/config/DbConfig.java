package iq.earthlink.social.groupservice.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import java.util.Optional;

@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
@EnableJpaRepositories(basePackages = {
        "iq.earthlink.social.groupservice",
        "iq.earthlink.social.common.file"
})
@EntityScan({
    "iq.earthlink.social.groupservice",
    "iq.earthlink.social.common.file"
})
@EnableTransactionManagement
public class DbConfig {

  @Bean
  public AuditorAware<Long> auditorAware() {
    return Optional::empty;
  }

}
