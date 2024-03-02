package iq.earthlink.social.groupservice;

import iq.earthlink.social.personservice.rest.FollowingRestService;
import iq.earthlink.social.personservice.rest.PersonRestService;
import iq.earthlink.social.postservice.rest.PostRestService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Group Service for Social Network.
 * This service is responsible for CRUD operations for group entities.
 * See README.md in the root of the project for more information.
 */
@SpringBootApplication
@EnableFeignClients(clients = {PersonRestService.class, FollowingRestService.class, PostRestService.class})
@EnableScheduling
public class GroupServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(GroupServiceApplication.class, args);
	}

}

