package iq.earthlink.social.auditservice;

import iq.earthlink.social.personservice.rest.PersonRestService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Audit Service for Social Network. This service is responsible for CRUD operations for audit
 * entities. See README.md in the root of the project for more information.
 */
@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class })
@EnableScheduling
@EnableFeignClients(clients = {PersonRestService.class})
public class AuditServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuditServiceApplication.class, args);
    }
}
