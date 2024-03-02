package iq.earthlink.social.shortvideoservice;

import iq.earthlink.social.commentservice.rest.CommentRestService;
import iq.earthlink.social.shortvideoregistryservice.rest.ShortVideoRegistryRestService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
@EnableFeignClients(clients = {CommentRestService.class, ShortVideoRegistryRestService.class})
public class ShortVideoServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ShortVideoServiceApplication.class, args);
    }

}
