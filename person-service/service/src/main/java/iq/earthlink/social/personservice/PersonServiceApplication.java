package iq.earthlink.social.personservice;

import iq.earthlink.social.groupservice.group.rest.MembersRestService;
import iq.earthlink.social.groupservice.group.rest.UserGroupPermissionRestService;
import iq.earthlink.social.postservice.rest.PostRestService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestTemplate;

/**
 * Person Service for Social Network. This service is responsible for CRUD operations for Users and
 * Person Profiles entities. See README.md in the root of the project for more information.
 */
@SpringBootApplication
@EnableJpaRepositories
@EnableScheduling
@EnableCaching
@EnableAsync
@EnableFeignClients(clients = {UserGroupPermissionRestService.class, PostRestService.class, MembersRestService.class})
public class PersonServiceApplication {

  public static void main(String[] args) {
    SpringApplication.run(PersonServiceApplication.class, args);
  }

  @Bean
  public RestTemplate socialRestTemplate() {
    return new RestTemplate();
  }
}
