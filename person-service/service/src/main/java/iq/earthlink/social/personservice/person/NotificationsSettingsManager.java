package iq.earthlink.social.personservice.person;

import iq.earthlink.social.personservice.person.rest.JsonFollowerNotificationSettings;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface NotificationsSettingsManager {


  /**
   * @param personId the person id
   * @param page the pagination params
   * @return the list of followers notification settings
   */
  Page<JsonFollowerNotificationSettings> getNotificationsSettings(Long personId, List<Long> followerIds, Pageable page);

  List<Long> getPersonIdsWhoMutedFollowingId(Long followingId);

  /**
   * @param personId the person id
   * @param followerId the follower id
   * @return the follower notification settings
   */
  JsonFollowerNotificationSettings getNotificationSettingsByFollowerId(Long personId, Long followerId);

  /**
   * @param personId the person id
   * @param followerId the follower id
   * @param jsonFollowerNotificationSettings the follower notification settings
   * @return the updated follower notification settings
   */
  JsonFollowerNotificationSettings setNotificationsSettings(Long personId, Long followerId, JsonFollowerNotificationSettings jsonFollowerNotificationSettings);
}