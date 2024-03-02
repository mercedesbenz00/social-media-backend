package iq.earthlink.social.notificationservice.service;

import iq.earthlink.social.classes.data.event.NotificationEvent;
import iq.earthlink.social.classes.enumeration.NotificationType;
import iq.earthlink.social.notificationservice.data.model.PersonToken;
import iq.earthlink.social.notificationservice.data.repository.PersonTokenRepository;
import iq.earthlink.social.notificationservice.service.firebase.FCMService;
import iq.earthlink.social.notificationservice.service.firebase.FirebaseNotificationService;
import iq.earthlink.social.notificationservice.service.token.DefaultPersonTokenManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.PageImpl;

import java.util.Date;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class FirebaseNotificationServiceTest {
    @InjectMocks
    private FirebaseNotificationService firebaseNotificationService;
    @Mock
    private PersonTokenRepository personTokenRepository;
    @Mock
    private FCMService fcmService;
    @Mock
    private DefaultPersonTokenManager defaultPersonTokenManager;;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    void sendPushNotificationToPerson_validInput_noErrors() {
        //given
        Long personId = 1L;
        String device = "web";
        PersonToken personToken = PersonToken.builder()
                .id(1L)
                .pushToken("push token")
                .personId(personId)
                .device(device)
                .createdAt(new Date())
                .updatedAt(new Date())
                .build();
        NotificationEvent event = NotificationEvent.builder()
                .receiverIds(List.of(personId))
                .type(NotificationType.PERSON_IS_MENTIONED_IN_COMMENT)
                .build();

        String eventName = event.getType().getMessageId();

        given(defaultPersonTokenManager.getPushTokensByPersonId(any(), any())).willReturn(new PageImpl<>(List.of(personToken)));

        //when
        // Push notifications to receivers:
        event.getReceiverIds().forEach(receiveId ->
                firebaseNotificationService.sendPushNotificationToPerson(receiveId, "test title", "test message", eventName, event.getMetadata()));

        //then
        verify(defaultPersonTokenManager, times(1)).getPushTokensByPersonId(any(), any());
    }
}