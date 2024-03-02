package iq.earthlink.social.postservice;

import iq.earthlink.social.groupservice.group.rest.MembersRestService;
import iq.earthlink.social.groupservice.group.rest.UserGroupPermissionRestService;
import iq.earthlink.social.personservice.rest.FollowingRestService;
import iq.earthlink.social.personservice.rest.PersonBanRestService;
import iq.earthlink.social.personservice.rest.PersonRestService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Post Service for Social Network. This service is responsible for CRUD operations for post
 * entities. See README.md in the root of the project for more information.
 */
@SpringBootApplication
@EnableScheduling
@EnableFeignClients(clients = {PersonBanRestService.class, FollowingRestService.class, PersonRestService.class,
        UserGroupPermissionRestService.class, MembersRestService.class})
public class PostServiceApplication {

  public static void main(String[] args) {
    SpringApplication.run(PostServiceApplication.class, args);
  }
}

