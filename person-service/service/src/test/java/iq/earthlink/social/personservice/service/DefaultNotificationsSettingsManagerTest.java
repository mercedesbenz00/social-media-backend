package iq.earthlink.social.personservice.service;

import iq.earthlink.social.exception.NotFoundException;
import iq.earthlink.social.personservice.person.FollowManager;
import iq.earthlink.social.personservice.person.FollowSearchCriteria;
import iq.earthlink.social.personservice.person.PersonManager;
import iq.earthlink.social.personservice.person.impl.DefaultNotificationsSettingsManager;
import iq.earthlink.social.personservice.person.impl.repository.NotificationsSettingsRepository;
import iq.earthlink.social.personservice.person.model.FollowerNotificationSettings;
import iq.earthlink.social.personservice.person.model.Person;
import iq.earthlink.social.personservice.person.rest.JsonFollowerNotificationSettings;
import iq.earthlink.social.personservice.person.rest.JsonPerson;
import org.dozer.Mapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

class DefaultNotificationsSettingsManagerTest {

  @Mock
  NotificationsSettingsRepository repository;

  @Mock
  PersonManager personManager;

  @Mock
  FollowManager followManager;

  @Mock
  Mapper mapper;

  @InjectMocks
  DefaultNotificationsSettingsManager notificationsSettingsManager;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  void testGetNotificationsSettings_ReturnsNotificationsSettingsPage() {
    Long personId = 1L;
    Pageable page = PageRequest.of(0, 10);
    when(repository.findAllByPersonId(anyLong(), any(Pageable.class))).thenReturn(Page.empty());
    when(personManager.getPersonByIdInternal(personId)).thenReturn(new Person());
    when(mapper.map(any(FollowerNotificationSettings.class), eq(JsonFollowerNotificationSettings.class))).thenReturn(new JsonFollowerNotificationSettings());

    assertEquals(Page.empty(), notificationsSettingsManager.getNotificationsSettings(personId, List.of(), page));
  }

  @Test
  void testGetNotificationSettingsByFollowerId_ReturnsNotificationSettings() {
    Long personId = 1L;
    Long followerId = 2L;
    FollowerNotificationSettings followerNotificationSettings = new FollowerNotificationSettings();
    when(repository.findByPersonIdAndFollowingId(anyLong(), anyLong())).thenReturn(followerNotificationSettings);
    when(personManager.getPersonByIdInternal(anyLong())).thenReturn(new Person());
    JsonFollowerNotificationSettings jsonFollowerNotificationSettings = new JsonFollowerNotificationSettings();
    when(mapper.map(any(FollowerNotificationSettings.class), eq(JsonFollowerNotificationSettings.class))).thenReturn(jsonFollowerNotificationSettings);

    assertEquals(jsonFollowerNotificationSettings, notificationsSettingsManager.getNotificationSettingsByFollowerId(personId, followerId));

  }

  @Test
  void testSetNotificationsSettings() {
    Long personId = 1L;
    Long followerId = 2L;
    JsonFollowerNotificationSettings jsonFollowerNotificationSettings = new JsonFollowerNotificationSettings();
    FollowerNotificationSettings followerNotificationSettings = new FollowerNotificationSettings();
    FollowSearchCriteria criteria = FollowSearchCriteria.builder()
            .personId(personId)
            .followingIds(List.of(followerId).toArray(Long[]::new))
            .build();

    when(personManager.getPersonByIdInternal(personId)).thenReturn(new Person());
    when(personManager.getPersonByIdInternal(followerId)).thenReturn(new Person());
    when(followManager.findFollowedPersons(criteria, Pageable.unpaged())).thenReturn(new PageImpl<>(List.of(new JsonPerson())));
    when(mapper.map(any(JsonFollowerNotificationSettings.class), eq(FollowerNotificationSettings.class))).thenReturn(followerNotificationSettings);
    when(repository.save(any(FollowerNotificationSettings.class))).thenReturn(followerNotificationSettings);
    JsonFollowerNotificationSettings jsonFollowerNotificationSettings1 = new JsonFollowerNotificationSettings();
    when(mapper.map(any(FollowerNotificationSettings.class), eq(JsonFollowerNotificationSettings.class))).thenReturn(jsonFollowerNotificationSettings1);

    assertEquals(jsonFollowerNotificationSettings1, notificationsSettingsManager.setNotificationsSettings(personId, followerId, jsonFollowerNotificationSettings));
  }

  @Test
  void testSetNotificationsSettings_ThrowsExceptionWhenFollowerNotFollowingPerson() {
    Long personId = 1L;
    Long followerId = 2L;
    JsonFollowerNotificationSettings jsonFollowerNotificationSettings = new JsonFollowerNotificationSettings();
    FollowerNotificationSettings followerNotificationSettings = new FollowerNotificationSettings();
    FollowSearchCriteria criteria = FollowSearchCriteria.builder()
            .personId(personId)
            .followingIds(List.of(followerId).toArray(Long[]::new))
            .build();

    when(personManager.getPersonByIdInternal(personId)).thenReturn(new Person());
    when(personManager.getPersonByIdInternal(followerId)).thenReturn(new Person());
    when(followManager.findFollowedPersons(criteria, Pageable.unpaged())).thenReturn(Page.empty());
    when(mapper.map(any(JsonFollowerNotificationSettings.class), eq(FollowerNotificationSettings.class))).thenReturn(followerNotificationSettings);
    when(repository.save(any(FollowerNotificationSettings.class))).thenReturn(followerNotificationSettings);
    JsonFollowerNotificationSettings jsonFollowerNotificationSettings1 = new JsonFollowerNotificationSettings();
    when(mapper.map(any(FollowerNotificationSettings.class), eq(JsonFollowerNotificationSettings.class))).thenReturn(jsonFollowerNotificationSettings1);

    assertThatThrownBy(() -> notificationsSettingsManager.setNotificationsSettings(personId, followerId, jsonFollowerNotificationSettings))
            .isInstanceOf(NotFoundException.class)
            .hasMessageContaining("error.follower.not.following.person");
  }

}
