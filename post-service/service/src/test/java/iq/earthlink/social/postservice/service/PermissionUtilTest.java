package iq.earthlink.social.postservice.service;

import iq.earthlink.social.exception.RestApiException;
import iq.earthlink.social.groupservice.group.rest.UserGroupPermissionRestService;
import iq.earthlink.social.personservice.person.PersonInfo;
import iq.earthlink.social.personservice.person.rest.JsonPersonProfile;
import iq.earthlink.social.postservice.group.member.GroupMemberManager;
import iq.earthlink.social.postservice.group.member.dto.GroupMemberDTO;
import iq.earthlink.social.postservice.group.member.enumeration.Permission;
import iq.earthlink.social.postservice.person.dto.PersonDTO;
import iq.earthlink.social.postservice.util.PermissionUtil;
import iq.earthlink.social.security.config.ServerAuthProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

class PermissionUtilTest {

    private static final String ERROR_OPERATION_NOT_PERMITTED = "error.operation.not.permitted";

    @InjectMocks
    private PermissionUtil permissionUtil;

    @Mock
    private UserGroupPermissionRestService permissionRestService;
    @Mock
    private ServerAuthProperties authProperties;

    @Mock
    private GroupMemberManager groupMemberManager;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    void getModeratedGroups_emptyPerson_throwException() {
        assertThatThrownBy(() -> permissionUtil.getModeratedGroups(null))
                .isInstanceOf(RestApiException.class)
                .hasMessageContaining("error.check.not.null");
    }

    @Test
    void getModeratedGroups_noGroupPermissions_returnEmptyList() {
        PersonInfo person = JsonPersonProfile.builder()
                .id(1L)
                .build();

        given(permissionRestService.findGroupPermissions(any(), any(), any(),
                any())).willReturn(new ArrayList<>());

        List<Long> moderatedGroups = permissionUtil.getModeratedGroups(person.getId());

        assertTrue(moderatedGroups.isEmpty());
    }

    @Test
    void getModeratedGroups_validInput_returnListOfGroupIds() {
        PersonDTO person = PersonDTO.builder()
                .personId(1L)
                .build();
        Long groupId = 1L;
        var groupMemberDTO = GroupMemberDTO
                .builder()
                .groupId(groupId)
                .permissions(List.of(Permission.ADMIN))
                .build();
        given(groupMemberManager.getUserGroupMembershipsByPermissions(person.getPersonId(), List.of(Permission.ADMIN, Permission.MODERATOR))).willReturn(List.of(groupMemberDTO));
        List<Long> moderatedGroups = permissionUtil.getModeratedGroups(person.getPersonId());

        assertFalse(moderatedGroups.isEmpty());
        assertEquals(moderatedGroups.get(0), groupId);

    }

    @Test
    void checkGroupPermissions_emptyGroup_throwException() {
        PersonDTO person = PersonDTO.builder()
                .personId(1L)
                .roles(Set.of("USER"))
                .build();

        assertThatThrownBy(() -> permissionUtil.checkGroupPermissions(person, null))
                .isInstanceOf(RestApiException.class)
                .hasMessageContaining(ERROR_OPERATION_NOT_PERMITTED);
    }

    @Test
    void checkGroupPermissions_notAdmin_throwException() {
        PersonDTO person = PersonDTO.builder()
                .personId(1L)
                .roles(Set.of("USER"))
                .build();

        assertThatThrownBy(() -> permissionUtil.checkGroupPermissions(person, 1L))
                .isInstanceOf(RestApiException.class)
                .hasMessageContaining(ERROR_OPERATION_NOT_PERMITTED);
    }

    @Test
    void checkGroupPermissions_notMember_throwException() {
        PersonDTO person = PersonDTO.builder()
                .personId(1L)
                .roles(Set.of("USER"))
                .build();
        var groupMember = GroupMemberDTO
                .builder()
                .groupId(1L)
                .personId(1L)
                .permissions(List.of(Permission.USER))
                .build();
        given(groupMemberManager.getGroupMember(any(), any())).willReturn(groupMember);
        assertThatThrownBy(() -> permissionUtil.checkGroupPermissions(person, 1L))
                .isInstanceOf(RestApiException.class)
                .hasMessageContaining(ERROR_OPERATION_NOT_PERMITTED);
    }
}
