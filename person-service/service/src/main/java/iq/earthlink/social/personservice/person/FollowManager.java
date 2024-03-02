package iq.earthlink.social.personservice.person;

import iq.earthlink.social.personservice.person.model.Following;
import iq.earthlink.social.personservice.person.model.Person;
import iq.earthlink.social.personservice.person.rest.JsonPerson;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import javax.annotation.Nonnull;

public interface FollowManager {

  /**
   * Sets up following relation between persons.
   *
   * @param follower the follower person
   * @param followed the followed person
   */
  Following follow(@Nonnull Person follower, @Nonnull Person followed);

  /**
   * Removes the following relation between persons.
   *
   * @param followerId the follower person id
   * @param followed the followed person
   */
  void unfollow(@Nonnull Long followerId, @Nonnull Person followed);


  @Nonnull
  Page<Following> findFollowersOld(@Nonnull FollowSearchCriteria criteria, @Nonnull Pageable page);

  /**
   * Returns the followers for the person.
   *
   * @param criteria the search criteria
   * @param page the pagination params
   */
  @Nonnull
  Page<JsonPerson> findFollowers(@Nonnull FollowSearchCriteria criteria, @Nonnull Pageable page);

  /**
   * Returns the page with persons which provided person follows.
   *
   * @param person the search criteria
   * @param page the pagination params
   */
  @Nonnull
  Page<Following> findFollowedPersonsOld(@Nonnull FollowSearchCriteria person, @Nonnull Pageable page);

  /**
   * Returns the page with persons which provided person follows.
   *
   * @param person the search criteria
   * @param page the pagination params
   */
  @Nonnull
  Page<JsonPerson> findFollowedPersons(@Nonnull FollowSearchCriteria person, @Nonnull Pageable page);
}
