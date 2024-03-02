package iq.earthlink.social.shortvideousagestatsservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
public class ShortVideoUsageStatsServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ShortVideoUsageStatsServiceApplication.class, args);
    }

}
