package iq.earthlink.social.personservice.person.impl;

import feign.FeignException;
import iq.earthlink.social.common.audit.AuditMarker;
import iq.earthlink.social.common.audit.AuditMessage;
import iq.earthlink.social.common.audit.EventAction;
import iq.earthlink.social.common.util.DateUtil;
import iq.earthlink.social.exception.BadRequestException;
import iq.earthlink.social.exception.ForbiddenException;
import iq.earthlink.social.exception.NotFoundException;
import iq.earthlink.social.exception.NotUniqueException;
import iq.earthlink.social.groupservice.group.GroupMemberStatus;
import iq.earthlink.social.groupservice.group.rest.UserGroupPermissionRestService;
import iq.earthlink.social.personservice.person.*;
import iq.earthlink.social.personservice.person.impl.repository.BanRepository;
import iq.earthlink.social.personservice.person.impl.repository.ComplaintRepository;
import iq.earthlink.social.personservice.person.impl.repository.GroupBanRepository;
import iq.earthlink.social.personservice.person.model.Person;
import iq.earthlink.social.personservice.person.model.PersonBan;
import iq.earthlink.social.personservice.person.model.PersonComplaint;
import iq.earthlink.social.personservice.person.model.PersonGroupBan;
import iq.earthlink.social.personservice.person.rest.JsonPersonBanRequest;
import iq.earthlink.social.postservice.rest.PostRestService;
import iq.earthlink.social.util.ExceptionUtil;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.dao.DataIntegrityViolationException;
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
@RequiredArgsConstructor
public class DefaultBanManager implements BanManager {

    private static final Logger LOG = LogManager.getLogger(DefaultBanManager.class);

    public static final String CREATE_BAN = "Create ban";
    public static final String CREATE_GROUP_BAN = "Create group ban";
    public static final String FIND_BANS = "Find bans";
    public static final String FIND_GROUP_BANS = "Find group bans";
    public static final String REMOVE_BAN = "Remove ban";
    public static final String REMOVE_GROUP_BAN = "Remove group ban";
    public static final String UPDATE_BAN = "Update ban";
    public static final String UPDATE_GROUP_BAN = "Update group ban";

    private static final int DEFAULT_BAN_PERIOD_IN_DAYS = 1;
    public static final String PERSON_ID = "personId";
    public static final String GROUP_ID = "groupId";
    public static final String ERROR_OPERATION_NOT_PERMITTED = "error.operation.not.permitted";
    public static final String CURRENT_USER = "current user";
    public static final String BANNED_PERSON = "banned person";
    public static final String USER_GROUP_ID = "groupId";
    public static final int MAX_SUPPORTED_YEAR_FOR_POSTGRESQL = 294276;

    private final BanRepository banRepository;
    private final GroupBanRepository groupBanRepository;
    private final ComplaintRepository complaintRepository;
    private final UserGroupPermissionRestService permissionRestService;
    private final PostRestService postRestService;
    private final ComplaintManager complaintManager;
    private final PersonManager personManager;

    @Transactional
    @Nonnull
    @Override
    public PersonBan createBan(@Nonnull String authorizationHeader, @Nonnull Person currentUser, @Nonnull PersonBanData data) {
        checkNotNull(currentUser, ERROR_CHECK_NOT_NULL, CURRENT_USER);
        checkForSystemAdminRole(currentUser, CREATE_BAN);

        checkNotNull(data.getPersonId(), ERROR_CHECK_NOT_NULL, BANNED_PERSON);

        Person bannedPerson = personManager.getPersonByIdInternal(data.getPersonId());

        if (currentUser.getId().equals(bannedPerson.getId()) || bannedPerson.isAdmin()) {
            throw new ForbiddenException(ERROR_OPERATION_NOT_PERMITTED);
        }

        String reason = checkReason(authorizationHeader, data);

        try {
            Optional<PersonBan> existingBan = banRepository.findByAuthorIdAndBannedPersonId(currentUser.getId(), bannedPerson.getId());
            PersonBan ban;
            Date expiredDate = getExpiredDate(data.getDays());

            if (DateUtil.getYear(expiredDate) > MAX_SUPPORTED_YEAR_FOR_POSTGRESQL) {
                throw new BadRequestException("error.ban.expired.days.too.big");
            }

            if (existingBan.isPresent()) {
                ban = existingBan.get();
                // Reset created date:
                ban.setCreatedAt(new Date());
                // Reset expiration date:
                ban.setExpiredAt(expiredDate);
                // Reset reason:
                ban.setReason(data.getReason());
                ban.setReasonId(data.getReasonId());
            } else {
                ban = PersonBan.builder()
                        .author(currentUser)
                        .bannedPerson(bannedPerson)
                        .reason(data.getReason())
                        .reasonId(data.getReasonId())
                        .expiredAt(expiredDate)
                        .build();
            }

            // Resolve all corresponding pending complaints:
            // Find all not resolved generic complaints (without groups) for the person:
            List<PersonComplaint> complaints = complaintRepository.findByPersonIdAndStateAndUserGroupIdIsNull(data.getPersonId(),
                    PersonComplaint.PersonComplaintState.PENDING);
            complaints.forEach(c -> {
                c.setState(USER_BANNED);
                c.setResolverId(currentUser.getId());
                c.setResolvingText(reason);
                c.setResolvingDate(new Date());
            });

            complaintRepository.saveAll(complaints);

            PersonBan saved = banRepository.saveAndFlush(ban);

            String logMessage = String.format("The person \"%s\" was banned", bannedPerson.getUsername());
            LOG.info(AuditMarker.getMarker(), AuditMessage.create(EventAction.BAN, logMessage, currentUser.getId(), bannedPerson.getId()));

            return saved;
        } catch (DataIntegrityViolationException ex) {
            throw new NotUniqueException("error.ban.already.exists");
        }
    }

    @Transactional
    @Nonnull
    @Override
    public List<PersonGroupBan> createGroupBans(
            String authorizationHeader,
            @Nonnull Person currentUser,
            @Nonnull PersonBanData data) {

        checkNotNull(currentUser, ERROR_CHECK_NOT_NULL, CURRENT_USER);
        checkNotNull(data.getGroupIds(), ERROR_CHECK_NOT_NULL, USER_GROUP_ID);
        checkNotNull(data.getPersonId(), ERROR_CHECK_NOT_NULL, BANNED_PERSON);

        Person bannedPerson = personManager.getPersonByIdInternal(data.getPersonId());

        if (currentUser.getId().equals(bannedPerson.getId()) || bannedPerson.isAdmin()) {
            throw new ForbiddenException(ERROR_OPERATION_NOT_PERMITTED);
        }

        String resolvingText = checkReason(authorizationHeader, data);

        List<PersonGroupBan> groupBans = new ArrayList<>();
        Date expiredDate = getExpiredDate(data.getDays());

        Set<Long> groupIds = new HashSet<>(data.getGroupIds());
        for (Long userGroupId : groupIds) {
            checkUserGroup(authorizationHeader, userGroupId);
            checkGroupPermissions(authorizationHeader, userGroupId, currentUser, CREATE_GROUP_BAN,
                    GroupMemberStatus.ADMIN, GroupMemberStatus.MODERATOR);

            var bannedUserHasGroupPermissions = hasGroupPermissions(authorizationHeader, userGroupId,
                    bannedPerson, GroupMemberStatus.ADMIN, GroupMemberStatus.MODERATOR);

            if (currentUser.getId().equals(bannedPerson.getId()) || bannedPerson.isAdmin() || bannedUserHasGroupPermissions) {
                throw new ForbiddenException(ERROR_OPERATION_NOT_PERMITTED);
            }

            Optional<PersonGroupBan> existingGroupBan = groupBanRepository.findByAuthorIdAndBannedPersonIdAndUserGroupId(
                    currentUser.getId(), bannedPerson.getId(), userGroupId);
            PersonGroupBan groupBan;
            if (existingGroupBan.isPresent()) {
                groupBan = existingGroupBan.get();
                // Reset created date:
                groupBan.setCreatedAt(new Date());
                // Reset expiration date:
                groupBan.setExpiredAt(expiredDate);
                // Reset reason:
                groupBan.setReason(data.getReason());
                groupBan.setReasonId(data.getReasonId());
            } else {
                groupBan = PersonGroupBan.builder()
                        .author(currentUser)
                        .bannedPerson(bannedPerson)
                        .reason(data.getReason())
                        .reasonId(data.getReasonId())
                        .expiredAt(expiredDate)
                        .userGroupId(userGroupId)
                        .build();
            }
            groupBans.add(groupBan);
        }

        // Resolve all corresponding pending complaints:
        // Find all not resolved generic complaints (with groups) for the person:
        List<PersonComplaint> complaints = complaintRepository.findByPersonIdAndStateAndUserGroupIdIn(data.getPersonId(),
                PersonComplaint.PersonComplaintState.PENDING, data.getGroupIds());
        complaints.forEach(c -> {
            c.setState(USER_BANNED_GROUP);
            c.setResolverId(currentUser.getId());
            c.setResolvingText(resolvingText);
            c.setResolvingDate(new Date());
        });

        complaintRepository.saveAll(complaints);
        groupBanRepository.saveAll(groupBans);
        return groupBans;
    }

    @Transactional
    @Override
    public Page<PersonBan> findBans(
            @Nonnull BanSearchCriteria criteria,
            @Nonnull Person currentUser,
            @Nonnull Pageable page) {

        checkNotNull(criteria, ERROR_CHECK_NOT_NULL, "criteria");
        checkNotNull(page, ERROR_CHECK_NOT_NULL, "page");

        checkForSystemAdminRole(currentUser, FIND_BANS);
        if (Objects.nonNull(criteria.getQuery())) {
            criteria.setQuery("%" + criteria.getQuery().toLowerCase().trim().replaceAll("\\s+", "%") + "%");
        }
        return banRepository.findBans(criteria, new Date(), page);
    }

    @Transactional
    @Override
    public Page<PersonGroupBan> findGroupBans(
            String authorizationHeader,
            @Nonnull BanSearchCriteria criteria,
            @Nonnull Person currentUser,
            @Nonnull Pageable page) {

        checkNotNull(criteria, ERROR_CHECK_NOT_NULL, "criteria");
        checkNotNull(criteria.getGroupIds(), ERROR_CHECK_NOT_NULL, "groupIds");
        checkNotNull(currentUser, ERROR_CHECK_NOT_NULL, CURRENT_USER);
        checkNotNull(page, ERROR_CHECK_NOT_NULL, "page");
        List<Long> groupIds = new ArrayList<>();

        criteria.getGroupIds().forEach(groupId -> {
            if (hasGroupPermissions(authorizationHeader, groupId, currentUser, GroupMemberStatus.ADMIN, GroupMemberStatus.MODERATOR)) {
                groupIds.add(groupId);
            }
        });

        if (groupIds.isEmpty()) return Page.empty(page);

        criteria.setGroupIds(groupIds);
        if (Objects.nonNull(criteria.getQuery())) {
            criteria.setQuery("%" + criteria.getQuery().toLowerCase().trim().replaceAll("\\s+", "%") + "%");
        }
        return groupBanRepository.findGroupBans(criteria, new Date(), page);
    }

    @Transactional
    @Override
    public void removeBan(String authorizationHeader, @Nonnull Person currentUser, @Nonnull Long banId) {
        checkNotNull(currentUser, ERROR_CHECK_NOT_NULL, "owner");
        checkNotNull(banId, ERROR_CHECK_NOT_NULL, "banId");

        Optional<PersonBan> optionalBan = banRepository.findById(banId);
        if (optionalBan.isPresent()) {
            checkForSystemAdminRole(currentUser, REMOVE_BAN);
            banRepository.deleteById(banId);
        } else {
            throw new NotFoundException("error.ban.not.found", banId);
        }
    }

    @Transactional
    @Override
    public void removeGroupBan(String authorizationHeader, @NonNull Person currentUser, @NonNull Long groupBanId) {
        checkNotNull(currentUser, ERROR_CHECK_NOT_NULL, "owner");
        checkNotNull(groupBanId, ERROR_CHECK_NOT_NULL, "groupBanId");

        final PersonGroupBan groupBan = groupBanRepository.findById(groupBanId)
                .orElseThrow(() -> new NotFoundException("error.group.ban.not.found", groupBanId));

        // Check permissions for group ban:
        checkGroupPermissions(authorizationHeader, groupBan.getUserGroupId(), currentUser, REMOVE_GROUP_BAN,
                GroupMemberStatus.ADMIN, GroupMemberStatus.MODERATOR);

        groupBanRepository.deleteById(groupBanId);
    }

    @Nonnull
    @Override
    public PersonBan updateBan(
            String authorizationHeader,
            @Nonnull Long banId,
            @Nonnull Person currentUser,
            @Nonnull JsonPersonBanRequest request) {

        checkNotNull(banId, ERROR_CHECK_NOT_NULL, "banId");
        checkNotNull(currentUser, ERROR_CHECK_NOT_NULL, "current");
        checkNotNull(request, ERROR_CHECK_NOT_NULL, "request");

        PersonBan ban = banRepository.findById(banId)
                .orElseThrow(() -> new NotFoundException("error.ban.not.found", banId));

        checkForSystemAdminRole(currentUser, UPDATE_BAN);

        ban.setReason(request.getReason());

        if (request.getDays() > 0) {
            ban.setExpiredAt(getExpiredDate(request.getDays()));
        }

        return banRepository.save(ban);
    }

    @Nonnull
    @Override
    public PersonGroupBan updateGroupBan(
            String authorizationHeader,
            @Nonnull Long groupBanId,
            @Nonnull Person currentUser,
            @Nonnull JsonPersonBanRequest request) {

        checkNotNull(groupBanId, ERROR_CHECK_NOT_NULL, "groupBanId");
        checkNotNull(currentUser, ERROR_CHECK_NOT_NULL, CURRENT_USER);
        checkNotNull(request, ERROR_CHECK_NOT_NULL, "request");

        PersonGroupBan groupBan = groupBanRepository.findById(groupBanId)
                .orElseThrow(() -> new NotFoundException("error.group.ban.not.found", groupBanId));

        checkGroupPermissions(authorizationHeader, groupBan.getUserGroupId(), currentUser, UPDATE_GROUP_BAN,
                GroupMemberStatus.ADMIN, GroupMemberStatus.MODERATOR);

        groupBan.setReason(request.getReason());
        if (request.getDays() > 0) {
            groupBan.setExpiredAt(getExpiredDate(request.getDays()));
        }

        return groupBanRepository.save(groupBan);
    }

    @Transactional
    @Override
    @Nonnull
    public List<PersonBan> getActiveBans(@NonNull Long personId) {
        checkNotNull(personId, ERROR_CHECK_NOT_NULL, PERSON_ID);
        return banRepository.findActiveBans(personId, new Date());
    }

    @Nonnull
    @Override
    public List<PersonGroupBan> getActiveGroupBans(Long personId, Long groupId) {
        checkNotNull(personId, ERROR_CHECK_NOT_NULL, PERSON_ID);
        checkNotNull(groupId, ERROR_CHECK_NOT_NULL, USER_GROUP_ID);
        return groupBanRepository.findActiveBans(personId, groupId, new Date());
    }

    @Transactional
    @NonNull
    @Override
    public PersonGroupBan banPersonInGroupByComplaint(String authorizationHeader, Person currentUser, String reason,
                                                      Long complaintId, Integer periodInDays, Boolean resolveAll) {

        PersonComplaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new NotFoundException("error.not.found.complaint", complaintId));

        if (Objects.isNull(complaint.getUserGroupId())) {
            throw new BadRequestException("error.ban.person.in.group.by.complaint.missing.group", complaint.getId());
        }

        if (complaint.getState() != PersonComplaint.PersonComplaintState.PENDING) {
            throw new BadRequestException("error.ban.person.group.by.complaint.wrong.state", complaint.getId(), complaint.getState());
        }

        JsonPersonBanRequest data = JsonPersonBanRequest.builder()
                .personId(complaint.getPerson().getId())
                .reason(complaint.getReason())
                .groupIds(Collections.singletonList(complaint.getUserGroupId()))
                .days(periodInDays)
                .build();

        List<PersonGroupBan> groupBans = createGroupBans(authorizationHeader, currentUser, data);

        // Update person complaints in group:
        complaintManager.moderate(authorizationHeader, currentUser, complaintId, reason, USER_BANNED_GROUP, resolveAll);

        return groupBans.get(0);
    }

    private void checkForSystemAdminRole(Person currentUser, String operation) {
        // Check if user has Admin role:
        if (!currentUser.isAdmin()) {
            throw new ForbiddenException("error.person.is.not.authorized.to", operation);
        }
    }

    private void checkGroupPermissions(String authorizationHeader, Long groupId, Person currentUser,
                                       String operation, GroupMemberStatus... permissions) {

        // Check group permissions:
        if (!hasGroupPermissions(authorizationHeader, groupId, currentUser, permissions)) {
            throw new ForbiddenException("error.person.is.not.authorized.to", operation);
        }
    }

    private boolean hasGroupPermissions(String authorizationHeader, @Nonnull Long groupId, Person person, GroupMemberStatus... permissions) {
        List<GroupMemberStatus> statuses = Arrays.stream(permissions).toList();
        return person.isAdmin() || !permissionRestService.findGroupPermissions(
                authorizationHeader,
                Collections.singletonList(groupId),
                person.getId(),
                statuses).isEmpty();
    }

    private Date getExpiredDate(Integer periodInDays) {
        if (periodInDays != null && periodInDays > 0) {
            return DateUtils.addDays(new Date(), periodInDays);
        } else {
            return DateUtils.addDays(new Date(), DEFAULT_BAN_PERIOD_IN_DAYS);
        }
    }

    private String checkReason(String authorizationHeader, PersonBanData data) {
        String reason = data.getReason();
        if (Objects.isNull(data.getReasonId()) && Objects.isNull(data.getReason())) {
            throw new BadRequestException("error.person.ban.reason.not.provided");
        }
        if (Objects.nonNull(data.getReasonId())) {
            // Check if reason with provided reason ID exists in predefined reasons table:
            try {
                reason = postRestService.getComplaintReason(authorizationHeader, data.getReasonId()).getName();
            } catch (FeignException ex) {
                ExceptionUtil.processFeignException(ex, new NotFoundException("error.reason.not.found", data.getReasonId()),
                        HttpStatus.NOT_FOUND);
            }
        }
        return reason;
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
