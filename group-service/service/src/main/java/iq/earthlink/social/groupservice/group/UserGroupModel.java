package iq.earthlink.social.groupservice.group;

import org.springframework.data.domain.Pageable;

/**
 * The projection interface for the
 * {@link GroupRepository#findGroups(GroupSearchCriteria, Pageable)} method.
 */
public interface UserGroupModel {
  UserGroup getGroup();

  Long getMembersCount();

  Long getPublishedPostsCount();

  Long getScore();
}
