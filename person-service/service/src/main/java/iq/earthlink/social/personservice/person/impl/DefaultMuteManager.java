package iq.earthlink.social.personservice.person.impl;


import iq.earthlink.social.common.audit.AuditMarker;
import iq.earthlink.social.common.audit.AuditMessage;
import iq.earthlink.social.common.audit.EventAction;
import iq.earthlink.social.exception.NotUniqueException;
import iq.earthlink.social.personservice.person.MuteManager;
import iq.earthlink.social.personservice.person.impl.repository.MuteRepository;
import iq.earthlink.social.personservice.person.model.Person;
import iq.earthlink.social.personservice.person.model.PersonMute;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Nonnull;
import java.util.List;

import static iq.earthlink.social.common.util.CommonConstants.ERROR_CHECK_NOT_NULL;
import static iq.earthlink.social.common.util.Preconditions.checkNotNull;


@Service
public class DefaultMuteManager implements MuteManager {

    private static final Logger LOG = LogManager.getLogger(DefaultMuteManager.class);

    private final MuteRepository repository;
    private static final String OWNER = "owner";

    public DefaultMuteManager(
            MuteRepository repository) {
        this.repository = repository;
    }

    @Nonnull
    @Override
    public Page<PersonMute> findMutes(@Nonnull Person owner, @Nonnull Pageable page) {
        checkNotNull(owner, ERROR_CHECK_NOT_NULL, OWNER);
        checkNotNull(page, ERROR_CHECK_NOT_NULL, "page");

        return repository.findAllByPersonId(owner.getId(), page);
    }

    @Nonnull
    @Override
    public PersonMute createMute(@Nonnull Person owner, @Nonnull Person mutedPerson) {
        checkNotNull(owner, ERROR_CHECK_NOT_NULL, OWNER);
        checkNotNull(mutedPerson, ERROR_CHECK_NOT_NULL, "mutedPerson");

        PersonMute mute = new PersonMute();
        mute.setPerson(owner);
        mute.setMutedPerson(mutedPerson);
        try {
            PersonMute personMute = repository.save(mute);
            String logMessage = String.format("The person \"%s\" was muted by \"%s\"", mutedPerson.getUsername(), owner.getUsername());
            LOG.info(AuditMarker.getMarker(), AuditMessage.create(EventAction.MUTE, logMessage, owner.getId(), mutedPerson.getId()));
            return personMute;
        } catch (Exception ex) {
          throw new NotUniqueException("error.mute.already.exists");
        }
    }

    @Transactional
    @Override
    public void removeMute(@Nonnull Long ownerId, @Nonnull Person mutedPerson) {
        checkNotNull(ownerId, ERROR_CHECK_NOT_NULL, OWNER);
        checkNotNull(mutedPerson, ERROR_CHECK_NOT_NULL, "mutedPerson");

        repository.removeMute(ownerId, mutedPerson.getId());

        String logMessage = String.format("The mute for the person \"%s\" was removed", mutedPerson.getUsername());
        LOG.info(AuditMarker.getMarker(), AuditMessage.create(EventAction.UNMUTE, logMessage, ownerId, mutedPerson.getId()));
    }

    @Override
    public List<Long> findWhoMutes(Long personId) {
        List<PersonMute> allByMutedPersonId = repository.findAllByMutedPersonId(personId);
        return allByMutedPersonId.stream()
                .map(PersonMute::getPerson)
                .map(Person::getId)
                .toList();
    }
}
