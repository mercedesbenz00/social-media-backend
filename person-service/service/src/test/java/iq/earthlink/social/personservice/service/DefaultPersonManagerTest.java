package iq.earthlink.social.personservice.service;

import iq.earthlink.social.classes.enumeration.RegistrationState;
import iq.earthlink.social.classes.enumeration.RequestState;
import iq.earthlink.social.common.filestorage.CompositeFileStorageProvider;
import iq.earthlink.social.common.filestorage.minio.MinioFileStorage;
import iq.earthlink.social.exception.ForbiddenException;
import iq.earthlink.social.exception.RestApiException;
import iq.earthlink.social.groupservice.group.rest.MembersRestService;
import iq.earthlink.social.personservice.data.Gender;
import iq.earthlink.social.personservice.dto.JsonPersonReported;
import iq.earthlink.social.personservice.dto.PersonDTO;
import iq.earthlink.social.personservice.dto.PersonStatus;
import iq.earthlink.social.personservice.outbox.service.EmailOutboxService;
import iq.earthlink.social.personservice.person.PersonComplaintSearchCriteria;
import iq.earthlink.social.personservice.person.PersonSearchCriteria;
import iq.earthlink.social.personservice.person.impl.DefaultPersonManager;
import iq.earthlink.social.personservice.person.impl.repository.ChangeEmailRequestRepository;
import iq.earthlink.social.personservice.person.impl.repository.ComplaintRepository;
import iq.earthlink.social.personservice.person.impl.repository.FollowRepository;
import iq.earthlink.social.personservice.person.impl.repository.PersonRepository;
import iq.earthlink.social.personservice.person.model.*;
import iq.earthlink.social.personservice.person.rest.JsonPerson;
import iq.earthlink.social.personservice.person.rest.JsonPersonProfile;
import iq.earthlink.social.personservice.person.rest.JsonUpdateEmailRequest;
import iq.earthlink.social.personservice.security.model.Role;
import iq.earthlink.social.personservice.util.CommonProperties;
import iq.earthlink.social.personservice.util.ProfileUtil;
import lombok.NonNull;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.dozer.Mapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DefaultPersonManagerTest {

    private static final String EMAIL = "user@gmail.com";
    private static final String TEST_EMAIL = "test@abc.com";
    private static final String NEW_EMAIL = "newEmail@example.com";
    private static final String USERNAME = "abc1234";
    private static final String ADMIN = "ADMIN";
    private static final String AUTHORIZATION_HEADER = "authorizationHeader";
    private static final String DISPLAY_NAME = "test name";
    private static final String ERROR_CHECK_NOT_NULL = "error.check.not.null";


    @InjectMocks
    private DefaultPersonManager personManager;

    @MockBean
    private MinioFileStorage minioFileStorage;
    @MockBean
    private CompositeFileStorageProvider compositeFileStorageProvider;

    @Mock
    private RabbitTemplate rabbitTemplate;
    @Mock
    private PersonRepository personRepository;
    @Mock
    private FollowRepository followRepository;
    @Mock
    private ComplaintRepository complaintRepository;
    @Mock
    private ChangeEmailRequestRepository changeEmailRequestRepository;
    @Mock
    private CommonProperties commonProperties;
    @Mock
    private ChatAdministrationService chatAdministrationService;
    @Mock
    private MembersRestService membersRestService;
    @Mock
    private KafkaProducerService kafkaProducerService;
    @Mock
    private EmailOutboxService emailOutboxService;
    @Mock
    private Mapper mapper;

    @InjectMocks
    @Spy
    private ProfileUtil profileUtil = new ProfileUtil();

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    void whenFindPersonByInvalidAttribute_throwError() {
        // 1. Find person by invalid ID:
        Long personId = Long.MAX_VALUE;
        assertThatThrownBy(() -> personManager.getPersonByIdInternal(personId))
                .isInstanceOf(RestApiException.class)
                .hasMessageContaining("error.person.not.found.byId");
        assertThatThrownBy(() -> personManager.getPersonByIdInternal(null))
                .isInstanceOf(RestApiException.class);

        // 2. Find person by invalid username:
        String username = UUID.randomUUID().toString();
        assertThatThrownBy(() -> personManager.getPersonByUsername(username))
                .isInstanceOf(RestApiException.class)
                .hasMessageContaining("error.person.not.found.by.username");
        assertThatThrownBy(() -> personManager.getPersonByUsername(null))
                .isInstanceOf(RestApiException.class);
        assertThatThrownBy(() -> personManager.getPersonByUsername(StringUtils.EMPTY))
                .isInstanceOf(RestApiException.class)
                .hasMessageContaining("error.person.not.found.by.username");

        // 3. Find person by invalid UUID:
        UUID uuid = UUID.randomUUID();
        assertThatThrownBy(() -> personManager.getPersonByUuid(uuid))
                .isInstanceOf(RestApiException.class)
                .hasMessageContaining("error.person.not.found.by.uuid");
        assertThatThrownBy(() -> personManager.getPersonByUuid(null))
                .isInstanceOf(RestApiException.class);


        // 4. Find person by invalid email:
        assertNull(personManager.findByEmail(null));
    }

    @Test
    void whenFindPersonByValidAttribute_returnPersonInfo() {
        UUID uuid = UUID.randomUUID();

        Person person = Person.builder()
                .id(1L)
                .email(TEST_EMAIL)
                .username(USERNAME)
                .displayName(USERNAME)
                .uuid(uuid)
                .build();

        // 1. Find person by valid ID:
        given(personRepository.findById(person.getId())).willReturn(java.util.Optional.of(person));

        Person foundPersonById = personManager.getPersonByIdInternal(person.getId());

        assertEquals(foundPersonById.getId(), person.getId());
        assertEquals(foundPersonById.getEmail(), person.getEmail());
        assertEquals(foundPersonById.getUsername(), person.getUsername());
        assertEquals(foundPersonById.getDisplayName(), person.getDisplayName());

        // 2. Find person by valid username:
        given(personRepository.findByUsernameIgnoreCase(person.getUsername())).willReturn(java.util.Optional.of(person));

        Person foundPersonByUsername = personManager.getPersonByUsername(person.getUsername());

        assertEquals(foundPersonByUsername.getId(), person.getId());
        assertEquals(foundPersonByUsername.getEmail(), person.getEmail());
        assertEquals(foundPersonByUsername.getUsername(), person.getUsername());
        assertEquals(foundPersonByUsername.getDisplayName(), person.getDisplayName());

        // 3. Find person by valid UUID:
        given(personRepository.findByUuid(person.getUuid())).willReturn(java.util.Optional.of(person));

        Person foundPersonByUuid = personManager.getPersonByUuid(person.getUuid());

        assertEquals(foundPersonByUuid.getId(), person.getId());
        assertEquals(foundPersonByUuid.getEmail(), person.getEmail());
        assertEquals(foundPersonByUuid.getUsername(), person.getUsername());
        assertEquals(foundPersonByUuid.getDisplayName(), person.getDisplayName());

        // 4. Find person by valid email
        given(personRepository.findByEmailIgnoreCase(person.getEmail())).willReturn(person);
        Person foundPersonByEmail = personManager.findByEmail(person.getEmail());

        assertEquals(foundPersonByEmail.getId(), person.getId());
        assertEquals(foundPersonByEmail.getEmail(), person.getEmail());
        assertEquals(foundPersonByEmail.getUsername(), person.getUsername());
        assertEquals(foundPersonByEmail.getDisplayName(), person.getDisplayName());
    }

    @Test
    void whenFindPersonsByCriteria_returnPaginatedResult() {
        // 1. Find with criteria 'followingsFirst' = false
        PersonSearchCriteria criteria1 = PersonSearchCriteria.builder()
                .displayNameQuery("abc")
                .followingsFirst(false)
                .build();

        List<Person> personList = getPeople().stream().filter(p -> p.getDisplayName().contains("abc")).collect(Collectors.toList());
        Page<Person> page1 = new PageImpl<>(personList, PageRequest.of(0, 3), personList.size());

        // given
        given(personRepository.findPersonsOrderedBySimilarity(criteria1, page1.getPageable())).willReturn(page1);

        Page<Person> foundPersons1 = personManager.findPersons(criteria1, page1.getPageable());

        assertTrue(foundPersons1.isFirst());
        assertEquals(2, foundPersons1.getTotalPages());
        assertEquals(personList.size(), foundPersons1.getContent().size());
        assertEquals(personList.size(), foundPersons1.getTotalElements());

        // 2. Find with criteria 'followingsFirst' = true
        PersonSearchCriteria criteria2 = PersonSearchCriteria.builder()
                .currentPersonId(1L)
                .displayNameQuery("abc")
                .followingsFirst(true)
                .build();
        Long[] followerIds = new Long[]{2L, 3L, 4L};
        Long[] subscribedToIds = new Long[]{5L, 6L, 7L};
        personList.remove(0);
        Collections.reverse(personList);
        Page<Person> page2 = new PageImpl<>(personList, PageRequest.of(0, 3), personList.size());

        // given
        given(followRepository.getFollowerIds(criteria2.getCurrentPersonId())).willReturn(List.of(followerIds));
        given(followRepository.getSubscribedToIds(criteria2.getCurrentPersonId())).willReturn(List.of(subscribedToIds));
        given(personRepository.findPersonsWithFollowingsFirst(criteria2, List.of(followerIds), List.of(subscribedToIds),
                page2.getPageable())).willReturn(page2);

        Page<Person> foundPersons2 = personManager.findPersons(criteria2, page2.getPageable());

        assertTrue(foundPersons2.isFirst());
        assertEquals(2, foundPersons2.getTotalPages());
        assertEquals(personList.size(), foundPersons2.getContent().size());
        assertEquals(personList.size(), foundPersons2.getTotalElements());

    }

    @Test
    void whenFindPersonsWithComplaints_returnPaginatedResult() {
        //given
        List<ReportedPerson> reportedPersons = getReportedPersons();
        Page<ReportedPerson> page1 = new PageImpl<>(new ArrayList<>(reportedPersons),
                PageRequest.of(0, 3), reportedPersons.size());
        Pageable pageable = page1.getPageable();

        PersonDTO currentUser = new PersonDTO();
        currentUser.setRoles(Set.of(ADMIN));

        LocalDate toDate = LocalDate.now();
        LocalDate fromDate = toDate.minusDays(30);

        PersonComplaintSearchCriteria criteria = PersonComplaintSearchCriteria.builder()
                .currentUser(currentUser)
                .complaintState(PersonComplaint.PersonComplaintState.PENDING)
                .personStatus(PersonStatus.ACTIVE)
                .fromDate(fromDate)
                .toDate(toDate)
                .build();

        given(complaintRepository.findPersonsWithComplaints(criteria, pageable)).willReturn(page1);
        given(complaintRepository.getPersonComplainStats(any(), any())).willReturn(null);

        //when
        Page<JsonPersonReported> reportedPersonList = personManager.findPersonsWithComplaints(
                AUTHORIZATION_HEADER,
                criteria,
                pageable);

        //then
        assertTrue(reportedPersonList.isFirst());
        assertEquals(3, reportedPersonList.getTotalPages());
        assertEquals(reportedPersons.size(), reportedPersonList.getContent().size());
        assertEquals(reportedPersons.size(), reportedPersonList.getTotalElements());
    }

    @Test
    void whenFindPersonsWithComplaints_withoutAuthorizationHeader_returnBadRequestError() {
        //given
        List<ReportedPerson> reportedPersons = getReportedPersons();
        Page<ReportedPerson> page1 = new PageImpl<>(new ArrayList<>(reportedPersons),
                PageRequest.of(0, 3), reportedPersons.size());
        Pageable pageable = page1.getPageable();

        PersonDTO currentUser = new PersonDTO();
        currentUser.setRoles(Set.of(ADMIN));

        LocalDate toDate = LocalDate.now();
        LocalDate fromDate = toDate.minusDays(30);

        PersonComplaintSearchCriteria criteria = PersonComplaintSearchCriteria.builder()
                .currentUser(currentUser)
                .complaintState(PersonComplaint.PersonComplaintState.PENDING)
                .personStatus(PersonStatus.ACTIVE)
                .fromDate(fromDate)
                .toDate(toDate)
                .build();

        //when
        //then
        //noinspection DataFlowIssue
        assertThatThrownBy(() -> personManager.findPersonsWithComplaints(
                null,
                criteria,
                pageable))
                .isInstanceOf(RestApiException.class)
                .hasMessageContaining(ERROR_CHECK_NOT_NULL);
    }

    @Test
    void whenFindPersonsWithComplaints_withoutCurrentUser_returnBadRequestError() {
        //given
        List<ReportedPerson> reportedPersons = getReportedPersons();
        Page<ReportedPerson> page1 = new PageImpl<>(new ArrayList<>(reportedPersons),
                PageRequest.of(0, 3), reportedPersons.size());
        Pageable pageable = page1.getPageable();

        LocalDate toDate = LocalDate.now();
        LocalDate fromDate = toDate.minusDays(30);

        PersonComplaintSearchCriteria criteria = PersonComplaintSearchCriteria.builder()
                .complaintState(PersonComplaint.PersonComplaintState.PENDING)
                .personStatus(PersonStatus.ACTIVE)
                .fromDate(fromDate)
                .toDate(toDate)
                .build();
        //when
        //then
        //noinspection DataFlowIssue
        assertThatThrownBy(() -> personManager.findPersonsWithComplaints(
                AUTHORIZATION_HEADER,
                criteria,
                pageable))
                .isInstanceOf(RestApiException.class)
                .hasMessageContaining(ERROR_CHECK_NOT_NULL);
    }

    @Test
    void whenFindPersonsWithComplaints_withoutComplaintState_returnBadRequestError() {
        //given
        List<ReportedPerson> reportedPersons = getReportedPersons();
        Page<ReportedPerson> page1 = new PageImpl<>(new ArrayList<>(reportedPersons),
                PageRequest.of(0, 3), reportedPersons.size());
        Pageable pageable = page1.getPageable();

        PersonDTO currentUser = new PersonDTO();
        currentUser.setRoles(Set.of(ADMIN));


        LocalDate toDate = LocalDate.now();
        LocalDate fromDate = toDate.minusDays(30);

        PersonComplaintSearchCriteria criteria = PersonComplaintSearchCriteria.builder()
                .currentUser(currentUser)
                .personStatus(PersonStatus.ACTIVE)
                .fromDate(fromDate)
                .toDate(toDate)
                .build();

        //when
        //then
        //noinspection DataFlowIssue
        assertThatThrownBy(() -> personManager.findPersonsWithComplaints(
                AUTHORIZATION_HEADER,
                criteria,
                pageable))
                .isInstanceOf(RestApiException.class)
                .hasMessageContaining(ERROR_CHECK_NOT_NULL);
    }

    @Test
    void whenFindPersonsWithComplaints_withoutPersonStatus_returnBadRequestError() {
        //given
        List<ReportedPerson> reportedPersons = getReportedPersons();
        Page<ReportedPerson> page1 = new PageImpl<>(new ArrayList<>(reportedPersons),
                PageRequest.of(0, 3), reportedPersons.size());
        Pageable pageable = page1.getPageable();

        PersonDTO currentUser = new PersonDTO();
        currentUser.setRoles(Set.of(ADMIN));


        LocalDate toDate = LocalDate.now();
        LocalDate fromDate = toDate.minusDays(30);

        PersonComplaintSearchCriteria criteria = PersonComplaintSearchCriteria.builder()
                .currentUser(currentUser)
                .complaintState(PersonComplaint.PersonComplaintState.PENDING)
                .fromDate(fromDate)
                .toDate(toDate)
                .build();

        //when
        //then
        //noinspection DataFlowIssue
        assertThatThrownBy(() -> personManager.findPersonsWithComplaints(
                AUTHORIZATION_HEADER,
                criteria,
                pageable))
                .isInstanceOf(RestApiException.class)
                .hasMessageContaining(ERROR_CHECK_NOT_NULL);
    }

    @Test
    void whenFindPersonsWithComplaints_withNonAdminUser_returnForbiddenError() {
        //given
        List<ReportedPerson> reportedPersons = getReportedPersons();
        Page<ReportedPerson> page1 = new PageImpl<>(new ArrayList<>(reportedPersons),
                PageRequest.of(0, 3), reportedPersons.size());
        Pageable pageable = page1.getPageable();

        PersonDTO currentUser = new PersonDTO();
        currentUser.setRoles(Set.of("USER"));


        LocalDate toDate = LocalDate.now();
        LocalDate fromDate = toDate.minusDays(30);

        PersonComplaintSearchCriteria criteria = PersonComplaintSearchCriteria.builder()
                .currentUser(currentUser)
                .complaintState(PersonComplaint.PersonComplaintState.PENDING)
                .personStatus(PersonStatus.ACTIVE)
                .fromDate(fromDate)
                .toDate(toDate)
                .build();

        //when
        //then
        assertThatThrownBy(() -> personManager.findPersonsWithComplaints(
                AUTHORIZATION_HEADER,
                criteria,
                pageable))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("error.operation.not.permitted");
    }

    @Test
    void whenUpdatePerson_validateProfileData() {
        Role userRole = new Role();
        userRole.setId(1L);
        userRole.setCode(ADMIN);
        Person person = Person.builder()
                .id(1L)
                .email(TEST_EMAIL)
                .username(USERNAME)
                .displayName(USERNAME)
                .personRoles(Set.of(userRole))
                .uuid(UUID.randomUUID())
                .build();
        Person updatedPerson = Person.builder()
                .id(1L)
                .email(TEST_EMAIL)
                .username(USERNAME)
                .displayName(DISPLAY_NAME)
                .personRoles(Set.of(userRole))
                .isVerifiedAccount(true)
                .build();

        // 1. Update person display name - set default display name:
        JsonPersonProfile toUpdate = JsonPersonProfile.builder()
                .id(1L)
                .displayName("SN User")
                .build();
        given(commonProperties.getDefaultDisplayName()).willReturn("SN User");
        assertThatThrownBy(() -> personManager.updatePerson(1L, true, toUpdate))
                .isInstanceOf(RestApiException.class)
                .hasMessageContaining("error.display.name.cannot.be");

        // 2. Update person display name:
        toUpdate.setDisplayName(DISPLAY_NAME);
        given(personRepository.findById(person.getId())).willReturn(Optional.of(person));
        given(personRepository.saveAndFlush(updatedPerson)).willReturn(updatedPerson);
        Person updated = personManager.updatePerson(1L, true, toUpdate);

        assertEquals(DISPLAY_NAME, updated.getDisplayName());
        assertTrue(updated.isVerifiedAccount());
    }

    @Test
    void whenChangeEmailRequested_saveChangeEmailRequestRecord() {
        Person person = Person.builder()
                .id(1L)
                .email("oldEmail@example.com")
                .username(USERNAME)
                .uuid(UUID.randomUUID())
                .build();
        ChangeEmailRequest request = ChangeEmailRequest.builder()
                .id(1L)
                .person(person)
                .newEmail(NEW_EMAIL)
                .build();

        Date createdAt = new Date();
        Date expiresAt = DateUtils.addHours(createdAt, 3);
        ChangeEmailRequest updatedRequest = ChangeEmailRequest.builder()
                .id(1L)
                .person(person)
                .newEmail(NEW_EMAIL)
                .oldEmail(person.getEmail())
                .token(any())
                .state(RequestState.ACTIVE)
                .expiresAt(expiresAt)
                .createdAt(createdAt)
                .build();

        given(personRepository.findByEmailIgnoreCase(request.getNewEmail())).willReturn(null);
        given(changeEmailRequestRepository.save(request)).willReturn(updatedRequest);
        given(commonProperties.getWebUrl()).willReturn("http://localhost");
        given(commonProperties.getChangeEmailRequestExpirationInHours()).willReturn(3);

        personManager.changeEmailRequest(person, NEW_EMAIL);

        // Verify the behavior of the dependencies
        verify(changeEmailRequestRepository).save(any());
    }

    @Test
    void whenUpdateEmail_updatePersonData() {
        Person person = Person.builder()
                .id(1L)
                .email("oldEmail@example.com")
                .username(USERNAME)
                .build();

        JsonUpdateEmailRequest data = new JsonUpdateEmailRequest();
        data.setOldEmail(person.getEmail());
        data.setNewEmail(NEW_EMAIL);
        data.setToken("12345");

        JsonUpdateEmailRequest invalidData = new JsonUpdateEmailRequest();
        invalidData.setOldEmail(person.getEmail());
        invalidData.setNewEmail("");
        invalidData.setToken("12345");

        Date expiresAt = DateUtils.addHours(new Date(), 3);
        ChangeEmailRequest request = ChangeEmailRequest.builder()
                .id(1L)
                .person(person)
                .newEmail(NEW_EMAIL)
                .state(RequestState.ACTIVE)
                .expiresAt(expiresAt)
                .build();

        ChangeEmailRequest processed = ChangeEmailRequest.builder()
                .id(1L)
                .person(person)
                .newEmail(NEW_EMAIL)
                .state(RequestState.PROCESSED)
                .expiresAt(expiresAt)
                .build();

        Person updated = Person.builder()
                .id(1L)
                .email(NEW_EMAIL)
                .username(USERNAME)
                .build();

        assertThatThrownBy(() -> personManager.updateEmail(invalidData))
                .isInstanceOf(RestApiException.class)
                .hasMessageContaining("error.email.not.blank");

        given(personRepository.findByEmailIgnoreCase(data.getOldEmail())).willReturn(person);
        given(changeEmailRequestRepository.findByPersonAndNewEmailAndTokenAndState(updated, data.getNewEmail(),
                data.getToken(), RequestState.ACTIVE)).willReturn(null);

        assertThatThrownBy(() -> personManager.updateEmail(data))
                .isInstanceOf(RestApiException.class)
                .hasMessageContaining("error.change.email.request.expired.or.not.exist");

        given(changeEmailRequestRepository.findByPersonAndNewEmailAndTokenAndState(updated, data.getNewEmail(),
                data.getToken(), RequestState.ACTIVE)).willReturn(Collections.singletonList(request));
        given(changeEmailRequestRepository.saveAndFlush(processed)).willReturn(processed);

        personManager.updateEmail(data);

        // Verify the behavior of the dependencies
        verify(personRepository).save(any());
        verify(changeEmailRequestRepository).saveAndFlush(any());
    }

    @NonNull
    private List<Person> getPeople() {
        Person person1 = Person.builder()
                .id(1L)
                .email("test1@abc.com")
                .username("abc1234_1")
                .displayName("abc1234_1")
                .uuid(UUID.randomUUID())
                .build();
        Person person2 = Person.builder()
                .id(2L)
                .email("test2@abc.com")
                .username("abc1234_2")
                .displayName("abc1234_2")
                .uuid(UUID.randomUUID())
                .build();
        Person person3 = Person.builder()
                .id(3L)
                .email("test3@abc.com")
                .username("abc1234_3")
                .displayName("abc1234_3")
                .uuid(UUID.randomUUID())
                .build();
        Person person4 = Person.builder()
                .id(4L)
                .email("test4@abc.com")
                .username("abc1234_4")
                .displayName("abc1234_4")
                .uuid(UUID.randomUUID())
                .build();
        Person person5 = Person.builder()
                .id(5L)
                .email("test5@abc.com")
                .username("abc1234_5")
                .displayName("abc1234_5")
                .uuid(UUID.randomUUID())
                .build();
        Person person6 = Person.builder()
                .id(6L)
                .email("test6@abc.com")
                .username("abc1234_6")
                .displayName("1234_6")
                .uuid(UUID.randomUUID())
                .build();
        Person person7 = Person.builder()
                .id(7L)
                .email("test7@abc.com")
                .username("abc1234_7")
                .displayName("1234_7")
                .uuid(UUID.randomUUID())
                .build();
        Person person8 = Person.builder()
                .id(8L)
                .email("test8@abc.com")
                .username("abc1234_8")
                .displayName("1234_8")
                .uuid(UUID.randomUUID())
                .build();

        List<Person> personList = new ArrayList<>();
        personList.add(person1);
        personList.add(person2);
        personList.add(person3);
        personList.add(person4);
        personList.add(person5);
        personList.add(person6);
        personList.add(person7);
        personList.add(person8);
        return personList;
    }

    private List<ReportedPerson> getReportedPersons() {
        List<ReportedPerson> reportedPersons = new ArrayList<>();

        getPeople().forEach(p -> {
            ReportedPerson c = ReportedPersonImpl.builder()
                    .id(1L)
                    .displayName(p.getDisplayName())
                    .avatar(p.getAvatar())
                    .postCount(p.getPostCount())
                    .createdAt(p.getCreatedAt())
                    .isBanned(false)
                    .build();
            reportedPersons.add(c);
        });

        return reportedPersons;
    }

    @Test
    void findByEmail() {
        String email = EMAIL;
        Person person = Person.builder()
                .id(1L)
                .email(email)
                .username("user")
                .displayName("user")
                .uuid(UUID.randomUUID())
                .build();
        when(personRepository.findByEmailIgnoreCase(email)).thenReturn(person);
        Person foundPerson = personManager.findByEmail(email);
        assertEquals(person, foundPerson);

    }

    @Test
    void findByEmail_notFound() {
        String email = EMAIL;
        when(personRepository.findByEmailIgnoreCase(email)).thenReturn(null);
        Person foundPerson = personManager.findByEmail(email);
        assertNull(foundPerson);
    }

    @Test
    void deactivateProfile() {
        Long personId = 1L;
        Person person = Person.builder()
                .id(personId)
                .email(EMAIL)
                .username("user")
                .displayName("user")
                .uuid(UUID.randomUUID())
                .build();
        when(personRepository.findById(personId)).thenReturn(Optional.of(person));
        when(personRepository.saveAndFlush(person)).thenReturn(person);
        personManager.deactivateProfile(person, null);
        assertFalse(person.isActive());
    }

    @Test
    void removeUser() {
        Long personId = 1L;
        Person person = Person.builder()
                .id(personId)
                .email(EMAIL)
                .username("user")
                .displayName("user")
                .personRoles(Set.of(new Role(1L, "Admin")))
                .uuid(UUID.randomUUID())
                .build();
        when(personRepository.findById(personId)).thenReturn(Optional.of(person));
        when(personRepository.saveAndFlush(person)).thenReturn(person);
        personManager.removeUser(person);
        assertNull(person.getFirstName());
        assertNull(person.getLastName());
        assertNull(person.getEmail());
        assertNull(person.getGender());
        assertEquals(person.getDisplayName(), commonProperties.getDefaultDisplayName());
        assertNull(person.getUsername());

    }

    @Test
    void onboardPerson() {
        Long personId = 1L;
        Role userRole = new Role();
        userRole.setId(1L);
        userRole.setCode(ADMIN);
        Person person = Person.builder()
                .id(personId)
                .email(EMAIL)
                .username("user")
                .displayName("user")
                .uuid(UUID.randomUUID())
                .state(RegistrationState.EMAIL_CONFIRMED)
                .personRoles(Set.of(userRole))
                .build();
        JsonPersonProfile personData = JsonPersonProfile.builder()
                .firstName("test")
                .lastName("test")
                .email(EMAIL)
                .birthDate(new Date(1999, Calendar.SEPTEMBER, 9))
                .gender(Gender.MALE)
                .state(RegistrationState.INFO_PROVIDED)
                .build();

        when(personRepository.findById(personId)).thenReturn(Optional.of(person));
        when(personRepository.saveAndFlush(person)).thenReturn(person);
        personManager.onboardPerson(person.getId(), personData);
        assertTrue(person.isActive());
        assertEquals(RegistrationState.INFO_PROVIDED, person.getState());
    }

    @Test
    void findPersonViews() {
        // Set up the test environment
        String authorizationHeader = "authorization_header";
        PersonSearchCriteria criteria = PersonSearchCriteria
                .builder()
                .personIds(new Long[]{1L, 2L})
                .followingsFirst(false)
                .build();
        List<Long> groupIds = Arrays.asList(1L, 2L);
        Pageable page = PageRequest.of(0, 10);

        // Set up the mock objects
        Set<Long> memberIds = new HashSet<>(List.of(1L, 2L));
        when(membersRestService.getAllMembersInGroups(authorizationHeader, groupIds))
                .thenReturn(memberIds);

        Page<Person> personPage = new PageImpl<>(List.of(Person.builder()
                .id(1L)
                .email(EMAIL)
                .username("user")
                .displayName("user")
                .uuid(UUID.randomUUID())
                .state(RegistrationState.EMAIL_CONFIRMED)
                .bio("bio")
                .build()
        ));
        when(personManager.findPersonsInGroups(authorizationHeader, criteria, new ArrayList<>(memberIds), page))
                .thenReturn(personPage);

        Page<Person> mockPersonPage2 = new PageImpl<>(List.of(
                Person.builder()
                        .id(1L)
                        .email("user@gamil.com")
                        .username("user")
                        .displayName("user")
                        .uuid(UUID.randomUUID())
                        .state(RegistrationState.EMAIL_CONFIRMED)
                        .bio("bio")
                        .build()
        ));
        when(personRepository.findPersons(criteria, page))
                .thenReturn(mockPersonPage2);

        Page<JsonPerson> result = personManager.findPersons(authorizationHeader, criteria, groupIds, page);

        // Assert that the expected result is returned
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
    }

}