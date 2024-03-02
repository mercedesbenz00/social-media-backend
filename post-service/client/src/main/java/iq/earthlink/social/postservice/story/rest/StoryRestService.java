package iq.earthlink.social.postservice.story.rest;

import iq.earthlink.social.classes.data.dto.JsonStoryConfiguration;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name="post-service", url = "${post.service.url}")
public interface StoryRestService {

    @GetMapping(value = "/api/v1/stories/configuration")
    JsonStoryConfiguration getStoryConfiguration(
            @RequestHeader(value = "Authorization") String authorizationHeader);

    @PutMapping(value = "/api/v1/stories/configuration")
    JsonStoryConfiguration setStoryConfiguration(
            @RequestHeader(value = "Authorization") String authorizationHeader,
            JsonStoryConfiguration storyConfiguration);


}
