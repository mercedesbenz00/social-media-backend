package iq.earthlink.social.groupservice.group;

import iq.earthlink.social.classes.data.dto.PersonData;
import iq.earthlink.social.exception.BadRequestException;
import iq.earthlink.social.exception.ForbiddenException;
import iq.earthlink.social.groupservice.event.KafkaProducerService;
import iq.earthlink.social.groupservice.group.member.GroupMember;
import iq.earthlink.social.groupservice.group.member.GroupMemberRepository;
import iq.earthlink.social.groupservice.group.permission.DefaultGroupPermissionManager;
import iq.earthlink.social.groupservice.group.permission.GroupPermission;
import iq.earthlink.social.groupservice.group.permission.PermissionSearchCriteria;
import iq.earthlink.social.groupservice.group.permission.repository.GroupPermissionRepository;
import iq.earthlink.social.groupservice.group.rest.JsonGroupPermission;
import iq.earthlink.social.groupservice.group.rest.JsonGroupPermissionData;
import iq.earthlink.social.groupservice.group.rest.enumeration.AccessType;
import iq.earthlink.social.groupservice.person.PersonManager;
import iq.earthlink.social.groupservice.person.dto.PersonDTO;
import iq.earthlink.social.postservice.post.rest.GroupStatsDTO;
import iq.earthlink.social.postservice.rest.PostRestService;
import org.dozer.DozerBeanMapperBuilder;
import org.dozer.Mapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

class DefaultGroupPermissionManagerTest {

    @InjectMocks
    private DefaultGroupPermissionManager groupPermissionManager;
    @Mock
    private GroupPermissionRepository repository;
    @Mock
    private GroupRepository groupRepository;
    @Mock
    private GroupMemberRepository groupMemberRepository;
    @Mock
    private KafkaProducerService kafkaProducerService;
    @Mock
    private GroupManagerUtils groupManagerUtils;
    @Mock
    private PostRestService postRestService;
    @Mock
    private PersonManager personManager;
    @Spy
    private Mapper mapper = DozerBeanMapperBuilder.create().build();

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    void setPermission_setPermissionFromNotAdmin_throwForbiddenException() {
        //given
        Long personId = 1L;
        Boolean isAdmin = false;
        UserGroup userGroup = UserGroup.builder()
                .id(1L)
                .accessType(AccessType.PUBLIC)
                .build();

        JsonGroupPermissionData groupPermissionData = new JsonGroupPermissionData();
        groupPermissionData.setPersonId(2L);
        groupPermissionData.setPermission(GroupMemberStatus.MODERATOR);

        //when
        //then
        assertThatThrownBy(() -> groupPermissionManager.setPermission(personId, isAdmin, userGroup, groupPermissionData))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("error.operation.not.permitted");

    }

    @Test
    void setPermission_setPermissionFromAdminForNotAMember_throwBadRequestException() {
        //given
        Long personId = 1L;
        Boolean isAdmin = true;
        UserGroup userGroup = UserGroup.builder()
                .id(1L)
                .accessType(AccessType.PUBLIC)
                .build();

        JsonGroupPermissionData groupPermissionData = new JsonGroupPermissionData();
        groupPermissionData.setPersonId(2L);
        groupPermissionData.setPermission(GroupMemberStatus.MODERATOR);

        given(groupMemberRepository.findActiveMember(any(), any())).willReturn(null);

        //when
        //then
        assertThatThrownBy(() -> groupPermissionManager.setPermission(personId, isAdmin, userGroup, groupPermissionData))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("error.not.member.of.group");
    }

    @Test
    void setPermission_setPermissionFromAdmin_permissionSet() {
        //given
        Long adminId = 1L;
        Boolean isAdmin = true;
        Long personId = 2L;
        UserGroup userGroup = UserGroup.builder()
                .id(1L)
                .accessType(AccessType.PUBLIC)
                .build();

        PersonDTO person = PersonDTO.builder()
                .personId(personId)
                .displayName("Name")
                .build();

        JsonGroupPermissionData groupPermissionData = new JsonGroupPermissionData();
        groupPermissionData.setPersonId(2L);
        groupPermissionData.setPermission(GroupMemberStatus.MODERATOR);

        given(groupMemberRepository.findActiveMember(any(), any())).willReturn(new GroupMember());
        given(personManager.getPersonByPersonId(any())).willReturn(person);
        given(repository.save(any())).will(returnsFirstArg());

        //when
        JsonGroupPermission groupPermission = groupPermissionManager.setPermission(adminId, isAdmin, userGroup, groupPermissionData);

        //then
        assertEquals(GroupMemberStatus.MODERATOR, groupPermission.getPermission());
        assertEquals(userGroup.getId(), groupPermission.getUserGroup().getId());
        assertEquals(adminId, groupPermission.getAuthorId());
        assertEquals(personId, groupPermission.getPerson().getId());
        assertEquals(2L, groupPermission.getPersonId().longValue());
    }

    @Test
    void removePermission_removePersonModeratorPermission_permissionDeleted() {
        //given
        Long personId = 1L;
        Long permissionId = 2L;

        GroupPermission groupPermission = GroupPermission.builder()
                .id(3L)
                .permission(GroupMemberStatus.MODERATOR)
                .personId(2L)
                .userGroup(UserGroup.builder().id(1L).build())
                .build();
        doNothing().when(kafkaProducerService).sendMessage(any(), any());
        given(repository.findById(permissionId)).willReturn(Optional.ofNullable(groupPermission));

        //when
        groupPermissionManager.removePermission(personId, true, permissionId);

        //then
        assert groupPermission != null;
        verify(repository, times(1)).delete(groupPermission);
    }

    @Test
    void findPermissions_findPermissionsByAdmin_returnPageableGroupPermission() {
        //given
        PermissionSearchCriteria criteria = PermissionSearchCriteria.builder()
                .groupIds(Collections.singletonList(2L))
                .build();
        Long personId = 1L;
        Pageable pageable = PageRequest.of(0, 3);
        UserGroup userGroup = UserGroup.builder()
                .stats(new UserGroupStats())
                .build();
        GroupPermission groupPermission = GroupPermission.builder()
                .id(3L)
                .permission(GroupMemberStatus.MODERATOR)
                .personId(2L)
                .userGroup(userGroup)
                .build();

        Page<GroupPermission> expectedComments = new PageImpl<>(List.of(groupPermission), pageable, 1);

        given(repository.findByPersonIdAndGroupIds(personId, criteria.getGroupIds())).willReturn(Collections.singletonList(groupPermission));
        given(repository.findPermissionsOrderedBySimilarity(criteria, pageable)).willReturn(expectedComments);
        given(postRestService.getGroupStats(any(), any())).willReturn(new GroupStatsDTO(3L));
        given(personManager.getPersonByPersonId(any())).willReturn(new PersonDTO());

        //when
        Page<JsonGroupPermission> permissions = groupPermissionManager.findPermissions(criteria, personId, true,
                "authorizationHeader", pageable);

        //then
        assertEquals(1, permissions.getContent().size());
        assertEquals(3L, permissions.getContent().get(0).getId().longValue());
    }

    @Test
    void getAccessibleGroups_getGroupsWithValidData_returnListsOfGroupIds() {
        //given
        Long personId = 1L;
        Boolean isAdmin = true;
        UserGroup publicUserGroup = UserGroup.builder()
                .id(1L)
                .accessType(AccessType.PUBLIC)
                .build();
        UserGroup privateUserGroup = UserGroup.builder()
                .id(2L)
                .accessType(AccessType.PRIVATE)
                .build();

        given(groupRepository.findByAccessType(AccessType.PUBLIC)).willReturn(List.of(publicUserGroup));
        given(groupRepository.findByAccessType(AccessType.PRIVATE)).willReturn(List.of(privateUserGroup));

        //when
        List<Long> accessibleGroups = groupPermissionManager.getAccessibleGroups(personId, isAdmin, new ArrayList<>());

        //then
        assertEquals(List.of(1L, 2L), accessibleGroups);
    }
}