package iq.earthlink.social.shortvideoregistryservice;

import iq.earthlink.social.personservice.rest.PersonRestService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients(clients = {PersonRestService.class})
public class ShortVideoRegistryServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ShortVideoRegistryServiceApplication.class, args);
    }
}
