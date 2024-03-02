package iq.earthlink.social.notificationservice.service.token;

import iq.earthlink.social.exception.NotFoundException;
import iq.earthlink.social.notificationservice.data.dto.JsonPushTokenAction;
import iq.earthlink.social.notificationservice.data.dto.TokenActionType;
import iq.earthlink.social.notificationservice.data.model.PersonToken;
import iq.earthlink.social.notificationservice.data.repository.PersonTokenRepository;
import iq.earthlink.social.notificationservice.event.KafkaProducerService;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.time.DateUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Nonnull;
import java.util.Date;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DefaultPersonTokenManager implements PersonTokenManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultPersonTokenManager.class);
    private final PersonTokenRepository personTokenRepository;
    private final PersonTokenProperties properties;
    private final KafkaProducerService kafkaProducerService;

    @Transactional
    @Nonnull
    @Override
    public PersonToken addPushToken(Long personId, String pushToken, String device) {
        Optional<PersonToken> personTokenOpt = personTokenRepository.findByPersonIdAndDevice(personId, device);
        PersonToken personToken;
        if (personTokenOpt.isPresent()) {
            personToken = personTokenOpt.get();
            personToken.setPushToken(pushToken);
        } else {
            personToken = PersonToken.builder()
                    .personId(personId)
                    .pushToken(pushToken)
                    .device(device)
                    .build();
        }
        personToken = personTokenRepository.save(personToken);

        JsonPushTokenAction tokenAction = JsonPushTokenAction
                .builder()
                .action(TokenActionType.SET)
                .device(device)
                .personId(personId)
                .pushToken(pushToken)
                .build();
        kafkaProducerService.sendMessage("PushTokenAction", tokenAction);

        return personToken;
    }


    @Transactional
    @Override
    public void deletePushToken(@NonNull Long personId, @NonNull String pushToken) {
        PersonToken pt = personTokenRepository.findByPushTokenAndPersonId(pushToken, personId)
                .orElseThrow(() -> new NotFoundException(PersonToken.class, pushToken));

        personTokenRepository.deleteByPushTokenAndPersonId(pushToken, personId);
        JsonPushTokenAction tokenAction = JsonPushTokenAction
                .builder()
                .action(TokenActionType.DELETE)
                .device(pt.getDevice())
                .personId(personId)
                .pushToken(pushToken)
                .build();
        kafkaProducerService.sendMessage("PushTokenAction", tokenAction);
    }

    @Nonnull
    public Page<PersonToken> getPushTokensByPersonId(Long personId, Pageable page) {
        return personTokenRepository.findByPersonId(personId, page);
    }

    @NotNull
    @Override
    public Page<PersonToken> getPushTokensByPersonIdAndDevice(Long personId, String device, Pageable page) {
        return personTokenRepository.findByPersonIdAndDevice(personId, device, page);
    }

    @Scheduled(cron = "${social.notificationservice.persontoken.cleanup.cron}")
    @Transactional
    public void deleteOldTokens() {
        LOGGER.info("Running a scheduled job to delete old tokens...");
        Date clearBeforeDate = DateUtils.addDays(new Date(), -1 * properties.getDeleteAfterDays());
        Long numberOfRemoved = personTokenRepository.deleteByUpdatedAtBefore(clearBeforeDate);
        LOGGER.info("Removed  {} records from person_token table. ", numberOfRemoved);
    }
}
