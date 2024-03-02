package iq.earthlink.social.postservice.post.complaint;

import iq.earthlink.social.exception.ForbiddenException;
import iq.earthlink.social.exception.NotFoundException;
import iq.earthlink.social.exception.NotUniqueException;
import iq.earthlink.social.exception.RestApiException;
import iq.earthlink.social.postservice.person.dto.PersonDTO;
import iq.earthlink.social.postservice.post.comment.complaint.repository.CommentComplaintRepository;
import iq.earthlink.social.postservice.post.complaint.model.Reason;
import iq.earthlink.social.postservice.post.complaint.model.ReasonLocalized;
import iq.earthlink.social.postservice.post.complaint.repository.PostComplaintRepository;
import iq.earthlink.social.postservice.post.complaint.repository.ReasonRepository;
import iq.earthlink.social.postservice.post.rest.ReasonRequest;
import iq.earthlink.social.util.LocalizationUtil;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Nonnull;
import java.util.Map;

@Service
public class DefaultReasonManager implements ReasonManager {

    private static final String ERROR_PERSON_NOT_AUTHORIZED = "error.person.not.authorized";
    private final ReasonRepository repository;
    private final PostComplaintRepository postComplaintRepository;
    private final CommentComplaintRepository commentComplaintRepository;

    public DefaultReasonManager(ReasonRepository repository, PostComplaintRepository postComplaintRepository,
                                CommentComplaintRepository commentComplaintRepository) {
        this.repository = repository;
        this.postComplaintRepository = postComplaintRepository;
        this.commentComplaintRepository = commentComplaintRepository;
    }

    @Transactional
    @Override
    public Reason createComplaintReason(ReasonRequest data, PersonDTO person) {

        if (!person.isAdmin()) throw new ForbiddenException(ERROR_PERSON_NOT_AUTHORIZED);

        Reason reason = new Reason();
        reason.setName(data.getName());
        setReasonLocalizations(data, reason);
        try {
            return repository.saveAndFlush(reason);
        } catch (DataIntegrityViolationException ex) {
            throw new NotUniqueException("error.reason.create.duplicate", data.getName(), ex.getMostSpecificCause().getMessage());
        }
    }

    @Override
    public Reason getComplaintReason(@Nonnull Long reasonId) {
        return repository.findById(reasonId)
                .orElseThrow(() -> new NotFoundException("error.reason.not.found", reasonId));
    }

    @Override
    public Page<Reason> findComplaintReasons(ReasonSearchCriteria criteria, Pageable page) {
        return repository.findReasons(criteria, page);
    }

    @Transactional
    @Override
    public Reason updateComplaintReason(@Nonnull Long reasonId, ReasonRequest data, PersonDTO person) {

        if (!person.isAdmin()) throw new ForbiddenException(ERROR_PERSON_NOT_AUTHORIZED);

        Reason reason = getComplaintReason(reasonId);
        try {
            if (data.getName() != null) {
                reason.setName(data.getName());
            }
            setReasonLocalizations(data, reason);
            repository.flush();
        } catch (DataIntegrityViolationException ex) {
            throw new RestApiException(HttpStatus.CONFLICT, "error.reason.update.with.existing.name", reasonId);
        }
        return reason;
    }

    @Transactional
    @Override
    public void removeComplaintReason(@Nonnull Long reasonId, PersonDTO person) {
        if (!person.isAdmin()) throw new ForbiddenException(ERROR_PERSON_NOT_AUTHORIZED);

        if (repository.existsById(reasonId)) {
            Reason reason = repository.getReferenceById(reasonId);
            if (postComplaintRepository.existsByReason(reason) || commentComplaintRepository.existsByReason(reason)) {
                throw new RestApiException(HttpStatus.CONFLICT, "error.reason.remove.in.use", reasonId);
            }
            repository.deleteById(reasonId);
        } else {
            throw new NotFoundException("error.reason.not.found", reasonId);
        }
    }

    @Transactional
    public void setReasonLocalizations(ReasonRequest req, Reason reason) {
        if (req.getLocalizations() != null && !req.getLocalizations().isEmpty()) {
            reason.getLocalizations().clear();
            for (Map.Entry<String, String> entry : req.getLocalizations().entrySet()) {
                String locale = entry.getKey();
                String name = entry.getValue();
                LocalizationUtil.checkLocalization(locale);
                reason.getLocalizations().put(locale, createReasonLocalized(reason, name, locale));
            }
        }
    }

    private ReasonLocalized createReasonLocalized(Reason reason, String reasonName, String locale) {
        return ReasonLocalized
                .builder()
                .locale(locale)
                .reason(reason)
                .name(reasonName)
                .build();
    }
}
