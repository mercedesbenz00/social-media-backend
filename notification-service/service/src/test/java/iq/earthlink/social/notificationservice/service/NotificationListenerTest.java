package iq.earthlink.social.notificationservice.service;

import iq.earthlink.social.classes.data.dto.PersonData;
import iq.earthlink.social.classes.data.event.NotificationEvent;
import iq.earthlink.social.classes.enumeration.ContentType;
import iq.earthlink.social.classes.enumeration.NotificationState;
import iq.earthlink.social.classes.enumeration.NotificationType;
import iq.earthlink.social.notificationservice.event.NotificationListener;
import iq.earthlink.social.notificationservice.service.firebase.FirebaseNotificationService;
import iq.earthlink.social.notificationservice.service.notification.NotificationManager;
import iq.earthlink.social.util.LocalizationUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class NotificationListenerTest {

    @InjectMocks
    @Spy
    private NotificationListener eventListener;

    @Mock
    private FirebaseNotificationService firebaseNotificationService;
    @Mock
    private NotificationManager notificationManager;
    @Mock
    private LocalizationUtil localizationUtil;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    void pushNotificationEvent_emptyReceivers_doNothing() {
        NotificationEvent event = NotificationEvent.builder()
                .type(NotificationType.POST_COMMENTED)
                .receiverIds(new ArrayList<>())
                .build();

        //given
        eventListener.pushNotificationEvent(event);
        //then
        verify(notificationManager, times(0)).createNotifications(any());
    }

    @Test
    void pushNotificationEvent_notEmptyReceivers_notificationCreated() {
        NotificationType type = NotificationType.GROUP_JOIN_REQUESTED;
        List<Long> receiverIds = List.of(1L, 2L, 3L);
        PersonData person = new PersonData();
        person.setId(1L);
        person.setDisplayName("test name");
        NotificationEvent event = NotificationEvent
                .builder()
                .eventAuthor(person)
                .receiverIds(receiverIds)
                .type(type)
                .state(NotificationState.NEW)
                .metadata(Map.of(
                        "routeTo", ContentType.GROUP.name(),
                        "groupId", "1",
                        "groupName", "test group name")).build();

        //given
        eventListener.pushNotificationEvent(event);
        //then
        verify(notificationManager, times(1)).createNotifications(any());
        verify(firebaseNotificationService, times(receiverIds.size())).sendPushNotificationToPerson(any(), any(), any(), any(), any());
    }

}
