package iq.earthlink.social.groupservice.group;

import feign.FeignException;
import iq.earthlink.social.classes.data.dto.JsonMediaFile;
import iq.earthlink.social.classes.data.dto.JsonSizedImage;
import iq.earthlink.social.classes.data.dto.PersonData;
import iq.earthlink.social.classes.data.event.NotificationEvent;
import iq.earthlink.social.classes.enumeration.*;
import iq.earthlink.social.common.audit.AuditMarker;
import iq.earthlink.social.common.audit.AuditMessage;
import iq.earthlink.social.common.audit.EventAction;
import iq.earthlink.social.common.data.event.GroupActivityEvent;
import iq.earthlink.social.common.file.SizedImage;
import iq.earthlink.social.common.rest.RestPageImpl;
import iq.earthlink.social.common.util.CommonConstants;
import iq.earthlink.social.common.util.DateUtil;
import iq.earthlink.social.exception.*;
import iq.earthlink.social.groupservice.category.Category;
import iq.earthlink.social.groupservice.category.CategoryModel;
import iq.earthlink.social.groupservice.category.DefaultCategoryManager;
import iq.earthlink.social.groupservice.category.repository.CategoryRepository;
import iq.earthlink.social.groupservice.event.KafkaProducerService;
import iq.earthlink.social.groupservice.group.dto.*;
import iq.earthlink.social.groupservice.group.member.GroupMember;
import iq.earthlink.social.groupservice.group.member.GroupMemberRepository;
import iq.earthlink.social.groupservice.group.notificationsettings.GroupNotificationSettingsManager;
import iq.earthlink.social.groupservice.group.permission.GroupPermission;
import iq.earthlink.social.groupservice.group.permission.GroupPermissionManager;
import iq.earthlink.social.groupservice.group.permission.PermissionSearchCriteria;
import iq.earthlink.social.groupservice.group.rest.*;
import iq.earthlink.social.groupservice.group.rest.enumeration.AccessType;
import iq.earthlink.social.groupservice.group.rest.enumeration.ApprovalState;
import iq.earthlink.social.groupservice.group.rest.enumeration.GroupVisibility;
import iq.earthlink.social.groupservice.person.PersonManager;
import iq.earthlink.social.groupservice.person.dto.PersonDTO;
import iq.earthlink.social.groupservice.tag.TagRepository;
import iq.earthlink.social.groupservice.util.RoleUtil;
import iq.earthlink.social.personservice.person.rest.JsonPerson;
import iq.earthlink.social.personservice.rest.FollowingRestService;
import iq.earthlink.social.postservice.rest.PostRestService;
import iq.earthlink.social.security.SecurityProvider;
import iq.earthlink.social.util.ExceptionUtil;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dozer.Mapper;
import org.jetbrains.annotations.NotNull;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Nonnull;
import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static iq.earthlink.social.common.util.CommonConstants.ERROR_CHECK_NOT_NULL;
import static iq.earthlink.social.common.util.Preconditions.checkNotNull;
import static iq.earthlink.social.groupservice.category.DefaultCategoryManager.checkIfAssignable;
import static java.util.stream.Collectors.groupingBy;
import static org.apache.commons.lang3.ObjectUtils.firstNonNull;

@Service
@RequiredArgsConstructor
public class DefaultGroupManager implements GroupManager {

    private static final Logger LOGGER = LogManager.getLogger(DefaultGroupManager.class);

    private final GroupRepository repository;
    private final CategoryRepository categoryRepository;
    private final TagRepository tagRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final GroupMediaService mediaService;
    private final RabbitTemplate rabbitTemplate;
    private final PersonManager personManager;
    private final FollowingRestService followingRestService;
    private final GroupPermissionManager permissionManager;
    private final GroupNotificationSettingsManager groupNotificationSettingsManager;
    private final DefaultCategoryManager categoryManager;
    private final GroupManagerUtils groupManagerUtils;
    private final PostRestService postRestService;
    private final KafkaProducerService kafkaProducerService;
    private final SecurityProvider securityProvider;
    private final RoleUtil roleUtil;
    private final RedisTemplate<String, Long> migrationFlag;

    private final Mapper mapper;

    @Value("${social.groupservice.personToTag.maxReturnItems}")
    private int limitForPersonSearch;

    private static final String GROUP_ID = "groupId";
    private static final String PERSON_ID = "personId";
    private static final String ROUTE_TO = "routeTo";
    private static final String GROUP_NAME = "groupName";
    private static final String PUSH_NOTIFICATION = "PUSH_NOTIFICATION";
    private static final String ERROR_PERSON_NOT_AUTHORIZED = "error.person.not.authorized";

    @Transactional(readOnly = true)
    @Nonnull
    @Override
    public Page<UserGroup> findGroupsBySearchCriteria(@Nonnull GroupSearchCriteria criteria, @Nonnull Pageable page) {
        updateQuery(criteria);

        // Find groups based on search criteria:
        return page.getSort().isSorted() ? repository.findGroups(criteria, page).map(UserGroupModel::getGroup)
                : repository.findGroupsOrderedBySimilarity(criteria, page).map(UserGroupModel::getGroup);
    }

    @Transactional(readOnly = true)
    @Nonnull
    @Override
    public Page<MemberUserGroupDto> findMyGroups(@Nonnull String authorizationHeader, @Nonnull Long personId,
                                                 @Nonnull GroupSearchCriteria criteria, @Nonnull Pageable page) {
        updateQuery(criteria);
        Page<MemberUserGroupModel> memberGroups;
        if (page.getSort().isSorted()) {
            memberGroups = criteria.getStatus() == null ? repository.findMyGroups(personId, criteria, page)
                    : repository.findMyGroupsByPermission(personId, criteria, page);
        } else {
            memberGroups = criteria.getStatus() == null ? repository.findMyGroupsOrderedBySimilarity(personId, criteria, page)
                    : repository.findMyGroupsByPermissionOrderedBySimilarity(personId, criteria, page);
        }

        if (memberGroups.isEmpty()) {
            return Page.empty(page);
        }

        // Find all group permissions for the current user:
        Map<Long, Set<GroupMemberStatus>> permissionsByGroupId = new HashMap<>();

        List<Long> groupIds = memberGroups.map(m -> m.getGroup().getId()).stream().toList();
        if (CollectionUtils.isNotEmpty(groupIds)) {
            List<GroupPermission> permissions = permissionManager.findPermissionsInternal(PermissionSearchCriteria.
                    builder()
                    .groupIds(groupIds)
                    .personId(personId)
                    .build());

            permissionsByGroupId = permissions.stream()
                    .collect(Collectors.groupingBy(m -> m.getUserGroup().getId(), Collectors.mapping(GroupPermission::getPermission, Collectors.toSet())));
        }

        Map<Long, Set<GroupMemberStatus>> permissionsByFinalGroupId = permissionsByGroupId;
        return memberGroups.map(m -> {
            UserGroupDto group = mapper.map(m.getGroup(), UserGroupDto.class);
            group.setMemberState(m.getState());

            Set<GroupMemberStatus> groupMemberStatuses = permissionsByFinalGroupId.get(group.getId());
            groupMemberStatuses = groupMemberStatuses != null ? groupMemberStatuses : Set.of(GroupMemberStatus.NOT_MEMBER);
            group.setPermissions(groupMemberStatuses);

            return MemberUserGroupDto.builder()
                    .memberSince(m.getMemberSince())
                    .publishedPostsCount(m.getPublishedPostsCount())
                    .visitedAt(m.getVisitedAt())
                    .group(group)
                    .memberState(m.getState())
                    .build();
        });
    }

    @Nonnull
    @Override
    public Page<UserGroup> findGroupsByFilterType(@Nonnull Long personId, @Nonnull GroupSearchCriteria criteria,
                                                  @Nonnull FilterType filterType, @Nonnull Pageable page) {
        if (FilterType.SIMILAR.equals(filterType)) {
            Page<CategoryModel> userCategories = categoryManager.findCategoriesInternal(CategorySearchCriteria.builder().personId(personId).build(), Pageable.ofSize(100));
            List<Long> catIds = userCategories.isEmpty() ? null : userCategories.getContent().stream().map(CategoryModel::getId).toList();

            return repository.findSimilarGroups(personId, catIds, page);
        }

        if (FilterType.SUGGESTED.equals(filterType)) {
            return repository.findSuggestedGroups(personId, criteria.getSubscribedToIds(), page);
        }

        // Return all groups if filter type is not specified:
        return repository.findGroups(criteria, page).map(UserGroupModel::getGroup);
    }

    @Cacheable(value = "spring.cache.person.subscriptions", key = "#personId")
    public List<Long> getSubscriptions(String authorizationHeader, Long personId) {
        return followingRestService.findSubscriptions(authorizationHeader, personId, 0, Integer.MAX_VALUE).stream()
                .map(f -> f.getSubscribedTo().getId()).toList();
    }

    @Cacheable(value = "spring.cache.person.subscribers", key = "#personId")
    public List<Long> getSubscribers(String authorizationHeader, Long personId) {
        return followingRestService.findSubscribers(authorizationHeader, personId, 0, Integer.MAX_VALUE).stream()
                .map(f -> f.getSubscriber().getId()).toList();
    }

    @Nonnull
    @Override
    public Page<UserGroup> findGroupsBySortType(@Nonnull SortType sortType, @Nonnull Pageable page) {
        if (SortType.TOP.equals(sortType)) {
            page = PageRequest.of(page.getPageNumber(), page.getPageSize(), Sort.Direction.DESC, UserGroupStats.SCORE_CONST);
            return repository.findOrderedGroups(UserGroupStats.SCORE_CONST, page).map(UserGroupModel::getGroup);
        }

        if (SortType.POPULAR.equals(sortType)) {
            page = PageRequest.of(page.getPageNumber(), page.getPageSize(), Sort.Direction.DESC, UserGroupStats.MEMBERS_COUNT_CONST);
            return repository.findOrderedGroups(UserGroupStats.MEMBERS_COUNT_CONST, page).map(UserGroupModel::getGroup);
        }

        // Sort by published posts count if sort type is not specified:
        page = PageRequest.of(page.getPageNumber(), page.getPageSize(), Sort.Direction.DESC, UserGroupStats.PUBLISHED_POSTS_COUNT_CONST);
        return repository.findOrderedGroups(UserGroupStats.PUBLISHED_POSTS_COUNT_CONST, page).map(UserGroupModel::getGroup);
    }

    @Override
    public List<UserGroup> findFrequentlyPostsGroups(Long personId) {
        //todo: redesign to decouple
        List<Long> frequentlyPostsGroupIds = postRestService.getFrequentlyPostsGroups(personId);
        return repository.findByIdIn(frequentlyPostsGroupIds);
    }

    @Transactional(readOnly = true)
    @Nonnull
    @Override
    public UserGroupDto getGroupDto(@Nonnull Long personId, @Nonnull String[] personRoles, @Nonnull Long groupId) {
        GroupMember member = getGroupMember(groupId, personId);
        boolean isMember = member != null;
        ApprovalState memberState = isMember ? member.getState() : ApprovalState.NOT_MEMBER;

        UserGroupDto group = mapper.map(groupManagerUtils.getGroup(groupId), UserGroupDto.class);

        boolean isGroupVisible = roleUtil.isAdmin(personRoles)
                || AccessType.PUBLIC.equals(group.getAccessType())
                || GroupVisibility.EVERYONE.equals(group.getVisibility())
                || ApprovalState.APPROVED.equals(memberState)
                || ApprovalState.INVITED.equals(memberState);

        if (!isGroupVisible) {
            throw new ForbiddenException(ERROR_PERSON_NOT_AUTHORIZED);
        }
        group.setMemberState(memberState);

        // Set group permissions for the current user:
        List<GroupPermission> currentUserPermissions = permissionManager.findPermissionsInternal(personId, groupId);
        group.setPermissions(CollectionUtils.isNotEmpty(currentUserPermissions) ?
                currentUserPermissions.stream().map(GroupPermission::getPermission).collect(Collectors.toSet())
                : Set.of(GroupMemberStatus.NOT_MEMBER));

        if (roleUtil.isAdmin(personRoles) || roleUtil.isModerator(personRoles)){
            List<JsonGroupPermission> jsonGroupPermissions = getJsonGroupPermissions(group);
            group.setGroupModerators(jsonGroupPermissions);
        }

        return group;
    }

    @Transactional
    @Nonnull
    @Override
    public UserGroupDto createGroup(@Nonnull String authorizationHeader, @Nonnull GroupData data) {
        checkNotNull(data, ERROR_CHECK_NOT_NULL, "data");
        checkNotNull(data.getCategories(), ERROR_CHECK_NOT_NULL, "categories");

        Long personId = securityProvider.getPersonIdFromAuthorization(authorizationHeader);
        String[] personRoles = securityProvider.getRolesFromAuthorization(authorizationHeader);
        var isAdmin = roleUtil.isAdmin(personRoles);

        UserGroup group = new UserGroup();
        group.setState(isAdmin ? ApprovalState.APPROVED : ApprovalState.PENDING);
        group.setOwnerId(personId);
        group.setStats(UserGroupStats.builder().group(group).build());

        try {
            UserGroup saved = repository.save(fillData(group, data, isAdmin));
            LOGGER.info("Successfully created group: {}", saved);
            kafkaProducerService.sendMessageOnFailureThrowError(CommonConstants.GROUP_CREATED, getUserGroupDTOFromEntity(saved));

            joinGroup(saved, personId, isAdmin, GroupMemberStatus.ADMIN);
            LOGGER.info("The group: {} owner: {} joined as ADMIN", saved.getId(), personId);

            String logMessage = String.format("Successfully created group: \"%s\"", saved.getName());
            LOGGER.info(AuditMarker.getMarker(), AuditMessage.create(EventAction.CREATE_GROUP, logMessage, personId, saved.getId()));
            var groupDTO = mapper.map(saved, UserGroupDto.class);
            groupDTO.setMemberState(saved.getState());
            return groupDTO;
        } catch (DataIntegrityViolationException ex) {
            throw new NotUniqueException("error.group.already.exists", data.getName());
        }
    }

    @Transactional
    @Nonnull
    @Override
    public UserGroup updateGroup(
            @Nonnull Long personId, @Nonnull Boolean isAdmin, @Nonnull GroupData data, @Nonnull Long groupId) {
        checkNotNull(personId, ERROR_CHECK_NOT_NULL, PERSON_ID);
        checkNotNull(isAdmin, ERROR_CHECK_NOT_NULL, "isAdmin");
        checkNotNull(data, ERROR_CHECK_NOT_NULL, "data");
        checkNotNull(groupId, ERROR_CHECK_NOT_NULL, GROUP_ID);

        if (!(isAdmin || isGroupAdmin(groupId, personId)))
            throw new ForbiddenException("error.person.unauthorized.to.update.group");
        try {
            UserGroup original = groupManagerUtils.getGroup(groupId);
            UserGroup group = repository.saveAndFlush(fillData(original, data, isAdmin));

            if (StringUtils.isNotEmpty(data.getRules())) {
                String logMessage = String.format("Rules for the group \"%s\" were updated by \"%s\"", group.getName(), personId);
                LOGGER.info(AuditMarker.getMarker(), AuditMessage.create(EventAction.UPDATE_GROUP_RULES, logMessage, personId, groupId));
            }
            kafkaProducerService.sendMessageOnFailureThrowError(CommonConstants.GROUP_UPDATED, getUserGroupDTOFromEntity(group));
            return group;
        } catch (DataIntegrityViolationException ex) {
            throw new RestApiException(HttpStatus.CONFLICT, "error.group.update.with.existing.name", groupId);
        }
    }

    @Transactional
    @Override
    public void deleteGroup(@Nonnull Long personId, boolean isAdmin, @Nonnull Long groupId) {
        checkNotNull(personId, ERROR_CHECK_NOT_NULL, PERSON_ID);
        checkNotNull(isAdmin, ERROR_CHECK_NOT_NULL, "isAdmin");
        checkNotNull(groupId, ERROR_CHECK_NOT_NULL, GROUP_ID);
        if (!isAdmin && !isGroupAdmin(groupId, personId)) {
            throw new ForbiddenException("error.person.unauthorized.to.remove.group");
        }

        LOGGER.info("Person: {} removing the group: {}", personId, groupId);
        Optional<UserGroup> userGroup = repository.findById(groupId);
        if (userGroup.isPresent()) {
            GroupMemberSearchCriteria criteria = GroupMemberSearchCriteria.builder()
                    .groupId(groupId)
                    .build();
            Page<GroupMember> members = groupMemberRepository.findMembers(criteria, Pageable.unpaged());
            groupMemberRepository.deleteMembers(groupId);

            members.forEach(member ->
                    kafkaProducerService.sendMessageOnFailureThrowError(CommonConstants.GROUP_MEMBER_EVENT, GroupMemberEventDTO
                            .builder()
                            .personId(member.getPersonId())
                            .groupId(member.getGroup().getId())
                            .permissions(List.of(GroupMemberStatus.USER))
                            .eventType(GroupMemberEventType.LEFT)
                            .build()));
            // Delete user group permissions:
            permissionManager.removeAllGroupPermissionsInternal(groupId);
            removeMediaFiles(userGroup.get(), personId, isAdmin);
            try {
                repository.delete(userGroup.get());

                var userGroupDTO = UserGroupDto
                        .builder()
                        .id(groupId)
                        .name("")
                        .avatar(null)
                        .build();
                kafkaProducerService.sendMessageOnFailureThrowError(CommonConstants.GROUP_UPDATED, userGroupDTO);
            } catch (Exception ex) {
                throw new RestApiException(HttpStatus.INTERNAL_SERVER_ERROR,
                        (String.format("Error while deleting the group with Id: %s, error: %s", groupId, ex.getMessage())));
            }
        }
    }

    @Transactional
    @Override
    public GroupMember join(@Nonnull PersonDTO person, @Nonnull Long groupId) {

        UserGroup userGroup = repository.findById(groupId)
                .orElseThrow(() -> new NotFoundException("error.group.not.found", groupId));

        // Regular user cannot join group if group state is not approved:
        Boolean isAdmin = person.isAdmin() || isGroupAdmin(groupId, person.getPersonId());
        if (!isGroupOperationAllowed(Collections.singletonList(userGroup.getState()), isAdmin)) {
            throw new ForbiddenException(ERROR_PERSON_NOT_AUTHORIZED);
        }

        GroupMember groupMember = joinGroup(userGroup, person.getPersonId(), person.isAdmin(), GroupMemberStatus.USER);

        if (AccessType.PRIVATE.equals(userGroup.getAccessType())) {
            // send notification for group admins that join group requested:
            sendJoinRequestNotification(person, userGroup);
        }
        return groupMember;
    }

    @Transactional
    @Override
    public GroupMember initJoin(@Nonnull PersonDTO person, @NonNull Long groupId) {

        GroupMember groupMember = join(person, groupId);
        GroupSearchCriteria criteria = GroupSearchCriteria.builder().states(List.of(ApprovalState.PENDING, ApprovalState.INVITED, ApprovalState.APPROVED)).build();
        updateQuery(criteria);
        Page<MemberUserGroupModel> memberGroups = repository.findMyGroups(person.getPersonId(), criteria, Pageable.unpaged());
        if (CollectionUtils.isNotEmpty(memberGroups.getContent())) {
            try {
                kafkaProducerService.sendMessageOnFailureThrowError(CommonConstants.INTEREST_GROUP_ONBOARD_STATE, OnboardDTO
                        .builder()
                        .personId(person.getPersonId())
                        .state(OnboardState.GROUPS_PROVIDED)
                        .build());
            } catch (FeignException ex) {
                ExceptionUtil.processFeignException(ex);
            }
        }
        return groupMember;
    }

    @Transactional
    @Override
    public void updateGroupMemberState(@NonNull PersonDTO person, Long groupId, Long memberId, ApprovalState state) {
        if (!(person.isAdmin() || isGroupAdminOrModerator(person.getPersonId(), groupId) && !person.isAdmin())) {
            throw new ForbiddenException(ERROR_PERSON_NOT_AUTHORIZED);
        }
        GroupMember groupMember = groupMemberRepository.findByPersonIdAndGroupId(memberId, groupId)
                .orElseThrow(() -> new NotFoundException("error.not.found.member.for.group", memberId, groupId));

        ApprovalState originalGroupMemberState = groupMember.getState();
        if (!originalGroupMemberState.equals(state)) {
            groupMember.setState(state);
            groupMemberRepository.save(groupMember);

            if (ApprovalState.APPROVED.equals(state)) {
                kafkaProducerService.sendMessageOnFailureThrowError(CommonConstants.GROUP_MEMBER_EVENT, GroupMemberEventDTO
                        .builder()
                        .personId(groupMember.getPersonId())
                        .groupId(groupMember.getGroup().getId())
                        .permissions(List.of(GroupMemberStatus.USER))
                        .eventType(GroupMemberEventType.JOINED)
                        .build());
                GroupActivityEvent.send(rabbitTemplate, new GroupActivityEvent(groupMember.getGroup().getId(), GroupEventType.MEMBER_JOINED.name()));
            }

            // if originally user had state approved, and it changed to another state, it means user not a member anymore
            if (ApprovalState.APPROVED.equals(originalGroupMemberState)) {
                permissionManager.removeUserGroupPermissionsInternal(memberId, groupId);
                GroupActivityEvent.send(rabbitTemplate, new GroupActivityEvent(groupMember.getGroup().getId(), GroupEventType.MEMBER_LEFT.name()));

                kafkaProducerService.sendMessageOnFailureThrowError(CommonConstants.GROUP_MEMBER_EVENT, GroupMemberEventDTO
                        .builder()
                        .personId(groupMember.getPersonId())
                        .groupId(groupMember.getGroup().getId())
                        .permissions(List.of(GroupMemberStatus.USER))
                        .eventType(GroupMemberEventType.LEFT)
                        .build());
            }

            if (ApprovalState.PENDING.equals(originalGroupMemberState)) {
                // Send notification to the user about state change:
                sendGroupMemberStateChangeNotification(person, memberId, groupMember);
            }
        }
    }

    private void sendGroupMemberStateChangeNotification(PersonDTO person, Long memberId, GroupMember groupMember) {
        NotificationType type = ApprovalState.APPROVED.equals(groupMember.getState()) ? NotificationType.GROUP_JOIN_REQUEST_APPROVED
                : NotificationType.GROUP_JOIN_REQUEST_REJECTED;
        NotificationEvent event = NotificationEvent
                .builder()
                .eventAuthor(PersonData
                        .builder()
                        .id(person.getPersonId())
                        .displayName(person.getDisplayName())
                        .avatar(person.getAvatar())
                        .build())
                .receiverIds(Collections.singletonList(memberId))
                .type(type)
                .state(NotificationState.NEW)
                .metadata(Map.of(
                        ROUTE_TO, ContentType.GROUP.name(),
                        GROUP_ID, groupMember.getGroup().getId().toString(),
                        GROUP_NAME, groupMember.getGroup().getName()
                )).build();

        // Send event for notification:
        kafkaProducerService.sendMessage(PUSH_NOTIFICATION, event);
    }

    @Transactional
    @Override
    public void leave(@Nonnull Long personId, @Nonnull Long groupId) {
        Optional<GroupMember> member = groupMemberRepository.findByPersonIdAndGroupId(personId, groupId);
        if (member.isEmpty()) {
            LOGGER.warn("Not found member: {} for the group: {}", personId, groupId);
        } else {
            permissionManager.removeUserGroupPermissionsInternal(personId, groupId);
            groupMemberRepository.delete(member.get());
            if (member.get().getState().equals(ApprovalState.APPROVED)) {

                kafkaProducerService.sendMessageOnFailureThrowError(CommonConstants.GROUP_MEMBER_EVENT, GroupMemberEventDTO
                        .builder()
                        .personId(member.get().getPersonId())
                        .groupId(member.get().getGroup().getId())
                        .permissions(List.of(GroupMemberStatus.USER))
                        .eventType(GroupMemberEventType.LEFT)
                        .build());

                GroupActivityEvent.send(rabbitTemplate, new GroupActivityEvent(member.get().getGroup().getId(), GroupEventType.MEMBER_LEFT.name()));
            }

            LOGGER.info("Person: {} left group: {}", personId, groupId);
        }
    }

    @Transactional(readOnly = true)
    @Override
    public Page<JsonGroupMember> findMembers(@NotNull String authorizationHeader,
                                             @Nonnull Long personId,
                                             List<Long> memberIds,
                                             @NonNull Boolean isAdmin,
                                             @Nonnull Long groupId,
                                             List<ApprovalState> states,
                                             String query,
                                             Pageable page) {
        GroupMemberSearchCriteria criteria = GroupMemberSearchCriteria.builder()
                .groupId(groupId)
                .personIds(memberIds)
                .query(query)
                .build();

        return getGroupMembers(authorizationHeader, personId, isAdmin, criteria, states, page);
    }

    @Override
    public Page<JsonGroupMemberWithNotificationSettings> findMembersWithNotificationSettings(@NotNull Long currentUserId,
                                                                                             @NotNull Long groupId,
                                                                                             Pageable page) {
        // to validate if requested user is a member of the given group
        getMember(groupId, currentUserId);

        GroupMemberSearchCriteria criteria = GroupMemberSearchCriteria.builder()
                .groupId(groupId)
                .states(List.of(ApprovalState.APPROVED))
                .build();

        Page<JsonGroupMemberWithNotificationSettings> groupMembers = groupMemberRepository.findMembers(criteria, page)
                .map(member -> mapper.map(member, JsonGroupMemberWithNotificationSettings.class));
        groupMembers.forEach(m -> {
            try {
                UserGroupNotificationSettingsDTO s = groupNotificationSettingsManager.findGroupNotificationSettingsByGroupId(m.getPersonId(), m.getGroupId());
                m.setMuted(s.isMuted());
            } catch (Exception ex) {
                m.setMuted(false);
            }
        });
        return groupMembers;
    }

    @Override
    public Page<JsonGroupMember> findMutualFriends(@NonNull String authorizationHeader, @Nonnull Long personId,
                                                   @NonNull Boolean isAdmin, Long groupId, String query,
                                                   Pageable page) {
        // Find person's subscriptions:
        List<Long> subscriptionIds = getSubscriptions(authorizationHeader, personId);
        // Find person's subscribers:
        List<Long> subscriberIds = getSubscribers(authorizationHeader, personId);

        Set<Long> personIds = new HashSet<>(subscriberIds);
        personIds.addAll(subscriptionIds);

        if (personIds.isEmpty()) {
            return Page.empty(page);
        }

        GroupMemberSearchCriteria criteria = GroupMemberSearchCriteria.builder()
                .groupId(groupId)
                .personIds(new ArrayList<>(personIds))
                .query(query)
                .build();

        return getGroupMembers(authorizationHeader, personId, isAdmin, criteria, null, page);
    }

    @Override
    public List<GroupMember> findPersonGroupMemberships(Long personId) {
        return groupMemberRepository.findByPersonId(personId);
    }

    @Override
    public List<PersonDTO> findPersonsToTag(Long currentPersonId, Long groupId, String query) {
        List<PersonDTO> personDataList = new ArrayList<>();

        GroupMemberSearchCriteria criteria = GroupMemberSearchCriteria.builder()
                .groupId(groupId)
                .query(query)
                .states(Collections.singletonList(ApprovalState.APPROVED))
                .build();

        Page<GroupMember> groupMembers = groupMemberRepository.findMembers(criteria, PageRequest.of(0, limitForPersonSearch));

        if (CollectionUtils.isNotEmpty(groupMembers.getContent())) {
            List<Long> memberIds = groupMembers.stream().map(GroupMember::getPersonId).toList();
            List<PersonDTO> personDataFromMembers = personManager.getPersonsByPersonIdIn(memberIds);
            personDataList.addAll(personDataFromMembers);
        }

        if (groupMembers.getTotalElements() < limitForPersonSearch) {
            try {
                List<Long> personIdsToExclude = groupMembers.stream().map(GroupMember::getPersonId).toList();
                int queryLimit = (int) (limitForPersonSearch - groupMembers.getTotalElements());
                List<PersonDTO> personList = personManager.getByDisplayNameWithExclusion(query, personIdsToExclude, queryLimit);

                if (!CollectionUtils.isEmpty(personList)) {
                    personDataList.addAll(personList);
                }
            } catch (Exception ex) {
                LOGGER.error("findPostRelatedPersons: error while searching persons from person-service: {}", ex.getMessage());
            }
        }
        personDataList.removeIf(person -> Objects.equals(person.getPersonId(), currentPersonId));
        return personDataList;
    }

    @Nonnull
    @Override
    public GroupMember getMember(Long groupId, Long memberId) {
        return findMember(groupId, memberId)
                .orElseThrow(() -> new NotFoundException("error.not.found.member.for.group", memberId, groupId));
    }

    @Transactional
    @Override
    public void inviteUser(@Nonnull PersonDTO currentUser, @Nonnull Long groupId, @Nonnull Long invitedPersonId) {
        UserGroup group = groupManagerUtils.getGroup(groupId);

        if (!canInvite(currentUser, group)) {
            throw new ForbiddenException("error.person.unauthorized.to.invite.group");
        }

        GroupMember member = findMember(groupId, invitedPersonId).orElseGet(() -> {
            personManager.getPersonByPersonId(invitedPersonId);
            return groupMemberRepository.save(GroupMember.builder()
                    .state(ApprovalState.INVITED)
                    .personId(invitedPersonId)
                    .group(group)
                    .visitedAt(null)
                    .build());
        });

        if (ApprovalState.REJECTED.equals(member.getState())) {
            member.setState(ApprovalState.INVITED);
            groupMemberRepository.save(member);
        }

        if (ApprovalState.INVITED.equals(member.getState())) {
            // Send notification:
            NotificationEvent event = NotificationEvent
                    .builder()
                    .eventAuthor(PersonData
                            .builder()
                            .id(currentUser.getPersonId())
                            .displayName(currentUser.getDisplayName())
                            .avatar(currentUser.getAvatar())
                            .build())
                    .receiverIds(List.of(invitedPersonId))
                    .type(NotificationType.USER_INVITED_TO_GROUP)
                    .metadata(Map.of(
                            ROUTE_TO, ContentType.GROUP.name(),
                            GROUP_ID, group.getId().toString(),
                            GROUP_NAME, group.getName()
                    )).build();
            String logMessage = String.format("User with id=\"%s\" was invited to the group \"%s\"", invitedPersonId, group.getName());
            LOGGER.info(AuditMarker.getMarker(), AuditMessage.create(EventAction.INVITE_TO_GROUP, logMessage, currentUser.getPersonId(), groupId));

            kafkaProducerService.sendMessage(PUSH_NOTIFICATION, event);
        }
    }

    @Transactional
    @Override
    public void removeMemberFromGroups(Long personId) {
        groupMemberRepository.deleteMemberFromGroups(personId);
    }

    @Override
    public Page<UserGroup> findGroupsByStates(Boolean isAdmin, List<ApprovalState> states, Pageable page) {
        if (!isGroupOperationAllowed(states, isAdmin)) {
            throw new ForbiddenException(ERROR_PERSON_NOT_AUTHORIZED);
        }

        return CollectionUtils.isNotEmpty(states) ? repository.findByStateIn(states, page) : repository.findAll(page);
    }

    @Override
    public GroupStats getGroupStats(String fromDate, TimeInterval timeInterval) {
        if (timeInterval == null) {
            timeInterval = TimeInterval.MONTH;
        }
        Timestamp timestamp = StringUtils.isEmpty(fromDate) ? null : Timestamp.valueOf(DateUtil.getDateFromString(fromDate).atStartOfDay());
        long allGroupsCount = repository.getAllGroupsCount();
        long newGroupsCount = repository.getNewGroupsCount(timestamp);

        GroupStats stats = GroupStats.builder()
                .allGroupsCount(allGroupsCount)
                .newGroupsCount(newGroupsCount)
                .fromDate(timestamp)
                .timeInterval(timeInterval)
                .build();

        switch (timeInterval) {
            case DAY -> {
                List<CreatedGroups> createdGroupsPerDay = repository.getCreatedGroupsPerDay(timestamp);
                List<CreateGroupRequests> createGroupRequestsPerDay = repository.getCreateGroupRequestsPerDay(timestamp);
                List<JoinGroupRequests> joinGroupRequestsPerDay = groupMemberRepository.getJoinGroupRequestsPerDay(timestamp);
                stats.setCreatedGroups(createdGroupsPerDay);
                stats.setCreateGroupRequests(createGroupRequestsPerDay);
                stats.setJoinGroupRequests(joinGroupRequestsPerDay);
            }
            case YEAR -> {
                List<CreatedGroups> createdGroupsPerYear = repository.getCreatedGroupsPerYear(timestamp);
                List<CreateGroupRequests> createGroupRequestsPerYear = repository.getCreateGroupRequestsPerYear(timestamp);
                List<JoinGroupRequests> joinGroupRequestsPerYear = groupMemberRepository.getJoinGroupRequestsPerYear(timestamp);
                stats.setCreatedGroups(createdGroupsPerYear);
                stats.setCreateGroupRequests(createGroupRequestsPerYear);
                stats.setJoinGroupRequests(joinGroupRequestsPerYear);
            }
            default -> {
                List<CreatedGroups> createdGroupsPerMonth = repository.getCreatedGroupsPerMonth(timestamp);
                List<CreateGroupRequests> createGroupRequestsPerMonth = repository.getCreateGroupRequestsPerMonth(timestamp);
                List<JoinGroupRequests> joinGroupRequestsPerMonth = groupMemberRepository.getJoinGroupRequestsPerMonth(timestamp);
                stats.setCreatedGroups(createdGroupsPerMonth);
                stats.setCreateGroupRequests(createGroupRequestsPerMonth);
                stats.setJoinGroupRequests(joinGroupRequestsPerMonth);
            }
        }
        return stats;
    }

    @Override
    public Set<Long> getGroupMemberIds(@Nonnull Long personId, @Nonnull Boolean isAdmin, List<Long> groupIds) {
        groupIds = permissionManager.getAccessibleGroups(personId, isAdmin, groupIds);
        if (CollectionUtils.isNotEmpty(groupIds)) {
            List<UserGroup> groups = repository.findAllById(groupIds);
            List<GroupMember> members = groupMemberRepository.findByGroupIn(groups);
            return CollectionUtils.isNotEmpty(members) ? members.stream().filter(m -> ApprovalState.APPROVED.equals(m.getState()))
                    .map(GroupMember::getPersonId).collect(Collectors.toSet())
                    : new HashSet<>();
        }
        return new HashSet<>();
    }

    public GroupMember joinGroup(UserGroup userGroup, Long personId, boolean isAdmin, GroupMemberStatus memberStatus) {
        boolean isPrivateGroup = AccessType.PRIVATE.equals(userGroup.getAccessType());
        boolean isSearchableGroup = GroupVisibility.EVERYONE.equals(userGroup.getVisibility());
        AtomicBoolean isNewMember = new AtomicBoolean(false);
        GroupMember groupMember = groupMemberRepository.findByPersonIdAndGroupId(personId, userGroup.getId())
                .orElseGet(() -> {
                    isNewMember.set(true);
                    return GroupMember.builder()
                            .group(userGroup)
                            .personId(personId)
                            .state(getDefaultState(userGroup, isAdmin))
                            .visitedAt(new Date())
                            .build();
                });

        if (!isNewMember.get() && ApprovalState.APPROVED.equals(groupMember.getState())) {
            return groupMember;
        }

        if (isPrivateGroup && !isSearchableGroup && !ApprovalState.INVITED.equals(groupMember.getState())
                && !GroupMemberStatus.ADMIN.equals(memberStatus)) {
            throw new ForbiddenException(ERROR_PERSON_NOT_AUTHORIZED);
        }

        // Reset state to PENDING if rejected user tries to join again:
        if (ApprovalState.REJECTED.equals(groupMember.getState())) {
            groupMember.setState(ApprovalState.PENDING);
        }
        if (ApprovalState.INVITED.equals(groupMember.getState())) {
            groupMember.setState(ApprovalState.APPROVED);
        }

        groupMember = groupMemberRepository.save(groupMember);

        if (ApprovalState.APPROVED.equals(groupMember.getState())) {

            permissionManager.setPermissionInternal(personId, userGroup, JsonGroupPermissionData
                    .builder()
                    .permission(memberStatus)
                    .personId(personId)
                    .build());

            kafkaProducerService.sendMessage(CommonConstants.GROUP_MEMBER_EVENT, GroupMemberEventDTO
                    .builder()
                    .personId(groupMember.getPersonId())
                    .groupId(groupMember.getGroup().getId())
                    .permissions(List.of(GroupMemberStatus.USER))
                    .eventType(GroupMemberEventType.JOINED)
                    .build());

            GroupActivityEvent.send(rabbitTemplate, new GroupActivityEvent(groupMember.getGroup().getId(), GroupEventType.MEMBER_JOINED.name()));

        }

        return groupMember;
    }

    private void sendJoinRequestNotification(PersonDTO person, UserGroup group) {
        // Request to join group notification should be sent to group admins and group moderators:
        List<Long> receiverIds = getGroupModeratorsAndAdmins(group.getId());

        NotificationEvent event = NotificationEvent
                .builder()
                .eventAuthor(PersonData
                        .builder()
                        .id(person.getPersonId())
                        .displayName(person.getDisplayName())
                        .avatar(person.getAvatar())
                        .build())
                .receiverIds(receiverIds)
                .type(NotificationType.GROUP_JOIN_REQUESTED)
                .metadata(Map.of(
                        ROUTE_TO, ContentType.GROUP.name(),
                        GROUP_ID, group.getId().toString(),
                        GROUP_NAME, group.getName()
                )).build();

        // Send event for notification:
        kafkaProducerService.sendMessage(PUSH_NOTIFICATION, event);
    }

    private void removeMediaFiles(UserGroup userGroup, Long personId, Boolean isAdmin) {
        mediaService.removeAvatar(userGroup, personId, isAdmin);
        mediaService.removeCover(userGroup, personId, isAdmin);
    }

    private Optional<GroupMember> findMember(Long groupId, Long memberId) {
        return groupMemberRepository.findByPersonIdAndGroupId(memberId, groupId);
    }

    private UserGroup fillData(UserGroup group, GroupData data, boolean isAdmin) {

        group.setName(firstNonNull(data.getName(), group.getName()));
        group.setDescription(firstNonNull(data.getDescription(), group.getDescription()));
        group.setRules(firstNonNull(data.getRules(), group.getRules()));
        if (group.getId() != null) {
            group.setState(firstNonNull(data.getState(), group.getState()));
        }

        if (Objects.nonNull(data.getTags())) {
            group.setTags(new HashSet<>(tagRepository.findAllById(data.getTags())));
        }

        if (Objects.nonNull(data.getAccessType())) {
            group.setAccessType(data.getAccessType());
        }

        if (Objects.nonNull(data.getPostingPermission())) {
            group.setPostingPermission(data.getPostingPermission());
        }

        if (Objects.nonNull(data.getInvitePermission()) && isAdmin) {
            group.setInvitePermission(data.getInvitePermission());
        }

        if (Objects.nonNull(data.getCategories())) {
            List<Category> categories = categoryRepository.findAllById(data.getCategories());
            if (categories.isEmpty()) {
                throw new BadRequestException("error.group.categories.are.required");
            }

            checkIfAssignable(categories);
            group.setCategories(new HashSet<>(categories));
        }

        group.setVisibility(data.getVisibility() == null || AccessType.PUBLIC.equals(group.getAccessType()) ?
                GroupVisibility.EVERYONE : data.getVisibility());

        return group;
    }

    private boolean canInvite(@NonNull PersonDTO currentUser, @NonNull UserGroup group) {
        // Admin user or group moderator can invite people to group with invite permission ADMIN.
        // Admin user, group moderator or group member can invite people to group with invite permission MEMBER.
        return switch (group.getInvitePermission()) {
            case ADMIN -> currentUser.isAdmin() || isGroupAdminOrModerator(currentUser.getPersonId(), group.getId());
            case MEMBER -> currentUser.isAdmin() || isGroupAdminOrModerator(currentUser.getPersonId(), group.getId())
                    || isGroupMember(currentUser.getPersonId(), group.getId());
        };
    }

    private boolean isGroupAdminOrModerator(Long memberId, Long groupId) {
        List<GroupMemberStatus> permissions = List.of(GroupMemberStatus.ADMIN, GroupMemberStatus.MODERATOR);
        List<GroupPermission> groupPermissions = permissionManager.findPermissionsInternal(PermissionSearchCriteria
                .builder()
                .groupIds(List.of(groupId))
                .personId(memberId)
                .statuses(permissions)
                .build());
        return !groupPermissions.isEmpty();
    }

    private boolean isGroupMember(Long memberId, Long groupId) {
        boolean isMember = findMember(groupId, memberId).isPresent();
        List<GroupPermission> groupPermissions = permissionManager.findPermissionsInternal(memberId, groupId);
        return isMember && !groupPermissions.isEmpty();
    }

    private boolean isGroupAdmin(Long groupId, Long memberId) {
        GroupMember member = groupMemberRepository.findActiveMember(groupId, memberId);
        return !Objects.isNull(member) && permissionManager.hasPermission(memberId, groupId, GroupMemberStatus.ADMIN);
    }

    private Page<JsonGroupMember> getGroupMembers(
            String authorizationHeader, @Nonnull Long personId, @NonNull Boolean isAdmin, GroupMemberSearchCriteria criteria,
            List<ApprovalState> states, Pageable page) {
        if (Objects.nonNull(states)) {
            if (!isGroupOperationAllowed(personId, isAdmin, criteria.getGroupId(), states)) {
                throw new ForbiddenException(ERROR_PERSON_NOT_AUTHORIZED);
            }
            criteria.setStates(states);
        } else {
            criteria.setStates(Collections.singletonList(ApprovalState.APPROVED));
        }
        var groupMembers = groupMemberRepository.findMembers(criteria, page)
                .map(member -> mapper.map(member, JsonGroupMember.class));

        var personIds = groupMembers.stream().map(JsonGroupMember::getPersonId).toArray(Long[]::new);
        var persons = personManager.getPersonsByPersonIdIn(Arrays.stream(personIds).toList());
        RestPageImpl<JsonPerson> personList = null;
        if (personIds.length > 0) {
            try {
                int queryLimit = personIds.length;
                personList = followingRestService.findSubscriptionsV2(authorizationHeader, personId, personIds, 0, queryLimit);
            } catch (Exception ex) {
                LOGGER.error("Error while fetching followings from the person service", ex);
            }
            RestPageImpl<JsonPerson> finalPersonList = personList;
            groupMembers.forEach(member -> {
                        if (finalPersonList != null) {
                            finalPersonList.stream()
                                    .filter(person1 -> person1.getId().equals(member.getPersonId()))
                                    .findFirst()
                                    .ifPresent(person1 -> member.setFollowing(true));
                        }
                        if (persons != null) {
                            persons.stream()
                                    .filter(personDTO -> personDTO.getPersonId().equals(member.getPersonId()))
                                    .findFirst()
                                    .ifPresent(person -> {
                                        member.setDisplayName(person.getDisplayName());
                                        member.setPerson(person);
                                    });
                        }
                    }
            );
        }
        return groupMembers;
    }

    private boolean isGroupOperationAllowed(Long personId, boolean isUserAdmin, Long groupId, List<ApprovalState> states) {
        boolean isGroupAdmin = isGroupAdminOrModerator(personId, groupId) || isUserAdmin;
        return isGroupOperationAllowed(states, isGroupAdmin);
    }

    private boolean isGroupOperationAllowed(List<ApprovalState> states, Boolean isAdmin) {
        return (CollectionUtils.isNotEmpty(states) && !containsPendingOrRejected(states)) || isAdmin;
    }

    private boolean containsPendingOrRejected(List<ApprovalState> states) {
        EnumSet<ApprovalState> pendingRejectedStates = EnumSet.of(ApprovalState.PENDING, ApprovalState.REJECTED);
        return states.stream().anyMatch(pendingRejectedStates::contains);
    }

    private ApprovalState getDefaultState(UserGroup userGroup, boolean isAdmin) {
        return isAdmin || AccessType.PUBLIC.equals(userGroup.getAccessType()) ? ApprovalState.APPROVED : ApprovalState.PENDING;
    }

    private GroupMember getGroupMember(Long groupId, Long personId) {
        try {
            return getMember(groupId, personId);
        } catch (Exception ex) {
            LOGGER.info("Person is not a member of the group {}", groupId);
            return null;
        }
    }

    private List<Long> getGroupModeratorsAndAdmins(Long userGroupId) {
        List<GroupMemberStatus> permissions = List.of(GroupMemberStatus.ADMIN, GroupMemberStatus.MODERATOR);
        PermissionSearchCriteria criteria = PermissionSearchCriteria.builder()
                .groupIds(Collections.singletonList(userGroupId))
                .statuses(permissions)
                .build();

        List<GroupPermission> groupPermissions = permissionManager.findPermissionsInternal(criteria);

        return groupPermissions.stream().map(GroupPermission::getPersonId).distinct().toList();
    }

    private void updateQuery(@Nonnull GroupSearchCriteria criteria) {
        String query = criteria.getQuery();
        if (Objects.nonNull(query)) {
            criteria.setQuery("%" + query.toLowerCase().trim().replaceAll("\\s+", "%") + "%");
        } else {
            criteria.setQuery("%");
        }
    }

    private UserGroupDto getUserGroupDTOFromEntity(UserGroup group) {
        var userGroupDTO = UserGroupDto
                .builder()
                .id(group.getId())
                .name(group.getName())
                .postingPermission(group.getPostingPermission())
                .accessType(group.getAccessType())
                .createdAt(group.getCreatedAt())
                .build();
        if (group.getAvatar() != null) {
            var avatar = group.getAvatar();
            Set<SizedImage> sizedImageSet = avatar.getSizedImages();

            Map<String, List<JsonSizedImage>> sizedImages = sizedImageSet.stream()
                    .map(sizedImage -> JsonSizedImage
                            .builder()
                            .imageSizeType(sizedImage.getImageSizeType())
                            .size(sizedImage.getSize())
                            .path(sizedImage.getPath())
                            .createdAt(sizedImage.getCreatedAt())
                            .mimeType(sizedImage.getMimeType())
                            .build()
                    ).collect(groupingBy(image -> image.getMimeType().replace("image/", "")));
            var jsonAvatar = JsonMediaFile
                    .builder()
                    .id(avatar.getId())
                    .path(avatar.getPath())
                    .mimeType(avatar.getMimeType())
                    .size(avatar.getSize())
                    .fileType(avatar.getFileType())
                    .createdAt(avatar.getCreatedAt())
                    .sizedImages(sizedImages)
                    .build();
            userGroupDTO.setAvatar(jsonAvatar);
        }
        if (group.getCover() != null) {
            var cover = group.getCover();
            Set<SizedImage> sizedImageSet = cover.getSizedImages();

            Map<String, List<JsonSizedImage>> sizedImages = sizedImageSet.stream()
                    .map(sizedImage -> JsonSizedImage
                            .builder()
                            .imageSizeType(sizedImage.getImageSizeType())
                            .size(sizedImage.getSize())
                            .path(sizedImage.getPath())
                            .createdAt(sizedImage.getCreatedAt())
                            .mimeType(sizedImage.getMimeType())
                            .build()
                    ).collect(groupingBy(image -> image.getMimeType().replace("image/", "")));
            var jsonCover = JsonMediaFile
                    .builder()
                    .id(cover.getId())
                    .path(cover.getPath())
                    .mimeType(cover.getMimeType())
                    .size(cover.getSize())
                    .fileType(cover.getFileType())
                    .createdAt(cover.getCreatedAt())
                    .sizedImages(sizedImages)
                    .build();
            userGroupDTO.setCover(jsonCover);
        }
        return userGroupDTO;
    }

    @NotNull
    private List<JsonGroupPermission> getJsonGroupPermissions(UserGroupDto group) {
        getGroupModeratorsAndAdmins(group.getId());
        List<GroupMemberStatus> statuses = List.of(GroupMemberStatus.ADMIN, GroupMemberStatus.MODERATOR);
        List<GroupPermission> permissions = permissionManager.findPermissionsInternal(PermissionSearchCriteria
                .builder()
                .groupIds(Collections.singletonList(group.getId()))
                .statuses(statuses)
                .build());
        return permissions.stream()
                .map(groupPermission -> mapper.map(groupPermission, JsonGroupPermission.class)).toList();
    }

    //todo: remove this after production deployment
    @Scheduled(cron = "${social.groupservice.migrateGroups}")
    @Transactional
    public void migrateAllGroupsAndMembers() {
        LOGGER.info("Running scheduler to push all group, members and notification settings to kafka topic");
        try {
            Long flag = migrationFlag.opsForValue().get("groupMigrationFlag");
            Long memberMigrationFlag = migrationFlag.opsForValue().get("groupMemberMigrationFlag");
            Long personFlag = migrationFlag.opsForValue().get("personsMigrationFlag");
            Long notificationSettingsFlag = migrationFlag.opsForValue().get("notificationSettingsMigrationFlag");

            if ((Objects.isNull(memberMigrationFlag) || memberMigrationFlag == 0) && (!Objects.isNull(flag) && flag == 1L)) {
                var groupMembers = getAllGroupMembers();
                if (!org.springframework.util.CollectionUtils.isEmpty(groupMembers)) {
                    groupMembers.forEach(groupMember -> kafkaProducerService.sendMessageOnFailureThrowError(CommonConstants.GROUP_MEMBER_EVENT, GroupMemberEventDTO
                            .builder()
                            .personId(groupMember.getPersonId())
                            .groupId(groupMember.getGroupId())
                            .permissions(groupMember.getStatuses())
                            .eventType(GroupMemberEventType.JOINED)
                            .build()));
                }
                migrationFlag.opsForValue().set("groupMemberMigrationFlag", 1L);
            }

            if ((Objects.isNull(flag) || flag == 0) && (!Objects.isNull(personFlag) && personFlag == 1L)) {
                var groups = findGroupsInternal();
                if (!org.springframework.util.CollectionUtils.isEmpty(groups)) {
                    groups.forEach(group -> kafkaProducerService.sendMessageOnFailureThrowError(CommonConstants.GROUP_CREATED, group));
                }

                migrationFlag.opsForValue().set("groupMigrationFlag", 1L);
            }

            if ((Objects.isNull(notificationSettingsFlag) || notificationSettingsFlag == 0) && (!Objects.isNull(flag) && flag == 1L)) {
                var userGroupNotificationSettings = groupNotificationSettingsManager.getAllGroupNotificationSettings();
                if (!org.springframework.util.CollectionUtils.isEmpty(userGroupNotificationSettings)) {
                    userGroupNotificationSettings.forEach(userGroupNotificationSetting ->
                            kafkaProducerService.sendMessageOnFailureThrowError(CommonConstants.USER_GROUP_NOTIFICATION_SETTINGS_SET, userGroupNotificationSetting));
                }
                migrationFlag.opsForValue().set("notificationSettingsMigrationFlag", 1L);
            }

        } catch (Exception ex) {
            LOGGER.error("Error while pushing groups and members details to kafka: {}", ex.getMessage());
        }
    }

    // Internal method only used by post service for migration
    private List<UserGroupDto> findGroupsInternal() {
        return repository.findAll().stream()
                .map(this::getUserGroupDTOFromEntity).toList();
    }

    // Internal method only used by post service for migration
    private List<JsonMemberPermission> getAllGroupMembers() {
        return groupMemberRepository.getAllActiveGroupMembers().stream().map(groupMember -> {
                    var memberWithPermission = JsonMemberPermission
                            .builder()
                            .id(groupMember.getId())
                            .groupId(groupMember.getGroup().getId())
                            .personId(groupMember.getPersonId())
                            .state(groupMember.getState())
                            .build();
                    var permissions = permissionManager.findPermissionsInternal(groupMember.getPersonId(), groupMember.getGroup().getId());
                    if (CollectionUtils.isNotEmpty(permissions)) {
                        memberWithPermission.setStatuses(permissions.stream().map(GroupPermission::getPermission).toList());
                    }
                    return memberWithPermission;
                })
                .toList();
    }
}
