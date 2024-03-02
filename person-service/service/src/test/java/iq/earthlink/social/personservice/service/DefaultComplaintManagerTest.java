package iq.earthlink.social.personservice.service;

import iq.earthlink.social.exception.RestApiException;
import iq.earthlink.social.groupservice.group.rest.UserGroupPermissionRestService;
import iq.earthlink.social.personservice.person.ComplaintData;
import iq.earthlink.social.personservice.person.PersonManager;
import iq.earthlink.social.personservice.person.impl.DefaultComplaintManager;
import iq.earthlink.social.personservice.person.impl.repository.ComplaintRepository;
import iq.earthlink.social.personservice.person.model.Person;
import iq.earthlink.social.personservice.person.model.PersonComplaint;
import iq.earthlink.social.personservice.person.rest.JsonComplaintData;
import iq.earthlink.social.postservice.rest.PostRestService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.BDDMockito.given;

class DefaultComplaintManagerTest {

    @InjectMocks
    private DefaultComplaintManager complaintManager;
    @Mock
    private ComplaintRepository complaintRepository;
    @Mock
    private PersonManager personManager;
    @Mock
    private PostRestService postRestService;
    @Mock
    private UserGroupPermissionRestService permissionRestService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    void createComplaint_withValidData_returnComplaint() {
        //given
        Person currentUser = Person.builder()
                .id(1L)
                .build();

        Person violator = Person.builder()
                .id(2L)
                .build();

        ComplaintData data = JsonComplaintData.builder()
                .reasonId(1L)
                .reason("some reason")
                .personId(2L)
                .userGroupId(1L)
                .build();

        PersonComplaint pendingComplaint = PersonComplaint.builder()
                .owner(currentUser)
                .person(violator)
                .userGroupId(data.getUserGroupId())
                .reasonId(data.getReasonId())
                .reason(data.getReason())
                .state(PersonComplaint.PersonComplaintState.PENDING)
                .build();

        given(complaintRepository.findByOwnerIdAndPersonId(currentUser.getId(), violator.getId())).willReturn(Optional.empty());
        given(personManager.getPersonByIdInternal(data.getPersonId())).willReturn(violator);
        given(complaintRepository.save(pendingComplaint)).will(returnsFirstArg());

        //when
        PersonComplaint complaint = complaintManager.createComplaint("authorizationHeader", currentUser, data);

        //then
        assertEquals(pendingComplaint, complaint);
    }

    @Test
    void createComplaint_complaintAlreadyExist_throwRestApiException() {
        //given
        Person currentUser = Person.builder()
                .id(1L)
                .build();

        Person violator = Person.builder()
                .id(2L)
                .build();

        ComplaintData data = JsonComplaintData.builder()
                .reasonId(1L)
                .reason("some reason")
                .personId(2L)
                .userGroupId(1L)
                .build();

        PersonComplaint pendingComplaint = PersonComplaint.builder()
                .owner(currentUser)
                .person(violator)
                .state(PersonComplaint.PersonComplaintState.PENDING)
                .build();

        given(complaintRepository.findByOwnerIdAndPersonId(currentUser.getId(), violator.getId())).willReturn(Optional.of(pendingComplaint));
        given(personManager.getPersonByIdInternal(data.getPersonId())).willReturn(violator);

        //when
        //then
        assertThatThrownBy(() -> complaintManager.createComplaint("authorizationHeader", currentUser, data))
                .isInstanceOf(RestApiException.class)
                .hasMessageContaining("error.person.complaint.already.created");

    }


}