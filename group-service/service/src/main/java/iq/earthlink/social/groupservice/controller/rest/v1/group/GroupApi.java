package iq.earthlink.social.groupservice.controller.rest.v1.group;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import iq.earthlink.social.classes.enumeration.FilterType;
import iq.earthlink.social.classes.enumeration.SortType;
import iq.earthlink.social.groupservice.group.GroupManager;
import iq.earthlink.social.groupservice.group.GroupMemberStatus;
import iq.earthlink.social.groupservice.group.GroupSearchCriteria;
import iq.earthlink.social.groupservice.group.dto.JsonGroupMember;
import iq.earthlink.social.groupservice.group.member.GroupMember;
import iq.earthlink.social.groupservice.group.permission.GroupPermission;
import iq.earthlink.social.groupservice.group.permission.GroupPermissionManager;
import iq.earthlink.social.groupservice.group.rest.*;
import iq.earthlink.social.groupservice.group.rest.enumeration.ApprovalState;
import iq.earthlink.social.groupservice.person.CurrentUser;
import iq.earthlink.social.groupservice.person.dto.PersonDTO;
import iq.earthlink.social.groupservice.util.RoleUtil;
import iq.earthlink.social.security.DefaultSecurityProvider;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections.CollectionUtils;
import org.dozer.Mapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Api(value = "GroupApi", tags = "Group Api")
@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/api/v1/groups", produces = MediaType.APPLICATION_JSON_VALUE)
public class GroupApi {

    private static final Logger LOGGER = LoggerFactory.getLogger(GroupApi.class);

    private final GroupManager groupManager;
    private final Mapper mapper;
    private final GroupPermissionManager permissionManager;
    private final DefaultSecurityProvider securityProvider;
    private final RoleUtil roleUtil;
    private final RedisTemplate<String, Long> migrationFlag;

    @ApiOperation("Returns the groups list")
    @GetMapping
    public Page<UserGroupDto> findGroups(
            @RequestHeader("Authorization") String authorizationHeader,

            @ApiParam("Filters groups if the name matched by the query")
            @RequestParam(required = false) String query,

            @ApiParam("Filters groups by the list of Category IDs")
            @RequestParam(required = false) Long[] categoryIds,

            @ApiParam("Filters groups by the list of Group IDs")
            @RequestParam(required = false) Long[] groupIds,

            @ApiParam("Filters groups by state")
            @RequestParam(required = false) List<ApprovalState> states,

            Pageable page) {
        LOGGER.debug("Requested find groups with page: {}", page);

        Long personId = securityProvider.getPersonIdFromAuthorization(authorizationHeader);
        String[] personRoles = securityProvider.getRolesFromAuthorization(authorizationHeader);

        GroupSearchCriteria criteria = GroupSearchCriteria.builder()
                .isAdmin(roleUtil.isAdmin(personRoles))
                .memberId(personId)
                .query(query)
                .categoryIds(categoryIds)
                .groupIds(groupIds)
                .states(states)
                .build();

        Page<UserGroupDto> userGroups = groupManager.findGroupsBySearchCriteria(criteria, page).map(g -> mapper.map(g, UserGroupDto.class));
        setMember(personId, userGroups);
        return userGroups;
    }

    @ApiOperation("Trigger to push all members to kafka")
    @GetMapping("/migration")
    public void triggerGroupEvents(@CurrentUser PersonDTO personDTO) {
        if(personDTO.isAdmin()) {
            LOGGER.debug("Triggered member migration by user: {}", personDTO.getDisplayName());
            migrationFlag.opsForValue().set("groupMigrationFlag", 0L);
        }
    }

    @ApiOperation("Trigger to push all members to kafka")
    @GetMapping("/migration/members")
    public void triggerMemberEvents(@CurrentUser PersonDTO personDTO) {
        if(personDTO.isAdmin()) {
            LOGGER.debug("Triggered member migration by user: {}", personDTO.getDisplayName());
            migrationFlag.opsForValue().set("groupMemberMigrationFlag", 0L);
        }
    }

    @ApiOperation("Returns my groups list")
    @GetMapping("/my-groups")
    public Page<MemberUserGroupDto> findMyGroups(
            @RequestHeader("Authorization") String authorizationHeader,
            @ApiParam("Filters groups if the name matched by the query")
            @RequestParam(required = false) String query,
            @ApiParam("Filters groups by state")
            @RequestParam(required = false) List<ApprovalState> memberStates,
            @ApiParam("Filters groups by member status")
            @RequestParam(required = false) GroupMemberStatus memberStatus,
            Pageable page) {
        LOGGER.debug("Requested find my groups with page: {}", page);

        Long personId = securityProvider.getPersonIdFromAuthorization(authorizationHeader);
        GroupSearchCriteria criteria = GroupSearchCriteria.builder()
                .query(query)
                .states(memberStates)
                .status(memberStatus)
                .build();

        return groupManager.findMyGroups(authorizationHeader, personId, criteria, page);
    }

    @ApiOperation("Returns groups based on user groups tags or categories, excluding user groups")
    @GetMapping("/similar")
    public Page<UserGroupDto> findSimilarGroups(
            @RequestHeader("Authorization") String authorizationHeader,
            Pageable page) {
        LOGGER.debug("Requested find similar groups with page: {}", page);

        Long personId = securityProvider.getPersonIdFromAuthorization(authorizationHeader);
        Page<UserGroupDto> userGroups = groupManager.findGroupsByFilterType(personId, GroupSearchCriteria.builder().build(),
                        FilterType.SIMILAR, page)
                .map(g -> mapper.map(g, UserGroupDto.class));
        userGroups.forEach(g -> g.setMemberState(ApprovalState.NOT_MEMBER));
        return userGroups;
    }

    @ApiOperation("Returns groups that following users joined and current user did not")
    @GetMapping("/suggested")
    public Page<UserGroupDto> findSuggestedGroups(
            @RequestHeader("Authorization") String authorizationHeader,
            Pageable page) {
        LOGGER.debug("Requested find suggested groups with page: {}", page);

        Long personId = securityProvider.getPersonIdFromAuthorization(authorizationHeader);

        List<Long> subscribedToIds = groupManager.getSubscriptions(authorizationHeader, personId);

        if (CollectionUtils.isEmpty(subscribedToIds)) {
            // If there is no subscriptions, return empty result:
            return Page.empty(page);
        }

        GroupSearchCriteria criteria = GroupSearchCriteria.builder()
                .subscribedToIds(subscribedToIds)
                .build();
        Page<UserGroupDto> userGroups = groupManager.findGroupsByFilterType(personId, criteria, FilterType.SUGGESTED, page)
                .map(g -> mapper.map(g, UserGroupDto.class));
        userGroups.forEach(g -> g.setMemberState(ApprovalState.NOT_MEMBER));
        return userGroups;
    }

    @ApiOperation("Returns groups sorted by group score")
    @GetMapping("/top")
    public Page<UserGroupDto> findTopGroups(@RequestHeader("Authorization") String authorizationHeader, Pageable page) {
        LOGGER.debug("Requested find top groups with page: {}", page);

        Long personId = securityProvider.getPersonIdFromAuthorization(authorizationHeader);
        Page<UserGroupDto> userGroups = groupManager.findGroupsBySortType(SortType.TOP, page)
                .map(g -> mapper.map(g, UserGroupDto.class));
        setMember(personId, userGroups);
        return userGroups;
    }


    @ApiOperation("Returns groups sorted by group members count")
    @GetMapping("/popular")
    public Page<UserGroupDto> findPopularGroups(@RequestHeader("Authorization") String authorizationHeader, Pageable page) {
        LOGGER.debug("Requested find popular groups with page: {}", page);

        Long personId = securityProvider.getPersonIdFromAuthorization(authorizationHeader);
        Page<UserGroupDto> userGroups = groupManager.findGroupsBySortType(SortType.POPULAR, page)
                .map(g -> mapper.map(g, UserGroupDto.class));
        setMember(personId, userGroups);
        return userGroups;
    }


    @ApiOperation("Returns top 10 groups based on user's frequent posts in the last 30 days, sorted by post count in descending order")
    @GetMapping("/frequently-posts")
    public List<UserGroupDto> findFrequentlyPostsGroups(@RequestHeader("Authorization") String authorizationHeader, Pageable page) {
        LOGGER.debug("Requested find a frequently posts groups");

        Long personId = securityProvider.getPersonIdFromAuthorization(authorizationHeader);
        List<UserGroupDto> userGroups = groupManager.findFrequentlyPostsGroups(personId).stream()
                .map(g -> mapper.map(g, UserGroupDto.class))
                .toList();
        setMember(personId, userGroups);
        return userGroups;
    }

    @ApiOperation("Filters groups by states")
    @GetMapping("/states")
    public Page<UserGroupDto> findGroupsByStates(
            @RequestHeader("Authorization") String authorizationHeader,
            @ApiParam(value = "Filters groups by states", example = "PENDING, APPROVED, REJECTED")
            @RequestParam(required = false) List<ApprovalState> states,
            Pageable page) {
        LOGGER.debug("Requested find groups by states with page: {}", page);

        Long personId = securityProvider.getPersonIdFromAuthorization(authorizationHeader);
        String[] personRoles = securityProvider.getRolesFromAuthorization(authorizationHeader);

        Page<UserGroupDto> userGroups = groupManager.findGroupsByStates(roleUtil.isAdmin(personRoles), states, page)
                .map(g -> mapper.map(g, UserGroupDto.class));
        setMember(personId, userGroups);
        return userGroups;
    }

    @ApiOperation("Returns the group found by id")
    @GetMapping("/{groupId}")
    public UserGroupDto getGroup(@RequestHeader("Authorization") String authorizationHeader,
                                 @PathVariable Long groupId) {
        LOGGER.debug("Requested get group with id: {}", groupId);

        Long personId = securityProvider.getPersonIdFromAuthorization(authorizationHeader);
        String[] personRoles = securityProvider.getRolesFromAuthorization(authorizationHeader);

        return groupManager.getGroupDto(personId, personRoles, groupId);
    }

    @ApiOperation("Invitation to join the group")
    @GetMapping("/{groupId}/invite/{userId}")
    public void inviteUser(@PathVariable Long groupId,
                           @PathVariable Long userId,
                           @CurrentUser PersonDTO currentUser) {
        LOGGER.debug("Requested to invite user {} to the group : {}", userId, groupId);
        groupManager.inviteUser(currentUser, groupId, userId);
    }

    @ApiOperation("Creates new group")
    @PostMapping
    public UserGroupDto createGroup(
            @RequestHeader("Authorization") String authorizationHeader,
            @RequestBody @Valid JsonGroupData request
    ) {
        Long personId = securityProvider.getPersonIdFromAuthorization(authorizationHeader);
        LOGGER.debug("Requested create group with data: {} by {}", request, personId);
        return groupManager.createGroup(authorizationHeader, request);
    }

    @ApiOperation("Updates existing group")
    @PutMapping("/{groupId}")
    public UserGroupDto updateGroup(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable Long groupId,
            @RequestBody JsonGroupData data
    ) {
        Long personId = securityProvider.getPersonIdFromAuthorization(authorizationHeader);
        String[] personRoles = securityProvider.getRolesFromAuthorization(authorizationHeader);
        boolean isAdmin = roleUtil.isAdmin(personRoles);
        LOGGER.debug("Requested updated group: {} with data: {} by {}",
                groupId, data, personId);
        UserGroupDto group = mapper.map(groupManager.updateGroup(personId, isAdmin, data, groupId), UserGroupDto.class);
        setMember(personId, new PageImpl<>(Collections.singletonList(group), Pageable.unpaged(), 1));
        List<GroupPermission> groupPermissions = permissionManager.findPermissionsInternal(personId, group.getId());
        group.setPermissions(groupPermissions.stream().map(GroupPermission::getPermission).collect(Collectors.toSet()));

        return group;
    }

    @ApiOperation("Removes the group")
    @DeleteMapping("/{groupId}")
    public void deleteGroup(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable Long groupId
    ) {
        Long personId = securityProvider.getPersonIdFromAuthorization(authorizationHeader);
        String[] personRoles = securityProvider.getRolesFromAuthorization(authorizationHeader);
        boolean isAdmin = roleUtil.isAdmin(personRoles);
        LOGGER.debug("Requested delete group: {} by {}", groupId, personId);
        groupManager.deleteGroup(personId, isAdmin, groupId);
    }

    @ApiOperation("Adds new member to the group")
    @PostMapping("/{groupId}/members")
    public void joinGroup(
            @PathVariable Long groupId,
            @RequestParam(required = false) boolean onboarding,
            @CurrentUser PersonDTO person) {
        LOGGER.debug("Requested join group: {} by {}", groupId, person.getPersonId());
        if (onboarding) {
            groupManager.initJoin(person, groupId);
        } else {
            groupManager.join(person, groupId);
        }
    }

    @ApiOperation("Returns the group membership info for the person")
    @GetMapping("/{groupId}/members/{memberId}")
    public JsonMemberPermission getMemberInfo(
            @PathVariable Long groupId,
            @PathVariable Long memberId
    ) {
        GroupMember member = groupManager.getMember(groupId, memberId);
        JsonMemberPermission groupMember = mapper.map(member, JsonMemberPermission.class);
        groupMember.setGroupId(member.getGroup().getId());
        enrichMemberStatuses(groupMember);
        return groupMember;
    }


    @ApiOperation("Updates the state of the group member")
    @PutMapping("/{groupId}/members/{memberId}")
    public void updateGroupMemberState(
            @PathVariable Long groupId,
            @PathVariable Long memberId,
            @RequestParam ApprovalState state,
            @CurrentUser PersonDTO person
    ) {
        groupManager.updateGroupMemberState(person, groupId, memberId, state);
    }

    @ApiOperation("Removes the membership from the group of the current user")
    @DeleteMapping("/{groupId}/members")
    public void leaveGroup(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable Long groupId) {
        Long personId = securityProvider.getPersonIdFromAuthorization(authorizationHeader);
        LOGGER.debug("Requested leave group: {} by {}", groupId, personId);

        groupManager.leave(personId, groupId);
    }

    @ApiOperation("Returns the members list for the group")
    @GetMapping("/{groupId}/members")
    public Page<JsonGroupMember> findMembers(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable Long groupId,
            @ApiParam("Filters group members by display names matching query pattern")
            @RequestParam(required = false) String query,
            @RequestParam(required = false) List<Long> memberIds,
            @RequestParam(required = false) List<ApprovalState> states,
            Pageable page
    ) {
        LOGGER.debug("Requested get members for group: {}", groupId);

        Long personId = securityProvider.getPersonIdFromAuthorization(authorizationHeader);
        String[] personRoles = securityProvider.getRolesFromAuthorization(authorizationHeader);
        boolean isAdmin = roleUtil.isAdmin(personRoles);

        return groupManager.findMembers(authorizationHeader, personId, memberIds, isAdmin, groupId, states, query, page);
    }

    @ApiOperation("Returns the members list for the group with notification settings")
    @GetMapping("/{groupId}/members-with-settings")
    public List<JsonGroupMemberWithNotificationSettings> findMembersWithNotificationSettings(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable Long groupId
    ) {
        LOGGER.debug("Requested get members for group: {}", groupId);
        Long personId = securityProvider.getPersonIdFromAuthorization(authorizationHeader);
        return groupManager.findMembersWithNotificationSettings(personId, groupId, Pageable.unpaged()).getContent();
    }

    @ApiOperation("Returns the list of user mutual friends in the group")
    @GetMapping("/{groupId}/mutual-friends")
    public Page<JsonGroupMember> findMutualFriends(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable Long groupId,
            @ApiParam("Filters group members by display names matching query pattern")
            @RequestParam(required = false) String query,
            Pageable page
    ) {
        LOGGER.debug("Requested get mutual friends in the group: {}", groupId);

        Long personId = securityProvider.getPersonIdFromAuthorization(authorizationHeader);
        String[] personRoles = securityProvider.getRolesFromAuthorization(authorizationHeader);
        boolean isAdmin = roleUtil.isAdmin(personRoles);

        return groupManager.findMutualFriends(authorizationHeader, personId, isAdmin, groupId, query, page);
    }

    @ApiOperation("Finds persons to tag in the post comment, it can be the group member, or any user if queried user name not found in the group members list")
    @GetMapping("/{groupId}/persons-to-tag")
    public List<PersonDTO> findPersonsToTag(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable Long groupId,
            @ApiParam("Filters group members by display names matching query pattern")
            @RequestParam(required = false) String query
    ) {
        LOGGER.debug("Requested get persons related to the post for the group: {}", groupId);
        Long currentPersonId = securityProvider.getPersonIdFromAuthorization(authorizationHeader);
        return groupManager.findPersonsToTag(currentPersonId, groupId, query);

    }

    @ApiOperation("Returns the group member IDs in groups provided")
    @GetMapping("/members")
    public Set<Long> getGroupMemberIds(@RequestHeader("Authorization") String authorizationHeader,
                                       @RequestParam(value = "groupIds") List<Long> groupIds) {
        Long personId = securityProvider.getPersonIdFromAuthorization(authorizationHeader);
        String[] personRoles = securityProvider.getRolesFromAuthorization(authorizationHeader);
        boolean isAdmin = roleUtil.isAdmin(personRoles);
        return groupManager.getGroupMemberIds(personId, isAdmin, groupIds);
    }

    @ApiOperation("Returns groups current user subscribed to")
    @GetMapping("/subscribed")
    public Set<Long> getMyGroups(@RequestHeader("Authorization") String authorizationHeader,
                                 @RequestParam(value = "groupIds", required = false) List<Long> groupIds) {
        GroupSearchCriteria criteria = GroupSearchCriteria.builder().build();
        Long personId = securityProvider.getPersonIdFromAuthorization(authorizationHeader);
        Page<MemberUserGroupDto> myGroups = groupManager.findMyGroups(authorizationHeader, personId, criteria, Pageable.unpaged());
        Set<Long> myGroupIds = myGroups.stream().map(g -> g.getGroup().getId()).collect(Collectors.toSet());
        if (CollectionUtils.isNotEmpty(groupIds)) {
            myGroupIds.retainAll(groupIds);
        }
        return myGroupIds;
    }

    private void enrichMemberStatuses(JsonMemberPermission groupMember) {
        List<GroupPermission> groupPermissions = permissionManager.findPermissionsInternal(groupMember.getPersonId(), groupMember.getGroupId());
        if (!groupPermissions.isEmpty()) {
            groupMember.setStatuses(groupPermissions.stream().map(GroupPermission::getPermission).toList());
        } else {
            groupMember.setStatuses(Collections.singletonList(GroupMemberStatus.USER));
        }
    }

    private void setMember(Long personId, Iterable<UserGroupDto> userGroups) {
        List<GroupMember> memberGroups = groupManager.findPersonGroupMemberships(personId);
        Map<Long, ApprovalState> memberGroupState = memberGroups.stream()
                .collect(Collectors.toMap(gm -> gm.getGroup().getId(), GroupMember::getState));

        // Set indicator if the current user is a member of the group:
        userGroups.forEach(g ->
                g.setMemberState(memberGroupState.getOrDefault(g.getId(), ApprovalState.NOT_MEMBER)));
    }
}
