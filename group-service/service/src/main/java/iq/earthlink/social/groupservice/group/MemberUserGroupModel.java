package iq.earthlink.social.groupservice.group;

import iq.earthlink.social.groupservice.group.rest.enumeration.ApprovalState;
import org.springframework.data.domain.Pageable;

import java.util.Date;

/**
 * The projection interface for the
 * {@link GroupRepository#findMyGroups(Long, GroupSearchCriteria, Pageable)} method.
 */
public interface MemberUserGroupModel {
  UserGroup getGroup();

  Date getMemberSince();

  Long getPublishedPostsCount();

  Date getVisitedAt();

  ApprovalState getState();
}
