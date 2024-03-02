package iq.earthlink.social.personservice.person.impl;

import feign.FeignException;
import iq.earthlink.social.exception.BadRequestException;
import iq.earthlink.social.exception.ForbiddenException;
import iq.earthlink.social.exception.NotFoundException;
import iq.earthlink.social.exception.NotUniqueException;
import iq.earthlink.social.groupservice.group.rest.UserGroupPermissionRestService;
import iq.earthlink.social.personservice.person.ComplaintData;
import iq.earthlink.social.personservice.person.ComplaintManager;
import iq.earthlink.social.personservice.person.PersonManager;
import iq.earthlink.social.personservice.person.impl.repository.ComplaintRepository;
import iq.earthlink.social.personservice.person.model.Person;
import iq.earthlink.social.personservice.person.model.PersonComplaint;
import iq.earthlink.social.personservice.person.model.PersonComplaint.PersonComplaintState;
import iq.earthlink.social.postservice.rest.PostRestService;
import iq.earthlink.social.util.ExceptionUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Nonnull;
import java.util.*;

import static iq.earthlink.social.common.util.CommonConstants.ERROR_CHECK_NOT_NULL;
import static iq.earthlink.social.common.util.Preconditions.checkNotNull;
import static iq.earthlink.social.personservice.person.model.PersonComplaint.PersonComplaintState.USER_BANNED;
import static iq.earthlink.social.personservice.person.model.PersonComplaint.PersonComplaintState.USER_BANNED_GROUP;

@Service
public class DefaultComplaintManager implements ComplaintManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultComplaintManager.class);

    private final PersonManager personManager;
    private final ComplaintRepository repository;
    private final UserGroupPermissionRestService permissionRestService;
    private final PostRestService postRestService;

    public DefaultComplaintManager(PersonManager personManager, ComplaintRepository repository,
                                   UserGroupPermissionRestService permissionRestService, PostRestService postRestService) {
        this.personManager = personManager;
        this.repository = repository;
        this.permissionRestService = permissionRestService;
        this.postRestService = postRestService;
    }

    @Transactional
    @Nonnull
    @Override
    public PersonComplaint createComplaint(
            String authorizationHeader, @Nonnull Person currentUser,
            @Nonnull ComplaintData data) {
        checkNotNull(currentUser, ERROR_CHECK_NOT_NULL, "owner");
        checkReason(authorizationHeader, data);
        checkUserGroup(authorizationHeader, data.getUserGroupId());

        Person violator = personManager.getPersonByIdInternal(data.getPersonId());

        LOGGER.debug("Creating complaint with owner: {} for the person: {}. Reason: {}",
                currentUser.getId(), violator.getId(), Objects.nonNull(data.getReasonId()) ? data.getReasonId() : data.getReason());

        Optional<PersonComplaint> complaintOpt = repository.findByOwnerIdAndPersonId(currentUser.getId(), violator.getId());
        if (complaintOpt.isPresent() && complaintOpt.get().getState().equals(PersonComplaintState.PENDING)) {
            throw new NotUniqueException("error.person.complaint.already.created");
        }

        PersonComplaint complaint = PersonComplaint.builder()
                .owner(currentUser)
                .person(violator)
                .userGroupId(data.getUserGroupId())
                .reasonId(data.getReasonId())
                .reason(data.getReason())
                .state(PersonComplaintState.PENDING)
                .build();

        return repository.save(complaint);
    }

    @Transactional(readOnly = true)
    @Nonnull
    @Override
    public Page<PersonComplaint> findComplaints(String authorizationHeader, Person currentUser, List<Long> groupIds,
                                                List<Long> personIds, PersonComplaintState state, @Nonnull Pageable page) {
        checkNotNull(currentUser, ERROR_CHECK_NOT_NULL, "currentUser");
        if (!currentUser.isAdmin()) {
            throw new ForbiddenException("error.operation.not.permitted");
        }
        return repository.findByPersonsAndGroupsAndState(personIds, groupIds, state, page);

    }

    @Transactional
    @Override
    public void removeComplaint(@Nonnull Person currentUser, @Nonnull Long complaintId) {
        checkNotNull(currentUser, ERROR_CHECK_NOT_NULL, "owner");
        checkNotNull(complaintId, ERROR_CHECK_NOT_NULL, "complaintId");

        PersonComplaint complaint = repository.findById(complaintId)
                .orElseThrow(() -> new NotFoundException("error.complaint.not.found", complaintId));

        if (currentUser.isAdmin() || Objects.equals(currentUser.getId(), complaint.getOwner().getId())) {
            repository.delete(complaint);
        } else {
            throw new ForbiddenException("error.operation.not.permitted");
        }
    }

    @Transactional
    @Override
    public void moderate(String authorizationHeader, Person currentUser, Long complaintId, String reason,
                         PersonComplaintState state, boolean resolveAll) {

        PersonComplaint complaint = repository.findById(complaintId)
                .orElseThrow(() -> new NotFoundException("error.complaint.not.found", complaintId));

        Long userGroupId = complaint.getUserGroupId();

        if (!currentUser.isAdmin()) {
            throw new ForbiddenException("error.person.is.not.authorized");
        }

        Set<PersonComplaint> complaints = new HashSet<>();
        complaints.add(complaint);

        if (resolveAll) {
            switch (state) {
                case USER_BANNED:
                    if (Objects.nonNull(userGroupId)) {
                        throw new BadRequestException("error.moderate.complaint.invalid.state", complaint.getId(), state, USER_BANNED.name());
                    }
                    // Find all not resolved generic complaints (without groups) for the person:
                    complaints.addAll(repository.findByPersonIdAndStateAndUserGroupIdIsNull(complaint.getPerson().getId(),
                            PersonComplaintState.PENDING));
                    break;
                case USER_BANNED_GROUP:
                    if (Objects.isNull(userGroupId)) {
                        throw new BadRequestException("error.moderate.complaint.invalid.state", complaint.getId(), state,
                                USER_BANNED_GROUP.name());
                    }
                    // Find all not resolved complaints for the person in moderated group:
                    complaints.addAll(repository.findByPersonIdAndStateAndUserGroupId(complaint.getPerson().getId(),
                            PersonComplaintState.PENDING, userGroupId));
                    break;
                case REJECTED:
                    // Find all not resolved complaints for the person:
                    complaints.addAll(repository.findByPersonIdAndState(complaint.getPerson().getId(),
                            PersonComplaintState.PENDING));
                    break;
                default:
                    LOGGER.error("Unhandled complaint state: {}", state);
            }
        }

        complaints.forEach(c -> {
            c.setState(state);
            c.setResolverId(currentUser.getId());
            c.setResolvingText(reason);
            c.setResolvingDate(new Date());
        });

        repository.saveAll(complaints);
    }

    private void checkReason(String authorizationHeader, ComplaintData data) {
        if (Objects.isNull(data.getReasonId()) && Objects.isNull(data.getReason())) {
            throw new BadRequestException("error.person.complaint.reason.not.provided");
        }
        if (Objects.nonNull(data.getReasonId())) {
            // Check if reason with provided reason ID exists in predefined reasons table:
            try {
                postRestService.getComplaintReason(authorizationHeader, data.getReasonId());
            } catch (FeignException ex) {
                ExceptionUtil.processFeignException(ex, new NotFoundException("error.reason.not.found", data.getReasonId()),
                        HttpStatus.NOT_FOUND);
            }
        }
    }

    private void checkUserGroup(String authorizationHeader, Long userGroupId) {
        if (Objects.isNull(userGroupId)) {
            return;
        }
        try {
            // Ensure that user group exist in database:
            permissionRestService.getGroup(authorizationHeader, userGroupId);
        } catch (FeignException ex) {
            ExceptionUtil.processFeignException(ex, new NotFoundException("error.group.with.id.not.found", userGroupId),
                    HttpStatus.NOT_FOUND);
        }
    }
}
