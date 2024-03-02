package iq.earthlink.social.personservice.service;

import iq.earthlink.social.classes.data.dto.JsonStoryConfiguration;
import iq.earthlink.social.exception.BadRequestException;
import iq.earthlink.social.personservice.person.impl.repository.FollowRepository;
import iq.earthlink.social.personservice.person.impl.repository.PersonConfigurationRepository;
import iq.earthlink.social.personservice.person.model.Following;
import iq.earthlink.social.personservice.person.model.PersonConfiguration;
import iq.earthlink.social.personservice.person.rest.JsonPersonConfiguration;
import iq.earthlink.social.postservice.story.rest.StoryRestService;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static iq.earthlink.social.classes.enumeration.StoryAccessType.SELECTED_FOLLOWERS;

@Service
public class PersonConfigurationService {

  private final PersonConfigurationRepository personConfigurationRepo;
  private final StoryRestService storyRestService;
  private final FollowRepository followRepository;
  public PersonConfigurationService(
      PersonConfigurationRepository personConfigurationRepo,
      StoryRestService storyRestService,
      FollowRepository followRepository) {
    this.personConfigurationRepo = personConfigurationRepo;
    this.storyRestService = storyRestService;
    this.followRepository = followRepository;
  }

  @Cacheable(value="spring.cache.person.config", key = "#personId")
  public JsonPersonConfiguration getByPersonId(String authorizationHeader, Long personId) {
    PersonConfiguration personConfiguration = personConfigurationRepo.findByPersonId(personId)
        .orElseGet(() -> personConfigurationRepo
            .save(PersonConfiguration.builder().personId(personId).build()));
    JsonStoryConfiguration storyConfiguration = storyRestService.getStoryConfiguration(authorizationHeader);

    return JsonPersonConfiguration
            .builder()
            .notificationMute(personConfiguration.isNotificationMute())
            .localization(personConfiguration.getLocalization())
            .theme(personConfiguration.getTheme())
            .personId(personId)
            .story(storyConfiguration)
            .build();
  }

  @CacheEvict(value="spring.cache.person.config", key = "#personId")
  public void updatePersonConfig(String authorizationHeader, Long personId,
      JsonPersonConfiguration jsonPersonConfiguration) {
      PersonConfiguration personConfiguration = personConfigurationRepo.findByPersonId(personId)
              .orElse(PersonConfiguration.builder().personId(personId).build());

      personConfiguration.setNotificationMute(jsonPersonConfiguration.isNotificationMute());

      if (Objects.nonNull(jsonPersonConfiguration.getLocalization())) {
          personConfiguration.setLocalization(jsonPersonConfiguration.getLocalization());
      }

      if (Objects.nonNull(jsonPersonConfiguration.getTheme())) {
          personConfiguration.setTheme(jsonPersonConfiguration.getTheme());
      }

      if(Objects.nonNull(jsonPersonConfiguration.getStory())) {
          JsonStoryConfiguration storyConfiguration = jsonPersonConfiguration.getStory();

          if(SELECTED_FOLLOWERS.equals(storyConfiguration.getAccessType())) {
              Set<Long> actualFollowerIds = getActualFollowersIds(personId, new ArrayList<>(storyConfiguration.getAllowedFollowersIds()));
              if(actualFollowerIds.isEmpty()) {
                  throw new BadRequestException("error.empty.or.wrong.followers.list");
              }
              storyConfiguration.setAllowedFollowersIds(actualFollowerIds);
          }

          storyRestService.setStoryConfiguration(authorizationHeader, jsonPersonConfiguration.getStory());
      }

      personConfigurationRepo.save(personConfiguration);
  }

  private Set<Long> getActualFollowersIds(Long personId, List<Long> potentialSubscribersIds) {
      Page<Following> followingList = followRepository.findFollowingBySubscribedToIdAndSubscriberIdIn(personId, potentialSubscribersIds, Pageable.unpaged());
      return followingList.get().map(following -> following.getSubscriber().getId()).collect(Collectors.toSet());
  }
}
