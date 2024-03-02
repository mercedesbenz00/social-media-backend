package iq.earthlink.social.groupservice.group;

import iq.earthlink.social.classes.enumeration.FilterType;
import iq.earthlink.social.classes.enumeration.SortType;
import iq.earthlink.social.classes.enumeration.TimeInterval;
import iq.earthlink.social.groupservice.group.dto.GroupStats;
import iq.earthlink.social.groupservice.group.dto.JsonGroupMember;
import iq.earthlink.social.groupservice.group.member.GroupMember;
import iq.earthlink.social.groupservice.group.rest.JsonGroupMemberWithNotificationSettings;
import iq.earthlink.social.groupservice.group.rest.MemberUserGroupDto;
import iq.earthlink.social.groupservice.group.rest.UserGroupDto;
import iq.earthlink.social.groupservice.group.rest.enumeration.ApprovalState;
import iq.earthlink.social.groupservice.person.dto.PersonDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Set;

/**
 * The group manager is responsible for managing user groups.
 */
public interface GroupManager {

    @Nonnull
    Page<UserGroup> findGroupsBySearchCriteria(GroupSearchCriteria criteria, @Nonnull Pageable page);

    @Nonnull
    Page<UserGroup> findGroupsByFilterType(@Nonnull Long personId, GroupSearchCriteria criteria,
                                           @Nonnull FilterType filterType, @Nonnull Pageable page);

    List<Long> getSubscriptions(String authorizationHeader, Long personId);
    List<Long> getSubscribers(String authorizationHeader, Long personId);

    @Nonnull
    Page<UserGroup> findGroupsBySortType(@Nonnull SortType sortType, @Nonnull Pageable page);

    @Nonnull
    UserGroupDto getGroupDto(@Nonnull Long personId, @Nonnull String[] personRoles, @Nonnull Long groupId);

    @Nonnull
    UserGroupDto createGroup(@Nonnull String authorizationHeader, @Nonnull GroupData data);

    @Nonnull
    UserGroup updateGroup(@Nonnull Long personId, @Nonnull Boolean isAdmin, @Nonnull GroupData data, @Nonnull Long groupId);

    void deleteGroup(@Nonnull Long personId, boolean isAdmin, @Nonnull Long groupId);

    GroupMember join(@Nonnull PersonDTO person, @Nonnull Long groupId);

    GroupMember initJoin(@Nonnull PersonDTO personDTO, @Nonnull Long groupId);

    void updateGroupMemberState(@Nonnull PersonDTO person, Long groupId,
                                Long memberId, ApprovalState state);

    void leave(@Nonnull Long personId, @Nonnull Long groupId);

    Page<JsonGroupMember> findMembers(@Nonnull String authorizationHeader, @Nonnull Long personId, List<Long> memberIds,
                                      @Nonnull Boolean isAdmin, Long groupId, List<ApprovalState> states,
                                      String query, Pageable page);

    Page<JsonGroupMemberWithNotificationSettings> findMembersWithNotificationSettings(@Nonnull Long personId, @Nonnull Long groupId, Pageable page);

    List<PersonDTO> findPersonsToTag(Long currentPersonId, Long groupId, String query);

    List<GroupMember> findPersonGroupMemberships(Long personId);

    @Nonnull
    GroupMember getMember(Long groupId, Long memberId);

    void inviteUser(@Nonnull PersonDTO currentUser, Long groupId, Long memberId);

    void removeMemberFromGroups(Long personId);

    Page<UserGroup> findGroupsByStates(Boolean isAdmin, List<ApprovalState> states, Pageable page);

    Page<MemberUserGroupDto> findMyGroups(@Nonnull String authorizationHeader, @Nonnull Long personId, @Nonnull GroupSearchCriteria criteria, Pageable page);

    /**
     * Returns group statistics.
     * @param fromDate, String
     * @param timeInterval, enum - Time interval for which statistical results are calculated: DAY, MONTH, or YEAR. Default value - MONTH.
     */
    GroupStats getGroupStats(String fromDate, TimeInterval timeInterval);

    Set<Long> getGroupMemberIds(@Nonnull Long personId, @Nonnull Boolean isAdmin, List<Long> groupIds);

    Page<JsonGroupMember> findMutualFriends(@Nonnull String authorizationHeader, @Nonnull Long personId,
                                            @Nonnull Boolean isAdmin, Long groupId, String query, Pageable page);

    List<UserGroup> findFrequentlyPostsGroups(Long personId);
}
