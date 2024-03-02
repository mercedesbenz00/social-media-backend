package iq.earthlink.social.notificationservice.service.token;

import iq.earthlink.social.notificationservice.data.model.PersonToken;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import javax.annotation.Nonnull;

public interface PersonTokenManager {

    @Nonnull
    PersonToken addPushToken(Long personId, String pushToken, String device);

    void deletePushToken(@Nonnull Long personId, @Nonnull String pushToken);

    @Nonnull
    Page<PersonToken> getPushTokensByPersonId(Long personId, Pageable page);

    @Nonnull
    Page<PersonToken> getPushTokensByPersonIdAndDevice(Long personId, String device, Pageable page);
}
