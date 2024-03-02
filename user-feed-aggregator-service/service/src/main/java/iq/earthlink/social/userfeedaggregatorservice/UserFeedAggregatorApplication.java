package iq.earthlink.social.userfeedaggregatorservice;

import iq.earthlink.social.groupservice.group.rest.MembersRestService;
import iq.earthlink.social.postservice.rest.PostRestService;
import iq.earthlink.social.userfeedaggregatorservice.feign.FeedAggregatorRestService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableFeignClients(clients = {MembersRestService.class, FeedAggregatorRestService.class, PostRestService.class})
@EnableAsync
public class UserFeedAggregatorApplication {
    public static void main(String[] args) {
        SpringApplication.run(UserFeedAggregatorApplication.class, args);
    }
}
