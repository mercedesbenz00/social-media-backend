package iq.earthlink.social.commentservice;

import iq.earthlink.social.personservice.rest.PersonRestService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Comment Service for Social Network. This service is responsible for CRUD operations for comment
 * entities. See README.md in the root of the project for more information.
 */
@SpringBootApplication
@EnableScheduling
@EnableFeignClients(clients = {PersonRestService.class})
public class CommentServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CommentServiceApplication.class, args);
    }
}
