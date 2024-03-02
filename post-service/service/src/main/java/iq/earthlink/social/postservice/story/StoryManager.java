package iq.earthlink.social.postservice.story;

import iq.earthlink.social.classes.data.dto.JsonStoryConfiguration;
import iq.earthlink.social.postservice.person.dto.PersonDTO;
import iq.earthlink.social.postservice.story.model.StoryConfiguration;
import iq.earthlink.social.postservice.story.rest.JsonStory;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public interface StoryManager {

  @Nonnull
  JsonStory createStory(String authorizationHeader,
                        @Nonnull PersonDTO person,
                        @Nonnull MultipartFile file,
                        @Nullable List<String> references);

  @Nonnull
  JsonStory getStory(Long id);

  @Nonnull
  Page<JsonStory> findStories(String authorizationHeader, Long personId, List<Long> ownerIds, boolean unseenOnly, Pageable page);

  void removeStory(Long personId, Long id);

  @Nonnull
  ResponseEntity<Resource> downloadMedia(String rangeHeader, Long storyId);

  void setStoryConfiguration(Long personId, JsonStoryConfiguration configuration);

  @Nonnull
  StoryConfiguration getStoryConfiguration(Long personId);

  @Nonnull
  JsonStory enrichStoryWithMedia(JsonStory story);
}
