package iq.earthlink.social.notificationservice.service;

import iq.earthlink.social.classes.data.dto.PersonData;
import iq.earthlink.social.classes.data.event.NotificationEvent;
import iq.earthlink.social.classes.enumeration.NotificationState;
import iq.earthlink.social.classes.enumeration.NotificationType;
import iq.earthlink.social.notificationservice.data.model.Notification;
import iq.earthlink.social.notificationservice.data.repository.NotificationRepository;
import iq.earthlink.social.notificationservice.service.notification.DefaultNotificationManager;
import iq.earthlink.social.notificationservice.service.notification.NotificationProperties;
import iq.earthlink.social.util.LocalizationUtil;
import org.apache.commons.lang3.time.DateUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class DefaultNotificationManagerTest {

    @InjectMocks
    private DefaultNotificationManager notificationManager;
    @Mock
    private NotificationRepository notificationRepository;
    @Mock
    private NotificationProperties properties;
    @Mock
    private LocalizationUtil localizationUtil;
    @Captor
    private ArgumentCaptor<ArrayList<Notification>> notificationsCaptor;
    @Captor
    private ArgumentCaptor<Set<Notification>> batchNotifications;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void createNotifications_createNotificationsForTwoReceiverIds_notificationsAreCreated() {
        //given
        HashMap<String, String> metadata = new HashMap<>();
        metadata.put("groupId", "1");

        PersonData personData = PersonData.builder()
                .id(1L)
                .displayName("John Smith")
                .build();

        NotificationEvent event = NotificationEvent.builder()
                .type(NotificationType.USER_INVITED_TO_GROUP)
                .metadata(metadata)
                .receiverIds(List.of(1L, 2L))
                .eventAuthor(personData)
                .build();


        //when
        notificationManager.createNotifications(event);

        //then
        then(notificationRepository).should().saveAll(notificationsCaptor.capture());
        verify(notificationRepository, times(1)).deletePreviousGroupInvitations(any(), any());
        verify(notificationRepository, times(1)).saveAll(any());
        assertEquals(2, notificationsCaptor.getValue().size());
        assertEquals(event.getType(), notificationsCaptor.getValue().get(0).getTopic());
        assertEquals(event.getMetadata(), notificationsCaptor.getValue().get(0).getMetadata());
        assertEquals(NotificationState.NEW, notificationsCaptor.getValue().get(0).getState());
    }

    @Test
    void findLatestNotifications_findWithNotNullState_returnPageOfNotifications() {
        //given
        Long receiverPersonId = 1L;

        List<Notification> notifications = getNotifications().stream()
                .filter(notification -> notification.getState() == NotificationState.NEW)
                .collect(Collectors.toList());

        Pageable pageable = PageRequest.of(0, 3);
        Page<Notification> page = new PageImpl<>(notifications, PageRequest.of(0, 3), notifications.size());

        given(properties.getLastNotificationsIntervalDays()).willReturn("1");
        given(notificationRepository.findByReceiverIdAndStateAndCreatedDateAfterOrderByCreatedDateDesc(any(), any(), any(), any())).willReturn(page);

        //when
        Page<Notification> latestNotifications = notificationManager.findLatestNotifications(receiverPersonId, false, NotificationState.NEW, pageable);

        //then
        assertEquals(1, latestNotifications.getContent().size());
        assertEquals(1L, latestNotifications.getContent().get(0).getReceiverId().longValue());
        assertEquals(1L, latestNotifications.getContent().get(0).getId().longValue());
    }

    @Test
    void findLatestNotifications_findAllWithDeleted_returnPageOfNotifications() {
        //given
        Long receiverPersonId = 1L;

        List<Notification> notifications = getNotifications();

        Pageable pageable = PageRequest.of(0, 3);
        Page<Notification> page = new PageImpl<>(notifications, PageRequest.of(0, 3), notifications.size());

        given(properties.getLastNotificationsIntervalDays()).willReturn("1");
        given(notificationRepository.findByReceiverIdAndCreatedDateAfterOrderByCreatedDateDesc(any(), any(), any())).willReturn(page);

        //when
        Page<Notification> latestNotifications = notificationManager.findLatestNotifications(receiverPersonId, true, null, pageable);

        //then
        assertEquals(3, latestNotifications.getContent().size());
    }

    @Test
    void findLatestNotifications_findAllWithoutDeleted_returnPageOfNotifications() {
        //given
        Long receiverPersonId = 1L;

        List<Notification> notifications = getNotifications().stream()
                .filter(notification -> notification.getState() != NotificationState.DELETED)
                .collect(Collectors.toList());

        Pageable pageable = PageRequest.of(0, 3);
        Page<Notification> page = new PageImpl<>(notifications, PageRequest.of(0, 3), notifications.size());

        given(properties.getLastNotificationsIntervalDays()).willReturn("1");
        given(notificationRepository.findActiveNotifications(any(), any(), any())).willReturn(page);

        //when
        Page<Notification> latestNotifications = notificationManager.findLatestNotifications(receiverPersonId, false, null, pageable);

        //then
        assertEquals(2, latestNotifications.getContent().size());
    }

    @Test
    void updateNotificationState_updateNotificationsStateToRead_notificationStateChanged() {
        //given
        List<Long> notificationIds = List.of(1L, 2L);

        List<Notification> notificationsByIds = getNotifications().stream()
                .filter(notification -> notificationIds.contains(notification.getId()))
                .collect(Collectors.toList());

        HashSet<Notification> allNotifications = new HashSet<>(getNotifications());

        given(notificationRepository.findByIdIn(notificationIds)).willReturn(notificationsByIds);
        given(notificationRepository.findByBatchIdIn(any())).willReturn(allNotifications);

        //when
        List<Notification> actualNotifications = notificationManager.updateNotificationState(notificationIds, NotificationState.READ);

        //then
        then(notificationRepository).should().saveAll(batchNotifications.capture());
        verify(notificationRepository, times(1)).saveAll(allNotifications);
        assertEquals(2, actualNotifications.size());
        assertEquals(3, batchNotifications.getValue().size());
        assertEquals(NotificationState.READ, batchNotifications.getValue().iterator().next().getState());
    }

    private List<Notification> getNotifications() {
        ArrayList<Notification> notifications = new ArrayList<>();
        Notification notification1 = Notification.builder()
                .id(1L)
                .receiverId(1L)
                .state(NotificationState.NEW)
                .topic(NotificationType.USER_INVITED_TO_GROUP)
                .batchId(1)
                .createdDate(DateUtils.addDays(new Date(), -1))
                .build();
        Notification notification2 = Notification.builder()
                .id(2L)
                .receiverId(2L)
                .state(NotificationState.DELETED)
                .topic(NotificationType.USER_INVITED_TO_GROUP)
                .batchId(2)
                .createdDate(DateUtils.addDays(new Date(), -1))
                .build();
        Notification notification3 = Notification.builder()
                .id(3L)
                .receiverId(1L)
                .state(null)
                .topic(NotificationType.USER_INVITED_TO_GROUP)
                .batchId(1)
                .createdDate(DateUtils.addDays(new Date(), -1))
                .build();

        notifications.add(notification1);
        notifications.add(notification2);
        notifications.add(notification3);

        return notifications;
    }
}