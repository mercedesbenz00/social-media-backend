package iq.earthlink.social.postservice.post.complaint;

import iq.earthlink.social.classes.data.dto.AuthorDetails;
import iq.earthlink.social.classes.data.dto.AvatarDetails;
import iq.earthlink.social.classes.data.dto.ComplaintRequest;
import iq.earthlink.social.classes.data.dto.JsonMediaFile;
import iq.earthlink.social.classes.enumeration.PostState;
import iq.earthlink.social.exception.BadRequestException;
import iq.earthlink.social.exception.ForbiddenException;
import iq.earthlink.social.exception.NotFoundException;
import iq.earthlink.social.exception.NotUniqueException;
import iq.earthlink.social.postservice.person.PersonManager;
import iq.earthlink.social.postservice.person.dto.PersonDTO;
import iq.earthlink.social.postservice.post.PostComplaintData;
import iq.earthlink.social.postservice.post.PostComplaintState;
import iq.earthlink.social.postservice.post.PostManager;
import iq.earthlink.social.postservice.post.complaint.model.PostComplaint;
import iq.earthlink.social.postservice.post.complaint.model.Reason;
import iq.earthlink.social.postservice.post.complaint.repository.PostComplaintRepository;
import iq.earthlink.social.postservice.post.model.Post;
import iq.earthlink.social.postservice.post.rest.JsonPostComplaint;
import iq.earthlink.social.postservice.post.rest.JsonPostComplaintData;
import iq.earthlink.social.postservice.util.PermissionUtil;
import org.dozer.Mapper;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import java.util.*;

@Service
public class DefaultPostComplaintManager implements PostComplaintManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultPostComplaintManager.class);

    private final PostComplaintRepository repository;
    private final ReasonManager reasonManager;
    private final PermissionUtil permissionUtil;
    private final PostManager postManager;
    private final PersonManager personManager;
    private final Mapper mapper;

    public DefaultPostComplaintManager(PostComplaintRepository repository, ReasonManager reasonManager,
                                       PermissionUtil permissionUtil, PostManager postManager, PersonManager personManager, Mapper mapper) {
        this.repository = repository;
        this.reasonManager = reasonManager;
        this.permissionUtil = permissionUtil;
        this.postManager = postManager;
        this.personManager = personManager;
        this.mapper = mapper;
    }

    @Override
    public JsonPostComplaint createComplaint(Long personId, Post post, PostComplaintData data) {
        PostComplaint complaint = createComplaintInternal(personId, post, data);

        return enrichWithAuthorDetails(Collections.singletonList(complaint)).get(0);
    }

    @Override
    public JsonPostComplaint createComplaint(Long personId, Post post, ComplaintRequest request) {
        JsonPostComplaintData data = mapper.map(request, JsonPostComplaintData.class);
        if (request.getReasonId() == null) {
            data.setReason(null);
        }
        PostComplaint complaint = createComplaintInternal(personId, post, data);

        return enrichWithAuthorDetails(Collections.singletonList(complaint)).get(0);
    }

    @Override
    public JsonPostComplaint getJsonPostComplaint(Long personId, Post post, Long complaintId) {
        PostComplaint complaint = getPostComplaintInternal(personId, post, complaintId);

        return enrichWithAuthorDetails(Collections.singletonList(complaint)).get(0);
    }

    @Override
    public Page<JsonPostComplaint> findComplaints(PersonDTO person, Post post, Pageable page) {
        if (!permissionUtil.hasGroupPermissions(person, post.getUserGroupId())) {
            throw new ForbiddenException("error.operation.not.permitted");
        }
        Page<PostComplaint> complaints = repository.findComplaints(post.getId(), page);
        List<JsonPostComplaint> jsonPostComplaints = enrichWithAuthorDetails(complaints.getContent());

        return new PageImpl<>(jsonPostComplaints, page, complaints.getTotalElements());
    }

    @Override
    public JsonPostComplaint updateComplaint(Long personId, Post post, Long complaintId, PostComplaintData data) {
        PostComplaint complaint = updateComplaintInternal(personId, post, complaintId, data);

        return enrichWithAuthorDetails(Collections.singletonList(complaint)).get(0);
    }



    @Override
    public JsonPostComplaint updateComplaint(Long personId, Post post, Long complaintId, ComplaintRequest request) {
        JsonPostComplaintData data = mapper.map(request, JsonPostComplaintData.class);
        if (request.getReasonId() == null) {
            data.setReason(null);
        }
        PostComplaint complaint = updateComplaintInternal(personId, post, complaintId, data);

        return enrichWithAuthorDetails(Collections.singletonList(complaint)).get(0);
    }

    @Override
    public void removeComplaint(Long personId, Post post, Long complaintId) {
        repository.delete(getPostComplaintInternal(personId, post, complaintId));
    }

    @Override
    public void rejectAllComplaints(PersonDTO person, String reason, Long postId) {
        Post post = postManager.getPost(postId);

        if (!permissionUtil.hasGroupPermissions(person, post.getUserGroupId())) {
            throw new ForbiddenException("error.operation.not.permitted");
        }

        LOGGER.debug("Rejecting all complaints for the post with id: {}, rejecting by: {}",
                postId, person.getPersonId());

        Page<PostComplaint> postComplaints = repository.findByPostIdAndState(postId, PostComplaintState.PENDING, Pageable.unpaged());
        postComplaints.forEach(postComplaint -> {
            postComplaint.setResolverId(person.getPersonId());
            postComplaint.setResolvingText(reason);
            postComplaint.setResolvingDate(new Date());
            postComplaint.setState(PostComplaintState.REJECTED);
            repository.save(postComplaint);
        });
    }

    private void checkReason(Long postId, PostComplaintData data) {
        if (ObjectUtils.isEmpty(data.getReason()) && ObjectUtils.isEmpty(data.getReasonOther())) {
            throw new BadRequestException("error.post.complaint.reason.not.provided", postId);
        }
    }
    private List<JsonPostComplaint> enrichWithAuthorDetails(List<PostComplaint> postComplaints) {
        var jsonPostComplaints = postComplaints.stream()
                .map(postComplaint -> {
                    JsonPostComplaint jsonPostComplaint = mapper.map(postComplaint, JsonPostComplaint.class);
                    jsonPostComplaint.setAuthor(AuthorDetails.builder()
                            .id(postComplaint.getAuthorId())
                            .build());
                    return jsonPostComplaint;
                }).toList();

        var authorIds = new HashSet<Long>();
        jsonPostComplaints.forEach(postComplaint -> authorIds.add(postComplaint.getAuthor().getId()));
        var authorsDetailsList = getAuthorsDetailsByAuthorIds(authorIds);
        aggregatePostWithDetails(jsonPostComplaints, authorsDetailsList);
        return jsonPostComplaints;
    }

    private PostComplaint getPostComplaintInternal(Long personId, Post post, Long complaintId) {
        return repository.findById(complaintId)
                .filter(c -> c.getAuthorId().equals(personId) && c.getPost().getId().equals(post.getId()))
                .orElseThrow(() -> new NotFoundException("error.not.found.complaint", complaintId));
    }

    @NotNull
    private PostComplaint createComplaintInternal(Long personId, Post post, PostComplaintData data) {
        LOGGER.debug("Creating new complaint from person: {} with data: {}",
                personId, data);
        checkReason(post.getId(), data);

        if (!PostState.PUBLISHED.equals(post.getState())) {
            throw new ForbiddenException("error.post.complaint.wrong.post.state", post.getState().name());
        }

        Optional<PostComplaint> complaintOpt = repository.findByAuthorIdAndPostId(personId, post.getId());
        if (complaintOpt.isPresent()) {
            throw new NotUniqueException("error.post.complaint.already.created");
        }

        // Find predefined reason (if any):
        Reason reason = Objects.nonNull(data.getReason()) ? reasonManager.getComplaintReason(data.getReason().getId()) : null;

        PostComplaint complaint = PostComplaint.builder()
                .authorId(personId)
                .post(post)
                .reason(reason)
                .reasonOther(data.getReasonOther())
                .state(PostComplaintState.PENDING)
                .build();

        repository.save(complaint);
        return complaint;
    }

    @NotNull
    private PostComplaint updateComplaintInternal(Long personId, Post post, Long complaintId, PostComplaintData data) {
        checkReason(post.getId(), data);
        PostComplaint complaint = getPostComplaintInternal(personId, post, complaintId);

        // Find predefined reason (if any):
        Reason reason = Objects.nonNull(data.getReason()) ? reasonManager.getComplaintReason(data.getReason().getId()) : null;

        complaint.setReason(reason);
        complaint.setReasonOther(data.getReasonOther());

        repository.save(complaint);
        return complaint;
    }

    private List<AuthorDetails> getAuthorsDetailsByAuthorIds(Set<Long> authorIds) {
        var personList = personManager.getPersonsByIds(List.copyOf(authorIds));
        return personList.stream().map(person -> AuthorDetails
                .builder()
                .id(person.getPersonId())
                .uuid(person.getUuid())
                .displayName(person.getDisplayName())
                .avatar(getAvatarDetails(person.getAvatar()))
                .isVerified(person.isVerifiedAccount())
                .build()).toList();
    }

    private AvatarDetails getAvatarDetails(JsonMediaFile avatarMediaFile) {
        AvatarDetails avatarDetails = null;
        if (avatarMediaFile != null) {
            avatarDetails = AvatarDetails
                    .builder()
                    .mimeType(avatarMediaFile.getMimeType())
                    .path(avatarMediaFile.getPath())
                    .size(avatarMediaFile.getSize())
                    .sizedImages(avatarMediaFile.getSizedImages())
                    .build();
        }
        return avatarDetails;
    }

    private void aggregatePostWithDetails(List<JsonPostComplaint> postComplaintList,
                                         List<AuthorDetails> authorDetailsList) {
        postComplaintList.forEach(postDetails -> authorDetailsList.forEach(authorDetails -> {
            if (authorDetails.getId().equals(postDetails.getAuthor().getId())) {
                var author = postDetails.getAuthor();
                author.setUuid(authorDetails.getUuid());
                author.setAvatar(authorDetails.getAvatar());
                author.setDisplayName(authorDetails.getDisplayName());
                author.setIsVerified(authorDetails.getIsVerified());
            }
        }));
    }
}
