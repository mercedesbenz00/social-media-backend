package iq.earthlink.social.notificationservice;

import iq.earthlink.social.groupservice.group.rest.UserGroupPermissionRestService;
import iq.earthlink.social.personservice.rest.PersonBanRestService;
import iq.earthlink.social.personservice.rest.PersonRestService;
import iq.earthlink.social.postservice.rest.PostRestService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Notification Service for Social Network.
 * This service is responsible for delivering notification events through STOMP protocol.
 * See README.md in the root of the project for more information.
 */
@SpringBootApplication
@EnableScheduling
@EnableAsync
@EnableFeignClients(clients = {PersonBanRestService.class, UserGroupPermissionRestService.class,
		PostRestService.class, PersonRestService.class})
public class NotificationServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(NotificationServiceApplication.class, args);
	}

}