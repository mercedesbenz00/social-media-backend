package iq.earthlink.social.personservice.service;

import iq.earthlink.social.exception.ForbiddenException;
import iq.earthlink.social.exception.RestApiException;
import iq.earthlink.social.groupservice.group.GroupMemberStatus;
import iq.earthlink.social.groupservice.group.rest.JsonGroupPermission;
import iq.earthlink.social.groupservice.group.rest.UserGroupDto;
import iq.earthlink.social.groupservice.group.rest.UserGroupPermissionRestService;
import iq.earthlink.social.personservice.person.BanSearchCriteria;
import iq.earthlink.social.personservice.person.ComplaintManager;
import iq.earthlink.social.personservice.person.PersonManager;
import iq.earthlink.social.personservice.person.impl.DefaultBanManager;
import iq.earthlink.social.personservice.person.impl.DefaultComplaintManager;
import iq.earthlink.social.personservice.person.impl.repository.BanRepository;
import iq.earthlink.social.personservice.person.impl.repository.ComplaintRepository;
import iq.earthlink.social.personservice.person.impl.repository.GroupBanRepository;
import iq.earthlink.social.personservice.person.model.Person;
import iq.earthlink.social.personservice.person.model.PersonBan;
import iq.earthlink.social.personservice.person.model.PersonComplaint;
import iq.earthlink.social.personservice.person.model.PersonGroupBan;
import iq.earthlink.social.personservice.person.rest.JsonPersonBanRequest;
import iq.earthlink.social.personservice.security.model.Role;
import org.apache.commons.lang3.time.DateUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.text.SimpleDateFormat;
import java.util.*;

import static iq.earthlink.social.personservice.util.Constants.ADMIN;
import static iq.earthlink.social.personservice.util.Constants.USER;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class DefaultBanManagerTest {

    private static final String DATE_FORMAT = "dd/MM/yyyy";
    private static final String REASON = "some reason";
    private static final String OLD_REASON = "some old reason";
    private static final String GROUP_NAME = "Group name";
    private static final String AUTHORIZATION_HEADER = "authorizationHeader";


    @InjectMocks
    private DefaultBanManager banManager;
    @InjectMocks
    private DefaultComplaintManager defaultComplaintManager;

    @Mock
    private BanRepository banRepository;
    @Mock
    private GroupBanRepository groupBanRepository;
    @Mock
    private ComplaintRepository complaintRepository;
    @Mock
    private PersonManager personManager;
    @Mock
    private UserGroupPermissionRestService permissionRestService;
    @Mock
    private ComplaintManager complaintManager;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    void createBan_createBanIfItNotExistByAdmin_returnPersonBan() {
        //given
        Role adminRole = new Role();
        adminRole.setId(1L);
        adminRole.setCode(ADMIN);

        Role userRole = new Role();
        userRole.setId(2L);
        userRole.setCode(USER);

        Person person = Person.builder()
                .id(1L)
                .personRoles(Set.of(adminRole))
                .build();
        Person bannedPerson = Person.builder()
                .id(2L)
                .personRoles(Set.of(userRole))
                .build();

        String reason = REASON;
        int periodInDays = 14;

        given(banRepository.findByAuthorIdAndBannedPersonId(person.getId(), bannedPerson.getId())).willReturn(Optional.empty());
        given(banRepository.saveAndFlush(any())).will(returnsFirstArg());
        given(personManager.getPersonByIdInternal(bannedPerson.getId())).willReturn(bannedPerson);

        //when
        PersonBan personBan = banManager.createBan(AUTHORIZATION_HEADER, person,
                JsonPersonBanRequest.builder().personId(bannedPerson.getId()).days(periodInDays).reason(reason).build());

        //then
        assertEquals(person, personBan.getAuthor());
        assertEquals(bannedPerson, personBan.getBannedPerson());
        assertEquals(reason, personBan.getReason());
    }

    @Test
    void createBan_createBanIfItExistByAdmin_returnPersonBan() {
        //given
        Role adminRole = new Role();
        adminRole.setId(1L);
        adminRole.setCode(ADMIN);

        Role userRole = new Role();
        userRole.setId(2L);
        userRole.setCode(USER);

        Person person = Person.builder()
                .id(1L)
                .personRoles(Set.of(adminRole))
                .build();
        Person bannedPerson = Person.builder()
                .id(2L)
                .personRoles(Set.of(userRole))
                .build();

        String reason = REASON;
        int periodInDays = 14;
        Date expiredDate = DateUtils.addDays(new Date(), periodInDays);
        Date oldExpiredDate = DateUtils.addDays(new Date(), 5);
        PersonBan personBan = PersonBan.builder()
                .id(1L)
                .author(person)
                .bannedPerson(bannedPerson)
                .reason(OLD_REASON)
                .expiredAt(oldExpiredDate)
                .build();

        SimpleDateFormat formatter = new SimpleDateFormat(DATE_FORMAT);
        String formattedDate = formatter.format(expiredDate);

        PersonComplaint pendingComplaint = PersonComplaint.builder()
                .id(1L)
                .person(bannedPerson)
                .state(PersonComplaint.PersonComplaintState.PENDING)
                .reason(reason)
                .build();

        PersonComplaint updatedComplaint = PersonComplaint.builder()
                .id(1L)
                .person(bannedPerson)
                .state(PersonComplaint.PersonComplaintState.USER_BANNED)
                .reason(reason)
                .resolvingText(reason)
                .build();

        given(complaintRepository.findByPersonIdAndStateAndUserGroupIdIsNull(bannedPerson.getId(), PersonComplaint.PersonComplaintState.PENDING))
                .willReturn(Collections.singletonList(pendingComplaint));
        given(banRepository.findByAuthorIdAndBannedPersonId(person.getId(), bannedPerson.getId())).willReturn(Optional.empty());
        given(banRepository.saveAndFlush(any())).will(returnsFirstArg());

        given(banRepository.findByAuthorIdAndBannedPersonId(person.getId(), bannedPerson.getId())).willReturn(Optional.ofNullable(personBan));
        given(banRepository.saveAndFlush(any())).will(returnsFirstArg());
        given(personManager.getPersonByIdInternal(bannedPerson.getId())).willReturn(bannedPerson);

        //when
        PersonBan actualPersonBan = banManager.createBan(AUTHORIZATION_HEADER, person,
                JsonPersonBanRequest.builder().personId(bannedPerson.getId()).days(periodInDays).reason(reason).build());

        //then
        assertEquals(person, actualPersonBan.getAuthor());
        assertEquals(bannedPerson, actualPersonBan.getBannedPerson());
        assertEquals(reason, actualPersonBan.getReason());
        assertEquals(formattedDate, formatter.format(actualPersonBan.getExpiredAt()));

        given(complaintRepository.findByPersonsAndGroupsAndState(any(), any(), any(), any()))
                .willReturn(new PageImpl<>(Collections.singletonList(updatedComplaint), Pageable.unpaged(), 1));
        //when
        Page<PersonComplaint> updatedComplaints = defaultComplaintManager.findComplaints(AUTHORIZATION_HEADER, person, null,
                Collections.singletonList(bannedPerson.getId()), PersonComplaint.PersonComplaintState.USER_BANNED, Pageable.unpaged());

        //then
        assertEquals(1, updatedComplaints.getTotalElements());
        assertEquals(updatedComplaints.getContent().get(0).getResolvingText(), reason);
    }

    @Test
    void createGroupBan_createBanIfItNotExistByAdmin_returnPersonGroupBan() {
        //given
        Role adminRole = new Role();
        adminRole.setId(1L);
        adminRole.setCode(ADMIN);

        Role userRole = new Role();
        userRole.setId(2L);
        userRole.setCode(USER);

        Person person = Person.builder()
                .id(1L)
                .personRoles(Set.of(adminRole))
                .build();

        Person bannedPerson = Person.builder()
                .id(2L)
                .personRoles(Set.of(userRole))
                .build();

        Long groupId = 1L;
        JsonPersonBanRequest data = JsonPersonBanRequest.builder()
                .personId(bannedPerson.getId())
                .reason(REASON)
                .groupIds(Collections.singletonList(groupId))
                .days(14)
                .build();

        UserGroupDto group = new UserGroupDto();
        group.setId(groupId);
        group.setName(GROUP_NAME);

        given(personManager.getPersonByIdInternal(any())).willReturn(bannedPerson);

        given(groupBanRepository.findByAuthorIdAndBannedPersonIdAndUserGroupId(
                person.getId(), bannedPerson.getId(), groupId)).willReturn(Optional.empty());
        given(permissionRestService.getGroup(any(), any())).willReturn(group);

        given(permissionRestService.findGroupPermissions(
                AUTHORIZATION_HEADER,
                List.of(groupId), bannedPerson.getId(),
                List.of(GroupMemberStatus.ADMIN, GroupMemberStatus.MODERATOR))).willReturn(new ArrayList<>());

        //when
        PersonGroupBan groupBan = banManager.createGroupBans(AUTHORIZATION_HEADER, person, data).get(0);

        //then
        assertEquals(person, groupBan.getAuthor());
        assertEquals(bannedPerson, groupBan.getBannedPerson());
        assertEquals(REASON, groupBan.getReason());
        assertEquals(groupId, groupBan.getUserGroupId());
    }

    @Test
    void createGroupBan_createBanIfItExistByAdmin_returnPersonGroupBan() {
        //given
        Role adminRole = new Role();
        adminRole.setId(1L);
        adminRole.setCode(ADMIN);

        Role userRole = new Role();
        userRole.setId(2L);
        userRole.setCode(USER);

        Person person = Person.builder()
                .id(1L)
                .personRoles(Set.of(adminRole))
                .build();
        Person bannedPerson = Person.builder()
                .id(2L)
                .personRoles(Set.of(userRole))
                .build();

        JsonPersonBanRequest data = JsonPersonBanRequest.builder()
                .personId(bannedPerson.getId())
                .reason(REASON)
                .groupIds(Collections.singletonList(1L))
                .days(14)
                .build();

        int periodInDays = 14;
        Long groupId = 1L;
        Date expiredDate = DateUtils.addDays(new Date(), periodInDays);
        Date oldExpiredDate = DateUtils.addDays(new Date(), 5);

        UserGroupDto group = new UserGroupDto();
        group.setId(groupId);
        group.setName(GROUP_NAME);

        PersonGroupBan personGroupBan = PersonGroupBan.builder()
                .id(1L)
                .author(person)
                .bannedPerson(bannedPerson)
                .reason(OLD_REASON)
                .expiredAt(oldExpiredDate)
                .userGroupId(groupId)
                .build();

        SimpleDateFormat formatter = new SimpleDateFormat(DATE_FORMAT);
        String formattedDate = formatter.format(expiredDate);

        given(personManager.getPersonByIdInternal(any())).willReturn(bannedPerson);

        given(groupBanRepository.findByAuthorIdAndBannedPersonIdAndUserGroupId(
                person.getId(), bannedPerson.getId(), groupId)).willReturn(Optional.ofNullable(personGroupBan));
        given(permissionRestService.getGroup(any(), any())).willReturn(group);

        given(permissionRestService.findGroupPermissions(
                AUTHORIZATION_HEADER,
                List.of(groupId), bannedPerson.getId(),
                List.of(GroupMemberStatus.ADMIN, GroupMemberStatus.MODERATOR))).willReturn(new ArrayList<>());

        //when
        PersonGroupBan actualGroupBan = banManager.createGroupBans(AUTHORIZATION_HEADER, person, data).get(0);

        //then
        assertEquals(person, actualGroupBan.getAuthor());
        assertEquals(bannedPerson, actualGroupBan.getBannedPerson());
        assertEquals(REASON, actualGroupBan.getReason());
        assertEquals(groupId, actualGroupBan.getUserGroupId());
        assertEquals(formattedDate, formatter.format(actualGroupBan.getExpiredAt()));
    }

    @Test
    void createGroupBan_withoutPermission_throwForbiddenException() {
        //given
        Role userRole = new Role();
        userRole.setId(2L);
        userRole.setCode(USER);

        Person person = Person.builder()
                .id(1L)
                .personRoles(Set.of(userRole))
                .build();

        Person bannedPerson = Person.builder()
                .id(2L)
                .personRoles(Set.of(userRole))
                .build();

        String reason = "some reason";
        int periodInDays = 14;
        Long groupId = 1L;

        JsonPersonBanRequest data = JsonPersonBanRequest.builder()
                .personId(bannedPerson.getId())
                .reason(reason)
                .groupIds(Collections.singletonList(groupId))
                .days(periodInDays)
                .build();

        given(personManager.getPersonByIdInternal(any())).willReturn(bannedPerson);
        given(permissionRestService.findGroupPermissions(
                AUTHORIZATION_HEADER,
                List.of(groupId), person.getId(),
                List.of(GroupMemberStatus.ADMIN, GroupMemberStatus.MODERATOR))).willReturn(new ArrayList<>());

        //when
        //then
        assertThatThrownBy(() -> banManager.createGroupBans(AUTHORIZATION_HEADER, person, data))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("error.person.is.not.authorized.to");
    }

    @Test
    void createGroupBan_banGroupAdmin_throwForbiddenException() {
        //given
        Role adminRole = new Role();
        adminRole.setId(1L);
        adminRole.setCode(ADMIN);

        Role userRole = new Role();
        adminRole.setId(2L);
        adminRole.setCode(USER);

        Person person = Person.builder()
                .id(1L)
                .personRoles(Set.of(adminRole))
                .build();

        Person bannedPerson = Person.builder()
                .id(2L)
                .personRoles(Set.of(userRole))
                .build();

        String reason = "some reason";
        int periodInDays = 14;
        Long groupId = 1L;

        JsonPersonBanRequest data = JsonPersonBanRequest.builder()
                .personId(bannedPerson.getId())
                .reason(reason)
                .groupIds(Collections.singletonList(groupId))
                .days(periodInDays)
                .build();

        given(personManager.getPersonByIdInternal(any())).willReturn(bannedPerson);
        given(permissionRestService.findGroupPermissions(
                AUTHORIZATION_HEADER,
                List.of(groupId), person.getId(),
                List.of(GroupMemberStatus.ADMIN, GroupMemberStatus.MODERATOR))).willReturn(
                List.of(JsonGroupPermission
                        .builder()
                        .permission(GroupMemberStatus.ADMIN)
                        .build()));
        given(permissionRestService.findGroupPermissions(
                AUTHORIZATION_HEADER,
                List.of(groupId), bannedPerson.getId(),
                List.of(GroupMemberStatus.ADMIN, GroupMemberStatus.MODERATOR)))
                .willReturn(List.of(JsonGroupPermission
                        .builder()
                        .permission(GroupMemberStatus.ADMIN)
                        .build()));

        //when
        //then
        assertThatThrownBy(() -> banManager.createGroupBans(AUTHORIZATION_HEADER, person, data))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("error.operation.not.permitted");
    }

    @Test
    void createGroupBan_banCurrentUser_throwForbiddenException() {
        //given
        Role role = new Role();
        role.setId(1L);
        role.setCode("ADMIN");

        Person person = Person.builder()
                .id(1L)
                .personRoles(Set.of(role))
                .build();

        String reason = "some reason";
        int periodInDays = 14;
        Long groupId = 1L;

        JsonPersonBanRequest data = JsonPersonBanRequest.builder()
                .personId(person.getId())
                .reason(reason)
                .groupIds(Collections.singletonList(groupId))
                .days(periodInDays)
                .build();

        given(personManager.getPersonByIdInternal(any())).willReturn(person);

        given(permissionRestService.findGroupPermissions(
                AUTHORIZATION_HEADER,
                List.of(groupId), person.getId(),
                List.of(GroupMemberStatus.ADMIN, GroupMemberStatus.MODERATOR))).willReturn(new ArrayList<>());

        //when
        //then
        assertThatThrownBy(() -> banManager.createGroupBans(AUTHORIZATION_HEADER, person, data))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("error.operation.not.permitted");
    }

    @Test
    void findBans_findWithoutPermission_throwForbiddenException() {
        //given
        Role role = new Role();
        role.setId(1L);
        role.setCode(USER);

        Person person = Person.builder()
                .id(1L)
                .personRoles(Set.of(role))
                .build();

        BanSearchCriteria banSearchCriteria = BanSearchCriteria.builder()
                .bannedPersonId(2L)
                .build();

        Pageable pageable = PageRequest.of(0, 3);

        //when
        //then
        assertThatThrownBy(() -> banManager.findBans(banSearchCriteria, person, pageable))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("error.person.is.not.authorized.to");
    }

    @Test
    void findBans_findWithPermission_returnPersonBanPage() {
        //given
        Role role = new Role();
        role.setId(1L);
        role.setCode(ADMIN);

        Person person = Person.builder()
                .id(1L)
                .personRoles(Set.of(role))
                .build();

        Person bannedPerson = Person.builder()
                .id(2L)
                .build();

        PersonBan personBan = PersonBan.builder()
                .id(1L)
                .author(person)
                .bannedPerson(bannedPerson)
                .reason(REASON)
                .expiredAt(new Date())
                .build();

        Page<PersonBan> page = new PageImpl<>(Collections.singletonList(personBan), PageRequest.of(0, 3), 1);

        BanSearchCriteria banSearchCriteria = BanSearchCriteria.builder()
                .bannedPersonId(2L)
                .build();

        Pageable pageable = PageRequest.of(0, 3);

        given(banRepository.findBans(any(), any(), any())).willReturn(page);

        //when
        Page<PersonBan> bans = banManager.findBans(banSearchCriteria, person, pageable);

        //then
        assertEquals(1, bans.getContent().size());
        assertEquals(personBan, bans.getContent().get(0));
    }

    @Test
    void findGroupBans_findWithoutPermission_returnsEmptyPage() {
        //given
        Role role = new Role();
        role.setId(1L);
        role.setCode(USER);

        Person person = Person.builder()
                .id(1L)
                .personRoles(Set.of(role))
                .build();

        BanSearchCriteria banSearchCriteria = BanSearchCriteria.builder()
                .bannedPersonId(2L)
                .groupIds(List.of(1L))
                .build();

        Pageable pageable = PageRequest.of(0, 3);
        given(permissionRestService.findGroupPermissions(
                AUTHORIZATION_HEADER,
                List.of(1L), person.getId(),
                List.of(GroupMemberStatus.ADMIN, GroupMemberStatus.MODERATOR))).willReturn(new ArrayList<>());

        //when
        Page<PersonGroupBan> bans = banManager.findGroupBans(AUTHORIZATION_HEADER, banSearchCriteria, person, pageable);

        //then
        assertEquals(Page.empty(pageable), bans);

    }

    @Test
    void findGroupBans_findWithPermission_returnPersonGroupBanPage() {
        //given
        Role role = new Role();
        role.setId(1L);
        role.setCode(ADMIN);

        Person person = Person.builder()
                .id(1L)
                .personRoles(Set.of(role))
                .build();

        Person bannedPerson = Person.builder()
                .id(2L)
                .build();

        PersonGroupBan personGroupBan = PersonGroupBan.builder()
                .id(1L)
                .author(person)
                .bannedPerson(bannedPerson)
                .reason(REASON)
                .expiredAt(new Date())
                .build();

        Page<PersonGroupBan> page = new PageImpl<>(Collections.singletonList(personGroupBan), PageRequest.of(0, 3), 1);

        BanSearchCriteria banSearchCriteria = BanSearchCriteria.builder()
                .bannedPersonIds(new Long[]{2L})
                .groupIds(List.of(1L))
                .build();

        Pageable pageable = PageRequest.of(0, 3);

        given(groupBanRepository.findGroupBans(any(), any(), any())).willReturn(page);

        //when
        Page<PersonGroupBan> groupBans = banManager.findGroupBans(AUTHORIZATION_HEADER, banSearchCriteria, person, pageable);

        //then
        assertEquals(1, groupBans.getContent().size());
        assertEquals(personGroupBan, groupBans.getContent().get(0));
    }

    @Test
    void findMyGroupBans_emptyGroup_throwException() {
        //given
        Long groupId = null;

        //when
        //then
        assertThatThrownBy(() -> banManager.getActiveGroupBans(2L, groupId))
                .isInstanceOf(RestApiException.class)
                .hasMessageContaining("error.check.not.null");
    }

    @Test
    void findMyGroupBans_groupIsPresent_returnGroupBanList() {
        //given
        Role adminRole = new Role();
        adminRole.setId(1L);
        adminRole.setCode(ADMIN);

        Role userRole = new Role();
        userRole.setId(1L);
        userRole.setCode(USER);

        Person currentPerson = Person.builder()
                .id(2L)
                .personRoles(Set.of(userRole))
                .build();

        Person author = Person.builder()
                .id(1L)
                .personRoles(Set.of(adminRole))
                .build();

        PersonGroupBan personGroupBan = PersonGroupBan.builder()
                .id(1L)
                .author(author)
                .bannedPerson(currentPerson)
                .reason(REASON)
                .userGroupId(1L)
                .expiredAt(new Date())
                .build();

        List<PersonGroupBan> bans = Collections.singletonList(personGroupBan);

        given(groupBanRepository.findActiveBans(any(), any(), any())).willReturn(bans);

        //when
        List<PersonGroupBan> groupBans = banManager.getActiveGroupBans(currentPerson.getId(), 1L);

        //then
        assertEquals(personGroupBan, groupBans.get(0));
    }

    @Test
    void removeBan_removeBanByAdmin_banDeleted() {
        //given
        Role role = new Role();
        role.setId(1L);
        role.setCode(ADMIN);

        Person person = Person.builder()
                .id(1L)
                .personRoles(Set.of(role))
                .build();

        long banId = 1L;

        given(banRepository.findById(banId)).willReturn(Optional.of(new PersonBan()));

        //when
        banManager.removeBan(AUTHORIZATION_HEADER, person, banId);

        //then
        verify(banRepository, times(1)).findById(banId);
        verify(banRepository, times(1)).deleteById(banId);
    }

    @Test
    void removeGroupBan_removeBanByAdmin_groupBanDeleted() {
        //given
        Role role = new Role();
        role.setId(1L);
        role.setCode(ADMIN);

        Person person = Person.builder()
                .id(1L)
                .personRoles(Set.of(role))
                .build();

        long banId = 1L;

        given(groupBanRepository.findById(banId)).willReturn(Optional.of(new PersonGroupBan()));

        //when
        banManager.removeGroupBan(AUTHORIZATION_HEADER, person, banId);

        //then
        verify(groupBanRepository, times(1)).findById(banId);
        verify(groupBanRepository, times(1)).deleteById(banId);
    }

    @Test
    void updateBan_updateBanWithNewReasonAndExpiredDate_returnPersonBan() {
        //given
        Role adminRole = new Role();
        adminRole.setId(1L);
        adminRole.setCode(ADMIN);

        Person person = Person.builder()
                .id(1L)
                .personRoles(Set.of(adminRole))
                .build();
        Person bannedPerson = Person.builder()
                .id(2L)
                .build();

        String reason = REASON;
        int periodInDays = 14;
        Date expiredDate = DateUtils.addDays(new Date(), periodInDays);
        Date oldExpiredDate = DateUtils.addDays(new Date(), 5);
        PersonBan personBan = PersonBan.builder()
                .id(1L)
                .author(person)
                .bannedPerson(bannedPerson)
                .reason(OLD_REASON)
                .expiredAt(oldExpiredDate)
                .build();

        SimpleDateFormat formatter = new SimpleDateFormat(DATE_FORMAT);
        String formattedDate = formatter.format(expiredDate);

        JsonPersonBanRequest request = JsonPersonBanRequest.builder()
                .reason(reason)
                .days(periodInDays)
                .build();

        given(banRepository.findById(personBan.getId())).willReturn(Optional.of(personBan));
        given(banRepository.save(any())).will(returnsFirstArg());

        //when
        PersonBan actualPersonBan = banManager.updateBan(AUTHORIZATION_HEADER, personBan.getId(), person, request);

        //then
        assertEquals(person, actualPersonBan.getAuthor());
        assertEquals(bannedPerson, actualPersonBan.getBannedPerson());
        assertEquals(reason, actualPersonBan.getReason());
        assertEquals(formattedDate, formatter.format(actualPersonBan.getExpiredAt()));
    }

    @Test
    void updateGroupBan_updateGroupBanWithNewReasonAndExpiredDate_returnPersonGroupBan() {
        //given
        Role adminRole = new Role();
        adminRole.setId(1L);
        adminRole.setCode(ADMIN);

        Person person = Person.builder()
                .id(1L)
                .personRoles(Set.of(adminRole))
                .build();
        Person bannedPerson = Person.builder()
                .id(2L)
                .build();

        String reason = "some reason";
        int periodInDays = 14;
        Long groupId = 1L;
        Date expiredDate = DateUtils.addDays(new Date(), periodInDays);
        Date oldExpiredDate = DateUtils.addDays(new Date(), 5);

        UserGroupDto group = new UserGroupDto();
        group.setId(groupId);
        group.setName(GROUP_NAME);

        PersonGroupBan personGroupBan = PersonGroupBan.builder()
                .id(1L)
                .author(person)
                .bannedPerson(bannedPerson)
                .reason(OLD_REASON)
                .expiredAt(oldExpiredDate)
                .userGroupId(groupId)
                .build();

        SimpleDateFormat formatter = new SimpleDateFormat(DATE_FORMAT);
        String formattedDate = formatter.format(expiredDate);

        JsonPersonBanRequest request = JsonPersonBanRequest.builder()
                .reason(reason)
                .days(periodInDays)
                .build();

        given(groupBanRepository.findById(personGroupBan.getId())).willReturn(Optional.of(personGroupBan));
        given(permissionRestService.getGroup(any(), any())).willReturn(group);
        given(groupBanRepository.save(any())).will(returnsFirstArg());

        //when
        PersonGroupBan actualGroupBan = banManager.updateGroupBan(AUTHORIZATION_HEADER, personGroupBan.getId(), person, request);

        //then
        assertEquals(person, actualGroupBan.getAuthor());
        assertEquals(bannedPerson, actualGroupBan.getBannedPerson());
        assertEquals(reason, actualGroupBan.getReason());
        assertEquals(groupId, actualGroupBan.getUserGroupId());
        assertEquals(formattedDate, formatter.format(actualGroupBan.getExpiredAt()));
    }

    @Test
    void banPersonInGroupByComplaint_banWithValidDataByAdmin_returnPersonGroupBan() {
        //given
        Role adminRole = new Role();
        adminRole.setId(1L);
        adminRole.setCode(ADMIN);

        Role userRole = new Role();
        userRole.setId(2L);
        userRole.setCode(USER);

        Person person = Person.builder()
                .id(1L)
                .personRoles(Set.of(adminRole))
                .build();

        Person bannedPerson = Person.builder()
                .id(2L)
                .personRoles(Set.of(userRole))
                .build();

        String reason = "some reason";
        int periodInDays = 14;
        Long complaintId = 123L;

        UserGroupDto group = new UserGroupDto();
        group.setId(321L);
        group.setName(GROUP_NAME);
        Date expiredDate = DateUtils.addDays(new Date(), periodInDays);

        PersonComplaint complaint = PersonComplaint.builder()
                .id(complaintId)
                .person(bannedPerson)
                .state(PersonComplaint.PersonComplaintState.PENDING)
                .reason(reason)
                .userGroupId(group.getId())
                .build();

        SimpleDateFormat formatter = new SimpleDateFormat(DATE_FORMAT);
        String formattedDate = formatter.format(expiredDate);

        given(personManager.getPersonByIdInternal(any())).willReturn(bannedPerson);

        given(complaintRepository.findById(complaintId)).willReturn(Optional.ofNullable(complaint));
        given(groupBanRepository.findByAuthorIdAndBannedPersonIdAndUserGroupId(
                person.getId(), bannedPerson.getId(), group.getId())).willReturn(Optional.empty());
        given(permissionRestService.getGroup(any(), any())).willReturn(group);

        given(permissionRestService.findGroupPermissions(
                AUTHORIZATION_HEADER,
                List.of(group.getId()), bannedPerson.getId(),
                List.of(GroupMemberStatus.ADMIN, GroupMemberStatus.MODERATOR))).willReturn(new ArrayList<>());

        //when
        PersonGroupBan actualGroupBan = banManager.banPersonInGroupByComplaint(AUTHORIZATION_HEADER, person, reason, complaintId, periodInDays, true);

        //then
        assertEquals(person, actualGroupBan.getAuthor());
        assertEquals(bannedPerson, actualGroupBan.getBannedPerson());
        assertEquals(reason, actualGroupBan.getReason());
        assertEquals(group.getId(), actualGroupBan.getUserGroupId());
        assertEquals(formattedDate, formatter.format(actualGroupBan.getExpiredAt()));
    }
}