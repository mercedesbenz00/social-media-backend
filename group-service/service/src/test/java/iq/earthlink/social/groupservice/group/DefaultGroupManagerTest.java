package iq.earthlink.social.groupservice.group;

import iq.earthlink.social.classes.enumeration.FilterType;
import iq.earthlink.social.classes.enumeration.SortType;
import iq.earthlink.social.classes.enumeration.TimeInterval;
import iq.earthlink.social.common.filestorage.CompositeFileStorageProvider;
import iq.earthlink.social.common.filestorage.minio.MinioFileStorage;
import iq.earthlink.social.common.rest.RestPageImpl;
import iq.earthlink.social.exception.ForbiddenException;
import iq.earthlink.social.exception.RestApiException;
import iq.earthlink.social.groupservice.category.Category;
import iq.earthlink.social.groupservice.category.CategoryLocalized;
import iq.earthlink.social.groupservice.category.DefaultCategoryManager;
import iq.earthlink.social.groupservice.category.repository.CategoryRepository;
import iq.earthlink.social.groupservice.event.KafkaProducerService;
import iq.earthlink.social.groupservice.group.dto.GroupStats;
import iq.earthlink.social.groupservice.group.dto.JsonGroupMember;
import iq.earthlink.social.groupservice.group.member.GroupMember;
import iq.earthlink.social.groupservice.group.member.GroupMemberRepository;
import iq.earthlink.social.groupservice.group.permission.GroupPermission;
import iq.earthlink.social.groupservice.group.permission.GroupPermissionManager;
import iq.earthlink.social.groupservice.group.permission.repository.GroupPermissionRepository;
import iq.earthlink.social.groupservice.group.rest.*;
import iq.earthlink.social.groupservice.group.rest.enumeration.AccessType;
import iq.earthlink.social.groupservice.group.rest.enumeration.ApprovalState;
import iq.earthlink.social.groupservice.group.rest.enumeration.GroupVisibility;
import iq.earthlink.social.groupservice.group.rest.enumeration.InvitePermission;
import iq.earthlink.social.groupservice.person.PersonManager;
import iq.earthlink.social.groupservice.person.dto.PersonDTO;
import iq.earthlink.social.groupservice.tag.TagRepository;
import iq.earthlink.social.groupservice.util.RoleUtil;
import iq.earthlink.social.personservice.person.rest.JsonFollowing;
import iq.earthlink.social.personservice.person.rest.JsonPerson;
import iq.earthlink.social.personservice.person.rest.JsonPersonProfile;
import iq.earthlink.social.personservice.rest.FollowingRestService;
import iq.earthlink.social.security.SecurityProvider;
import iq.earthlink.social.security.config.ServerAuthProperties;
import org.dozer.DozerBeanMapperBuilder;
import org.dozer.Mapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.*;

class DefaultGroupManagerTest {

    private static final String GROUP = "group";
    private static final String TEST_GROUP = "Test group";
    private static final String AUTHORIZATION_HEADER = "authorizationHeader";
    private static final String GROUP_1 = "group 1";
    private static final String GROUP_2 = "group 2";
    private static final String GROUP_3 = "group 3";
    private static final String GROUP_4 = "group 4";
    private static final String GROUP_5 = "group 5";
    private static final String GROUP_6 = "group 6";

    @InjectMocks
    private DefaultGroupManager groupManager;
    @Mock
    private GroupManagerUtils groupManagerUtils;
    @Mock
    private CachingConnectionFactory connectionFactory;
    @Mock
    private RabbitTemplate rabbitTemplate;
    @Mock
    private MinioFileStorage minioFileStorage;
    @Mock
    private CompositeFileStorageProvider compositeFileStorageProvider;
    @Mock
    private GroupRepository groupRepository;
    @Mock
    private GroupMemberRepository groupMemberRepository;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private GroupPermissionRepository groupPermissionRepository;
    @Mock
    private TagRepository tagRepository;
    @Mock
    private GroupMediaService mediaService;
    @Mock
    private PersonManager personManager;
    @Mock
    private SecurityProvider securityProvider;
    @Mock
    private RoleUtil roleUtil;
    @Mock
    private FollowingRestService followingRestService;
    @Mock
    private GroupPermissionManager permissionManager;
    @Mock
    private DefaultCategoryManager categoryManager;
    @Mock
    private ServerAuthProperties authProperties;
    @Mock
    private KafkaProducerService kafkaProducerService;
    @Captor
    private ArgumentCaptor<UserGroup> userGroupArgumentCaptor;
    @Spy
    private Mapper mapper = DozerBeanMapperBuilder.create().build();

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
        ReflectionTestUtils.setField(groupManager, "limitForPersonSearch", 10);
    }

    @Test
    void findGroupsBySearchCriteria_getByValidCriteria_returnPaginatedResult() {
        //given
        GroupSearchCriteria criteria1 = GroupSearchCriteria.builder()
                .query(GROUP)
                .groupIds(new Long[]{1L, 2L})
                .states(List.of(ApprovalState.APPROVED))
                .subscribedToIds(List.of(2L, 3L))
                .categoryIds(new Long[]{1L})
                .build();

        List<UserGroupModel> groups = getUserGroupModels().stream().filter(p -> criteria1.getStates().contains(p.getGroup().getState())
                && p.getGroup().getName().contains(criteria1.getQuery())).collect(Collectors.toList());

        Page<UserGroupModel> page1 = new PageImpl<>(groups, PageRequest.of(0, 3), groups.size());

        given(groupRepository.findGroupsOrderedBySimilarity(criteria1, page1.getPageable())).willReturn(page1);

        //when
        Page<UserGroup> foundGroups = groupManager.findGroupsBySearchCriteria(criteria1, page1.getPageable());

        //then
        assertTrue(foundGroups.isFirst());
        assertEquals(2, foundGroups.getTotalPages());
        assertEquals(groups.size(), foundGroups.getContent().size());
        assertEquals(groups.size(), foundGroups.getTotalElements());
    }

    @Test
    void findGroupsByFilterType_getByValidCriteriaAndFilterType_returnPaginatedResult() {
        //given
        PersonDTO person = getPerson();

        GroupSearchCriteria criteria = GroupSearchCriteria.builder()
                .query(GROUP)
                .groupIds(new Long[]{1L, 2L})
                .states(List.of(ApprovalState.APPROVED))
                .subscribedToIds(List.of(2L, 3L))
                .categoryIds(new Long[]{1L})
                .build();

        List<Long> groupCategoryIds = List.of(2L);

        List<UserGroup> similarGroups = getUserGroups().stream().filter(g -> {
            Set<Category> categories = g.getCategories();
            List<Long> categoryIds = categories.stream().map(Category::getId).collect(Collectors.toList());
            return categoryIds.containsAll(groupCategoryIds);
        }).collect(Collectors.toList());

        List<UserGroup> suggestedGroups = getGroupMembers().stream().filter(gm -> criteria.getSubscribedToIds().contains(gm.getPersonId()))
                .map(GroupMember::getGroup).collect(Collectors.toList());

        Page<UserGroup> page1 = new PageImpl<>(similarGroups, PageRequest.of(0, 3), similarGroups.size());
        Page<UserGroup> page2 = new PageImpl<>(suggestedGroups, PageRequest.of(0, 3), suggestedGroups.size());

        given(groupRepository.findSimilarGroups(person.getPersonId(), null, page1.getPageable())).willReturn(page1);
        given(categoryManager.findCategoriesInternal(any(), any())).willReturn(Page.empty());
        given(groupRepository.findSuggestedGroups(person.getPersonId(), criteria.getSubscribedToIds(), page2.getPageable())).willReturn(page2);

        Page<UserGroup> foundSimilarGroups = groupManager.findGroupsByFilterType(person.getPersonId(), criteria, FilterType.SIMILAR, page1.getPageable());
        Page<UserGroup> foundSuggestedGroups = groupManager.findGroupsByFilterType(person.getPersonId(), criteria, FilterType.SUGGESTED, page2.getPageable());

        //then
        assertTrue(foundSimilarGroups.isFirst());
        assertEquals(1, foundSimilarGroups.getTotalPages());
        assertEquals(similarGroups.size(), foundSimilarGroups.getContent().size());
        assertEquals(similarGroups.size(), foundSimilarGroups.getTotalElements());

        assertTrue(foundSuggestedGroups.isFirst());
        assertEquals(1, foundSuggestedGroups.getTotalPages());
        assertEquals(suggestedGroups.size(), foundSuggestedGroups.getContent().size());
        assertEquals(suggestedGroups.size(), foundSuggestedGroups.getTotalElements());
    }

    @Test
    void createGroup_createByGroupDataWithEmptyCategory_throwRestApiException() {
        //given
        JsonGroupData data = new JsonGroupData();
        data.setName(TEST_GROUP);
        data.setVisibility(GroupVisibility.EVERYONE);
        data.setCategories(null);
        //when
        //then
        assertThatThrownBy(() -> groupManager.createGroup(AUTHORIZATION_HEADER, data))
                .isInstanceOf(RestApiException.class)
                .hasMessageContaining("error.check.not.null");
    }

    @Test
    void createGroup_createByValidDataWithAdminRole_returnPublishedUserGroup() {
        //given
        JsonGroupData data = new JsonGroupData();
        data.setName(TEST_GROUP);
        data.setVisibility(GroupVisibility.EVERYONE);
        HashSet<Long> categoriesSet = new HashSet<>();
        categoriesSet.add(2L);
        data.setCategories(categoriesSet);

        UserGroup group = UserGroup.builder()
                .id(1L)
                .name(GROUP_1)
                .state(ApprovalState.APPROVED)
                .categories(getCategories(List.of(2L)))
                .build();

        GroupMember groupMember = getGroupMembers().stream().filter(g -> g.getId().equals(1L)).findFirst().orElseThrow();

        given(securityProvider.getPersonIdFromAuthorization(anyString())).willReturn(1L);
        given(securityProvider.getRolesFromAuthorization(anyString())).willReturn(new String[]{"ADMIN"});
        given(roleUtil.isAdmin(any())).willReturn(true);
        given(groupRepository.save(any())).willReturn(group);
        given(categoryRepository.findAllById(data.getCategories())).willReturn(new ArrayList<>(getCategories(List.of(2L))));
        given(groupPermissionRepository.findByPersonIdAndUserGroupIdAndPermissionIn(any(), any(), any())).willReturn(Collections.emptyList());
        given(groupMemberRepository.findByPersonIdAndGroupId(any(), any())).willReturn(Optional.of(groupMember));
        given(groupMemberRepository.save(any())).willReturn(groupMember);

        //when
        var createdGroup = groupManager.createGroup(AUTHORIZATION_HEADER, data);

        //then
        then(groupRepository).should().save(userGroupArgumentCaptor.capture());
        verify(groupRepository, times(1)).save(any(UserGroup.class));
        assertEquals(group.getId(), createdGroup.getId());
        assertEquals(ApprovalState.APPROVED, userGroupArgumentCaptor.getValue().getState());
    }

    @Test
    void createGroup_createByValidDataWithNonAdminRole_returnPendingUserGroup() {
        //given
        JsonGroupData data = new JsonGroupData();
        data.setName(TEST_GROUP);
        data.setVisibility(GroupVisibility.EVERYONE);
        HashSet<Long> categoriesSet = new HashSet<>();
        categoriesSet.add(2L);
        data.setCategories(categoriesSet);

        UserGroup group = UserGroup.builder()
                .id(1L)
                .name(GROUP_1)
                .state(ApprovalState.PENDING)
                .categories(getCategories(List.of(2L)))
                .build();

        GroupMember groupMember = getGroupMembers().stream().filter(g -> g.getId().equals(1L)).findFirst().orElseThrow();

        given(securityProvider.getPersonIdFromAuthorization(anyString())).willReturn(1L);
        given(securityProvider.getRolesFromAuthorization(anyString())).willReturn(new String[]{"User"});
        given(roleUtil.isAdmin(any())).willReturn(false);
        given(groupRepository.save(any())).willReturn(group);
        given(categoryRepository.findAllById(data.getCategories())).willReturn(new ArrayList<>(getCategories(List.of(2L))));
        given(groupPermissionRepository.findByPersonIdAndUserGroupIdAndPermissionIn(any(), any(), any())).willReturn(Collections.emptyList());
        given(groupMemberRepository.findByPersonIdAndGroupId(any(), any())).willReturn(Optional.of(groupMember));
        given(groupMemberRepository.save(any())).willReturn(groupMember);

        //when
        var createdGroup = groupManager.createGroup(AUTHORIZATION_HEADER, data);

        //then
        then(groupRepository).should().save(userGroupArgumentCaptor.capture());
        verify(groupRepository, times(1)).save(any(UserGroup.class));
        assertEquals(group.getId(), createdGroup.getId());
        assertEquals(ApprovalState.PENDING, userGroupArgumentCaptor.getValue().getState());

    }

    @Test
    void updateGroup_updateByGroupAdminPersonExistingGroup_returnUpdatedUserGroup() {
        //given
        PersonDTO person = getPerson();

        JsonGroupData data = new JsonGroupData();
        data.setName(TEST_GROUP);
        data.setVisibility(GroupVisibility.EVERYONE);
        HashSet<Long> categoriesSet = new HashSet<>();
        categoriesSet.add(2L);
        data.setCategories(categoriesSet);
        data.setAccessType("PUBLIC");

        UserGroup group = UserGroup.builder()
                .id(1L)
                .name(data.getName())
                .state(ApprovalState.APPROVED)
                .categories(getCategories(List.of(2L)))
                .accessType(AccessType.PUBLIC)
                .build();

        GroupMember groupMember = getGroupMembers().stream().filter(g -> g.getId().equals(1L)).findFirst().orElseThrow();

        given(groupRepository.saveAndFlush(any())).willReturn(group);
        given(groupManagerUtils.getGroup(group.getId())).willReturn(group);
        given(categoryRepository.findAllById(data.getCategories())).willReturn(new ArrayList<>(getCategories(List.of(2L))));
        given(groupPermissionRepository.findByPersonIdAndUserGroupIdAndPermissionIn(any(), any(), any())).willReturn(Collections.emptyList());
        given(groupMemberRepository.findByPersonIdAndGroupId(any(), any())).willReturn(Optional.of(groupMember));
        given(groupMemberRepository.findActiveMember(any(), any())).willReturn(groupMember);
        given(permissionManager.hasPermission(any(), any(), eq(GroupMemberStatus.ADMIN))).willReturn(true);

        //when
        UserGroup updatedGroup = groupManager.updateGroup(person.getPersonId(), person.isAdmin(), data, group.getId());

        //then
        assertEquals(group, updatedGroup);
    }

    @Test
    void deleteGroup_deleteByGroupAdminPersonExistingGroup_returnUpdatedUserGroup() {
        //given
        PersonDTO person = getPerson();

        UserGroup group = UserGroup.builder()
                .id(1L)
                .name(GROUP_1)
                .state(ApprovalState.APPROVED)
                .categories(getCategories(List.of(2L)))
                .build();

        GroupMember groupMember = getGroupMembers().stream().filter(g -> g.getId().equals(1L)).findFirst().orElseThrow();
        List<GroupMember> groupMembers = getGroupMembers();
        Page<GroupMember> page = new PageImpl<>(groupMembers, PageRequest.of(0, 3), groupMembers.size());

        given(groupRepository.findById(group.getId())).willReturn(Optional.of(group));
        given(groupMemberRepository.findActiveMember(any(), any())).willReturn(groupMember);
        given(permissionManager.hasPermission(any(), any(), eq(GroupMemberStatus.ADMIN))).willReturn(true);
        given(groupMemberRepository.findMembers(any(), any())).willReturn(page);
        doNothing().when(rabbitTemplate).convertAndSend(any(), any(), (Object) any());

        //when
        groupManager.deleteGroup(person.getPersonId(), person.isAdmin(), group.getId());

        //then
        verify(groupRepository, times(1)).delete(any());
    }

    @Test
    void findMyGroups_findBySpecificPerson_returnPaginatedResult() {
        //given
        PersonDTO person = getPerson();

        GroupSearchCriteria criteria = GroupSearchCriteria.builder()
                .query(GROUP)
                .groupIds(new Long[]{1L, 2L})
                .states(List.of(ApprovalState.APPROVED))
                .subscribedToIds(List.of(2L, 3L))
                .categoryIds(new Long[]{1L})
                .build();

        List<GroupMember> groupMembers = getGroupMembers();
        GroupMember groupMember = groupMembers.stream()
                .filter(member -> member.getPersonId().equals(person.getPersonId()))
                .findFirst().orElseThrow();

        List<MemberUserGroupModel> groups = getMemberUserGroupModel().stream()
                .filter(groupModel -> criteria.getStates().contains(groupModel.getGroup().getState())
                        && groupModel.getGroup().getName().contains(criteria.getQuery())
                        && groupModel.getGroup().getId().equals(groupMember.getGroup().getId()))
                .collect(Collectors.toList());

        Page<MemberUserGroupModel> page = new PageImpl<>(groups, PageRequest.of(0, 3), groups.size());

        given(groupRepository.findMyGroupsOrderedBySimilarity(person.getPersonId(), criteria, page.getPageable())).willReturn(page);

        //when
        Page<MemberUserGroupDto> foundGroups = groupManager.findMyGroups(AUTHORIZATION_HEADER, person.getPersonId(), criteria, page.getPageable());

        //then
        assertTrue(foundGroups.isFirst());
        assertEquals(1, foundGroups.getTotalPages());
        assertEquals(groups.size(), foundGroups.getContent().size());
        assertEquals(groups.size(), foundGroups.getTotalElements());
    }

    @Test
    void findGroupsBySortType_findBySortTypeTop_returnPaginatedResultOfTopGroups() {
        //given
        SortType sortType = SortType.TOP;
        Pageable pageable = PageRequest.of(0, 3);

        long i = 1;
        List<UserGroupModel> userGroups = getUserGroupModels();
        for (UserGroupModel userGroupModel : userGroups) {
            userGroupModel.getGroup().setStats(UserGroupStats.builder().score(i++).build());
        }

        userGroups.sort(Comparator.comparingLong(o -> o.getGroup().getStats().getScore() * -1));
        Page<UserGroupModel> page = new PageImpl<>(userGroups, PageRequest.of(0, 3), userGroups.size());

        given(groupRepository.findOrderedGroups(eq(UserGroupStats.SCORE_CONST), any())).willReturn(page);

        //when
        Page<UserGroup> topGroups = groupManager.findGroupsBySortType(sortType, pageable);

        //then
        assertEquals(2, topGroups.getTotalPages());
        assertEquals(userGroups.size(), topGroups.getContent().size());
        assertEquals(userGroups.size(), topGroups.getTotalElements());
        assertEquals(6, topGroups.getContent().get(0).getStats().getScore());
    }

    @Test
    void findGroupsBySortType_findBySortTypePopular_returnPaginatedResultOfPopularGroups() {
        //given
        SortType sortType = SortType.POPULAR;
        Pageable pageable = PageRequest.of(0, 3);

        long i = 1;
        List<UserGroupModel> userGroups = getUserGroupModels();
        for (UserGroupModel userGroupModel : userGroups) {
            userGroupModel.getGroup().setStats(UserGroupStats.builder().membersCount(i++).build());
        }

        userGroups.sort(Comparator.comparingLong(o -> o.getGroup().getStats().getMembersCount() * -1));
        Page<UserGroupModel> page = new PageImpl<>(userGroups, PageRequest.of(0, 3), userGroups.size());

        given(groupRepository.findOrderedGroups(eq(UserGroupStats.MEMBERS_COUNT_CONST), any())).willReturn(page);

        //when
        Page<UserGroup> topGroups = groupManager.findGroupsBySortType(sortType, pageable);

        //then
        assertEquals(2, topGroups.getTotalPages());
        assertEquals(userGroups.size(), topGroups.getContent().size());
        assertEquals(userGroups.size(), topGroups.getTotalElements());
        assertEquals(6, topGroups.getContent().get(0).getStats().getMembersCount());
    }

    @Test
    void getGroupDto_getByValidData_returnUserGroupDto() {
        //given
        PersonDTO person = getPerson();

        UserGroup group = UserGroup.builder()
                .id(1L)
                .name(GROUP_1)
                .state(ApprovalState.APPROVED)
                .categories(getCategories(List.of(2L)))
                .build();

        GroupMember groupMember = GroupMember.builder()
                .personId(person.getPersonId())
                .group(group)
                .state(ApprovalState.INVITED)
                .build();

        String[] personRoles = new String[]{"ADMIN"};

        given(groupMemberRepository.findByPersonIdAndGroupId(any(), any())).willReturn(Optional.ofNullable(groupMember));
        given(groupManagerUtils.getGroup(any())).willReturn(group);

        //when
        UserGroupDto groupDto = groupManager.getGroupDto(person.getPersonId(), personRoles, group.getId());

        //then
        assertEquals(group.getId(), groupDto.getId());
        assertEquals(group.getName(), groupDto.getName());
        assertEquals(ApprovalState.INVITED, groupDto.getMemberState());
    }

    @Test
    void join_joinToApprovedGroup_returnGroupMember() {
        //given
        PersonDTO person = getPerson();
        UserGroup group = UserGroup.builder()
                .id(1L)
                .name(GROUP_1)
                .state(ApprovalState.APPROVED)
                .categories(getCategories(List.of(2L)))
                .build();

        given(groupRepository.findById(group.getId())).willReturn(Optional.of(group));
        given(groupMemberRepository.save(any())).will(returnsFirstArg());

        //when
        GroupMember joinedMember = groupManager.join(person, group.getId());

        //then
        assertNotNull(joinedMember);
        assertEquals(ApprovalState.PENDING, joinedMember.getState());
        assertEquals(group, joinedMember.getGroup());
    }

    @Test
    void join_joinToApprovedGroupWithInvite_returnGroupMember() {
        //given
        PersonDTO person = getPerson();
        UserGroup group = UserGroup.builder()
                .id(1L)
                .name(GROUP_1)
                .state(ApprovalState.APPROVED)
                .categories(getCategories(List.of(2L)))
                .build();

        GroupMember groupMember = GroupMember.builder()
                .id(1L)
                .group(group)
                .personId(1L)
                .state(ApprovalState.INVITED)
                .build();

        given(groupRepository.findById(group.getId())).willReturn(Optional.of(group));
        given(groupMemberRepository.findByPersonIdAndGroupId(any(), any())).willReturn(Optional.of(groupMember));
        given(groupMemberRepository.save(any())).will(returnsFirstArg());

        //when
        GroupMember joinedMember = groupManager.join(person, group.getId());

        //then
        assertNotNull(joinedMember);
        assertEquals(ApprovalState.APPROVED, joinedMember.getState());
        assertEquals(group, joinedMember.getGroup());
    }

    @Test
    void initJoin_initJoinWithValidData_returnGroupMember() {
        //given
        PersonDTO person = getPerson();
        UserGroup group = UserGroup.builder()
                .id(1L)
                .name(GROUP_1)
                .state(ApprovalState.APPROVED)
                .categories(getCategories(List.of(2L)))
                .build();

        GroupMember groupMember = GroupMember.builder()
                .id(1L)
                .group(group)
                .personId(1L)
                .state(ApprovalState.INVITED)
                .build();

        GroupSearchCriteria criteria = GroupSearchCriteria.builder()
                .states(List.of(ApprovalState.PENDING, ApprovalState.INVITED, ApprovalState.APPROVED))
                .build();

        List<MemberUserGroupModel> groups = getMemberUserGroupModel().stream()
                .filter(groupModel -> criteria.getStates().contains(groupModel.getGroup().getState()))
                .collect(Collectors.toList());

        Page<MemberUserGroupModel> page = new PageImpl<>(groups, PageRequest.of(0, 3), groups.size());
        JsonPersonProfile jsonPersonProfile = mapper.map(person, JsonPersonProfile.class);

        given(groupRepository.findMyGroups(any(), any(), any())).willReturn(page);
        given(groupRepository.findById(group.getId())).willReturn(Optional.of(group));
        given(groupMemberRepository.findByPersonIdAndGroupId(any(), any())).willReturn(Optional.of(groupMember));
        given(groupMemberRepository.save(any())).will(returnsFirstArg());

        //when
        GroupMember joinedMember = groupManager.initJoin(person, group.getId());

        //then
        assertNotNull(joinedMember);
        assertEquals(ApprovalState.APPROVED, joinedMember.getState());
        assertEquals(group, joinedMember.getGroup());
    }

    @Test
    void updateGroupMemberState_byAdminAndSetStateToRejected_stateChangedNotificationsSent() {
        //given
        PersonDTO person = getPerson();

        UserGroup group = UserGroup.builder()
                .id(1L)
                .name(GROUP_1)
                .state(ApprovalState.APPROVED)
                .categories(getCategories(List.of(2L)))
                .build();

        GroupPermission groupPermission = GroupPermission.builder()
                .permission(GroupMemberStatus.ADMIN)
                .build();
        List<GroupPermission> groupPermissions = Collections.singletonList(groupPermission);

        GroupMember groupMember = GroupMember.builder()
                .id(1L)
                .personId(1L)
                .group(group)
                .state(ApprovalState.APPROVED)
                .build();

        given(permissionManager.findPermissionsInternal(any())).willReturn(groupPermissions);
        given(groupMemberRepository.findByPersonIdAndGroupId(any(), any())).willReturn(Optional.ofNullable(groupMember));
        given(groupMemberRepository.countJoinedGroupsByPerson(any())).willReturn(1L);

        //when
        groupManager.updateGroupMemberState(person, group.getId(),
                123L, ApprovalState.REJECTED);

        //then
        verify(groupMemberRepository, times(1)).findByPersonIdAndGroupId(123L, group.getId());
        verify(groupMemberRepository, times(1)).save(ArgumentMatchers.any(GroupMember.class));
    }

    @Test
    void leave_leaveGroupWithValidData_personDeletedFromGroup() {
        //given
        PersonDTO person = getPerson();

        UserGroup group = UserGroup.builder()
                .id(1L)
                .name(GROUP_1)
                .state(ApprovalState.APPROVED)
                .categories(getCategories(List.of(2L)))
                .build();

        GroupMember groupMember = GroupMember.builder()
                .id(1L)
                .personId(1L)
                .group(group)
                .state(ApprovalState.APPROVED)
                .build();

        given(groupMemberRepository.findByPersonIdAndGroupId(any(), any())).willReturn(Optional.of(groupMember));
        given(groupMemberRepository.countJoinedGroupsByPerson(any())).willReturn(1L);

        //when
        groupManager.leave(person.getPersonId(), group.getId());

        //then
        verify(groupMemberRepository, times(1)).delete(groupMember);
    }

    @Test
    void findMembers_findForGivenGroup_returnGroupMemberPage() {
        //given
        PersonDTO person = getPerson();

        List<ApprovalState> approvalStates = Collections.singletonList(ApprovalState.APPROVED);
        Pageable pageable = PageRequest.of(0, 10);

        UserGroup group = UserGroup.builder()
                .id(1L)
                .name(GROUP_1)
                .state(ApprovalState.APPROVED)
                .categories(getCategories(List.of(2L)))
                .build();

        GroupMemberSearchCriteria criteria = GroupMemberSearchCriteria.builder()
                .groupId(1L)
                .states(approvalStates)
                .build();

        List<GroupMember> groupMembers = getGroupMembers().stream()
                .filter(member -> criteria.getStates().contains(member.getGroup().getState())
                        && member.getGroup().getId().equals(criteria.getGroupId()))
                .collect(Collectors.toList());

        Page<GroupMember> page = new PageImpl<>(groupMembers, PageRequest.of(0, 3), groupMembers.size());

        given(groupMemberRepository.findMembers(any(), any())).willReturn(page);

        //when
        Page<JsonGroupMember> groupMemberPage = groupManager.findMembers(AUTHORIZATION_HEADER,
                person.getPersonId(), null, person.isAdmin(), group.getId(), approvalStates, null, pageable);

        //then
        assertEquals(1, groupMemberPage.getContent().size());
        assertEquals(groupMembers.get(0).getId(), groupMemberPage.getContent().get(0).getId());
    }

    @Test
    void findMutualFriends_findForGivenGroupAndSubIds_returnGroupMemberPage() {
        //given
        PersonDTO person = getPerson();

        List<ApprovalState> approvalStates = Collections.singletonList(ApprovalState.APPROVED);
        Pageable pageable = PageRequest.of(0, 10);

        UserGroup group = UserGroup.builder()
                .id(2L)
                .name(GROUP_2)
                .state(ApprovalState.APPROVED)
                .categories(getCategories(List.of(2L)))
                .build();

        List<Long> subscriptionIds = Arrays.asList(2L, 3L);
        List<Long> subscriberIds = Arrays.asList(4L, 5L);

        Set<Long> personIds = new HashSet<>(subscriberIds);
        personIds.addAll(subscriptionIds);

        JsonFollowing subscription1 = new JsonFollowing();
        subscription1.setSubscribedTo(JsonPerson.builder().id(2L).build());
        JsonFollowing subscription2 = new JsonFollowing();
        subscription2.setSubscribedTo(JsonPerson.builder().id(3L).build());
        List<JsonFollowing> subscriptions = List.of(subscription1, subscription2);

        JsonFollowing subscriber1 = new JsonFollowing();
        subscriber1.setSubscriber(JsonPerson.builder().id(4L).build());
        JsonFollowing subscriber2 = new JsonFollowing();
        subscriber2.setSubscriber(JsonPerson.builder().id(5L).build());
        List<JsonFollowing> subscribers = List.of(subscriber1, subscriber2);


        GroupMemberSearchCriteria criteria = GroupMemberSearchCriteria.builder()
                .groupId(2L)
                .personIds(new ArrayList<>(personIds))
                .states(approvalStates)
                .build();

        List<GroupMember> groupMembers = getGroupMembers().stream()
                .filter(member -> criteria.getStates().contains(member.getGroup().getState())
                        && member.getGroup().getId().equals(criteria.getGroupId())
                        && criteria.getPersonIds().contains(member.getId()))
                .collect(Collectors.toList());

        Page<GroupMember> page = new PageImpl<>(groupMembers, PageRequest.of(0, 3), groupMembers.size());

        given(groupMemberRepository.findMembers(any(), any())).willReturn(page);
        given(followingRestService.findSubscriptions(any(), any(), any(), any())).willReturn(new RestPageImpl<>(subscriptions));
        given(followingRestService.findSubscribers(any(), any(), any(), any())).willReturn(new RestPageImpl<>(subscribers));


        //when
        Page<JsonGroupMember> groupMemberPage = groupManager.findMutualFriends(AUTHORIZATION_HEADER,
                person.getPersonId(), person.isAdmin(), group.getId(), null, pageable);

        //then
        assertEquals(1, groupMemberPage.getContent().size());
        assertEquals(groupMembers.get(0).getId(), groupMemberPage.getContent().get(0).getId());
    }

    @Test
    void findPersonsToTag_findByValidData_returnListOfJsonPersonInfo() {
        //given
        JsonPerson person = JsonPerson.builder()
                .id(3L)
                .build();

        UserGroup group = UserGroup.builder()
                .id(2L)
                .name(GROUP_2)
                .state(ApprovalState.APPROVED)
                .categories(getCategories(List.of(2L)))
                .build();

        GroupMemberSearchCriteria criteria = GroupMemberSearchCriteria.builder()
                .groupId(2L)
                .states(Collections.singletonList(ApprovalState.APPROVED))
                .build();

        List<GroupMember> groupMembers = getGroupMembers().stream()
                .filter(member -> criteria.getStates().contains(member.getGroup().getState())
                        && member.getGroup().getId().equals(criteria.getGroupId()))
                .collect(Collectors.toList());

        JsonPersonInfo jsonPersonInfo = new JsonPersonInfo();
        jsonPersonInfo.setPersonId(person.getId());

        Page<GroupMember> page = new PageImpl<>(groupMembers, PageRequest.of(0, 3), groupMembers.size());

        given(groupMemberRepository.findMembers(any(), any())).willReturn(page);
        given(mapper.map(person, JsonPersonInfo.class)).willReturn(jsonPersonInfo);

        //when
        List<PersonDTO> personDTOS = groupManager.findPersonsToTag(3L, group.getId(), null);

        //then
        assertNotNull(personDTOS);
    }

    @Test
    void inviteUser_inviteUserWhoNotAMember_userInvited() {
        //given
        PersonDTO person = getPerson();
        JsonPerson invitedPerson = JsonPerson.builder()
                .id(2L)
                .displayName("John Pedro")
                .build();

        UserGroup group = UserGroup.builder()
                .id(2L)
                .name(GROUP_2)
                .state(ApprovalState.APPROVED)
                .accessType(AccessType.PUBLIC)
                .invitePermission(InvitePermission.MEMBER)
                .categories(getCategories(List.of(2L)))
                .build();

        Optional<GroupMember> groupMember = getGroupMembers().stream().filter(g -> g.getId().equals(1L)).findFirst();

        GroupPermission groupPermission = GroupPermission.builder()
                .permission(GroupMemberStatus.ADMIN)
                .build();
        List<GroupPermission> groupPermissions = Collections.singletonList(groupPermission);

        JsonPersonProfile jsonPersonProfile = mapper.map(person, JsonPersonProfile.class);

        given(groupMemberRepository.findByPersonIdAndGroupId(eq(person.getPersonId()), any())).willReturn(groupMember);
        given(groupManagerUtils.getGroup(group.getId())).willReturn(group);
        given(permissionManager.findPermissionsInternal(any())).willReturn(groupPermissions);
        given(groupMemberRepository.findByPersonIdAndGroupId(eq(invitedPerson.getId()), any())).willReturn(Optional.empty());
        given(groupMemberRepository.save(any())).will(returnsFirstArg());

        //when
        groupManager.inviteUser(person, group.getId(), invitedPerson.getId());

        //then
        verify(groupMemberRepository).save(any());
    }

    @Test
    void inviteUser_byAdminUser_userInvited() {
        //given
        PersonDTO person = getAdminPerson();
        PersonDTO invitedPerson = PersonDTO.builder()
                .personId(2L)
                .displayName("John Pedro")
                .build();

        UserGroup group = UserGroup.builder()
                .id(2L)
                .name(GROUP_2)
                .state(ApprovalState.APPROVED)
                .accessType(AccessType.PUBLIC)
                .invitePermission(InvitePermission.ADMIN)
                .categories(getCategories(List.of(2L)))
                .build();

        Optional<GroupMember> groupMember = getGroupMembers().stream().filter(g -> g.getId().equals(1L)).findFirst();

        GroupPermission groupPermission = GroupPermission.builder()
                .permission(GroupMemberStatus.ADMIN)
                .build();
        List<GroupPermission> groupPermissions = Collections.singletonList(groupPermission);

        given(groupMemberRepository.findByPersonIdAndGroupId(eq(person.getPersonId()), any())).willReturn(groupMember);
        given(groupManagerUtils.getGroup(group.getId())).willReturn(group);
        given(personManager.getPersonByPersonId(invitedPerson.getPersonId())).willReturn(invitedPerson);
        given(groupPermissionRepository.findByPersonIdAndGroupId(any(), any())).willReturn(groupPermissions);
        given(groupMemberRepository.findByPersonIdAndGroupId(eq(invitedPerson.getPersonId()), any())).willReturn(Optional.empty());
        given(groupMemberRepository.save(any())).will(returnsFirstArg());

        //when
        groupManager.inviteUser(person, group.getId(), invitedPerson.getPersonId());

        group.setInvitePermission(InvitePermission.MEMBER);
        given(groupRepository.findById(group.getId())).willReturn(Optional.of(group));

        //when
        groupManager.inviteUser(person, group.getId(), invitedPerson.getPersonId());

        //then
        verify(groupMemberRepository, times(2)).save(any());
    }

    @Test
    void inviteUser_withoutInvitePermission_throwException() {
        //given
        PersonDTO person = getPerson();
        UserGroup group = UserGroup.builder()
                .id(2L)
                .name(GROUP_2)
                .state(ApprovalState.APPROVED)
                .accessType(AccessType.PUBLIC)
                .invitePermission(InvitePermission.ADMIN)
                .categories(getCategories(List.of(2L)))
                .build();

        Optional<GroupMember> groupMember = getGroupMembers().stream().filter(g -> g.getId().equals(1L)).findFirst();

        GroupPermission groupPermission = GroupPermission.builder()
                .permission(GroupMemberStatus.ADMIN)
                .build();
        List<GroupPermission> groupPermissions = Collections.singletonList(groupPermission);

        given(groupMemberRepository.findByPersonIdAndGroupId(eq(person.getPersonId()), any())).willReturn(groupMember);
        given(groupManagerUtils.getGroup(group.getId())).willReturn(group);
        given(groupPermissionRepository.findByPersonIdAndGroupId(any(), any())).willReturn(groupPermissions);
        given(groupMemberRepository.save(any())).will(returnsFirstArg());

        //when
        assertThatThrownBy(() -> groupManager.inviteUser(person, 2L, 2L))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("error.person.unauthorized.to.invite.group");
    }

    @Test
    void getGroupStats_getStatsPerDay_returnGroupStats() {
        //given
        String date = "2021-12-27";
        List<CreatedGroups> createdGroupsPerDay = Collections.singletonList(
                CreatedGroups.builder()
                        .date(date)
                        .createdGroupsCount(1L)
                        .build());
        List<CreateGroupRequests> createGroupRequestsPerDay = Collections.singletonList(
                CreateGroupRequests.builder()
                        .date(date)
                        .createGroupRequestsCount(1L)
                        .build());
        List<JoinGroupRequests> joinGroupRequestsPerDay = Collections.singletonList(
                JoinGroupRequests.builder()
                        .date(date)
                        .joinGroupRequestsCount(1L)
                        .build());

        given(groupRepository.getCreatedGroupsPerDay(any())).willReturn(createdGroupsPerDay);
        given(groupRepository.getCreateGroupRequestsPerDay(any())).willReturn(createGroupRequestsPerDay);
        given(groupMemberRepository.getJoinGroupRequestsPerDay(any())).willReturn(joinGroupRequestsPerDay);

        //when
        GroupStats groupStats = groupManager.getGroupStats(date, TimeInterval.DAY);

        //then
        assertEquals(createdGroupsPerDay, groupStats.getCreatedGroups());
        assertEquals(createGroupRequestsPerDay, groupStats.getCreateGroupRequests());
        assertEquals(joinGroupRequestsPerDay, groupStats.getJoinGroupRequests());
    }

    @Test
    void getGroupMemberIds_getWithValidData_returnSetOfPersonIds() {
        //given
        PersonDTO person = getPerson();
        List<Long> groupIds = getUserGroups().stream().map(UserGroup::getId).collect(Collectors.toList());
        List<GroupMember> groupMembers = getGroupMembers();
        groupMembers.forEach(member -> member.setState(ApprovalState.APPROVED));

        given(groupRepository.findAllById(groupIds)).willReturn(getUserGroups());
        given(groupMemberRepository.findByGroupIn(any())).willReturn(groupMembers);
        given(permissionManager.getAccessibleGroups(any(), any(), any())).willReturn(groupIds);

        //when
        Set<Long> groupMemberIds = groupManager.getGroupMemberIds(person.getPersonId(), person.isAdmin(), new ArrayList<>());

        //then
        assertEquals(3, groupMemberIds.size());
    }

    private PersonDTO getPerson() {
        return PersonDTO.builder()
                .personId(1L)
                .roles(Set.of("USER"))
                .build();
    }

    private PersonDTO getAdminPerson() {
        return PersonDTO.builder()
                .personId(1L)
                .roles(Set.of("ADMIN"))
                .build();
    }

    private List<UserGroupModel> getUserGroupModels() {
        return getUserGroups().stream().map(g -> new UserGroupModel() {
            @Override
            public UserGroup getGroup() {
                return g;
            }

            @Override
            public Long getMembersCount() {
                return null;
            }

            @Override
            public Long getPublishedPostsCount() {
                return null;
            }

            @Override
            public Long getScore() {
                return null;
            }
        }).collect(Collectors.toList());
    }

    private List<MemberUserGroupModel> getMemberUserGroupModel() {
        return getUserGroups().stream().map(g -> new MemberUserGroupModel() {
            @Override
            public UserGroup getGroup() {
                return g;
            }

            @Override
            public Date getMemberSince() {
                return null;
            }

            @Override
            public Long getPublishedPostsCount() {
                return 0L;
            }

            @Override
            public Date getVisitedAt() {
                return null;
            }

            @Override
            public ApprovalState getState() {
                return null;
            }
        }).collect(Collectors.toList());
    }

    private List<UserGroup> getUserGroups() {
        UserGroup group1 = UserGroup.builder()
                .id(1L)
                .name(GROUP_1)
                .state(ApprovalState.APPROVED)
                .categories(getCategories(List.of(2L)))
                .build();
        UserGroup group2 = UserGroup.builder()
                .id(2L)
                .name(GROUP_2)
                .state(ApprovalState.APPROVED)
                .categories(getCategories(List.of(2L, 3L)))
                .build();

        UserGroup group3 = UserGroup.builder()
                .id(3L)
                .name(GROUP_3)
                .state(ApprovalState.APPROVED)
                .categories(getCategories(List.of(2L, 3L)))
                .build();

        UserGroup group4 = UserGroup.builder()
                .id(4L)
                .name(GROUP_4)
                .state(ApprovalState.PENDING)
                .categories(getCategories(List.of(3L)))
                .build();

        UserGroup group5 = UserGroup.builder()
                .id(5L)
                .name(GROUP_5)
                .state(ApprovalState.APPROVED)
                .categories(getCategories(List.of(4L)))
                .build();

        UserGroup group6 = UserGroup.builder()
                .id(6L)
                .name(GROUP_6)
                .state(ApprovalState.APPROVED)
                .categories(getCategories(List.of(5L)))
                .build();

        List<UserGroup> groups = new ArrayList<>();
        groups.add(group1);
        groups.add(group2);
        groups.add(group3);
        groups.add(group4);
        groups.add(group5);
        groups.add(group6);
        return groups;
    }

    private Set<Category> getCategories(List<Long> categoryIds) {
        Set<Category> categories = new HashSet<>();
        Map<String, CategoryLocalized> localizations = new HashMap<>();
        String testCategory = "Test Category ";
        categoryIds.forEach(id -> {
            localizations.put("en", CategoryLocalized.builder()
                    .category(Category.builder().id(id).name(testCategory + id).build())
                    .locale("en")
                    .name("Test Category Localized en" + id)
                    .build());
            localizations.put("ar", CategoryLocalized.builder()
                    .category(Category.builder().id(id).name(testCategory + id).build())
                    .locale("ar")
                    .name("Test Category Localized ar" + id)
                    .build());
        });
        Category parent = Category.builder()
                .id(1L).localizations(localizations)
                .name("Test Category 1")
                .parentCategory(null)
                .categoryUUID(UUID.randomUUID())
                .deleted(false)
                .createdAt(new Date(System.currentTimeMillis()))
                .build();
        categoryIds.forEach(id -> categories.add(Category.builder()
                .id(id).localizations(localizations)
                .name(testCategory + id)
                .parentCategory(parent)
                .categoryUUID(UUID.randomUUID())
                .deleted(false)
                .createdAt(new Date(System.currentTimeMillis()))
                .build()));
        return categories;
    }

    private List<GroupMember> getGroupMembers() {
        GroupMember groupMember1 = GroupMember.builder()
                .id(1L)
                .personId(1L)
                .group(getUserGroups().stream().filter(g -> g.getId().equals(1L)).findFirst().orElse(UserGroup.builder().build()))
                .build();

        GroupMember groupMember2 = GroupMember.builder()
                .id(2L)
                .personId(2L)
                .group(getUserGroups().stream().filter(g -> g.getId().equals(6L)).findFirst().orElse(UserGroup.builder().build()))
                .build();

        GroupMember groupMember3 = GroupMember.builder()
                .id(3L)
                .personId(3L)
                .group(getUserGroups().stream().filter(g -> g.getId().equals(2L)).findFirst().orElse(UserGroup.builder().build()))
                .build();

        GroupMember groupMember4 = GroupMember.builder()
                .id(4L)
                .personId(1L)
                .group(getUserGroups().stream().filter(g -> g.getId().equals(3L)).findFirst().orElse(UserGroup.builder().build()))
                .build();

        GroupMember groupMember5 = GroupMember.builder()
                .id(5L)
                .personId(1L)
                .group(getUserGroups().stream().filter(g -> g.getId().equals(5L)).findFirst().orElse(UserGroup.builder().build()))
                .build();

        GroupMember groupMember6 = GroupMember.builder()
                .id(6L)
                .personId(2L)
                .group(getUserGroups().stream().filter(g -> g.getId().equals(4L)).findFirst().orElse(UserGroup.builder().build()))
                .build();

        List<GroupMember> groupMembers = new ArrayList<>();
        groupMembers.add(groupMember1);
        groupMembers.add(groupMember2);
        groupMembers.add(groupMember3);
        groupMembers.add(groupMember4);
        groupMembers.add(groupMember5);
        groupMembers.add(groupMember6);
        return groupMembers;
    }
}
