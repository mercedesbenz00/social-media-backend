package iq.earthlink.social.notificationservice.service.matrix;

import iq.earthlink.social.notificationservice.data.dto.matrix.Device;
import iq.earthlink.social.notificationservice.data.dto.matrix.MatrixNotification;
import iq.earthlink.social.notificationservice.data.dto.matrix.NotificationPayload;
import iq.earthlink.social.notificationservice.model.PushNotificationRequest;
import iq.earthlink.social.notificationservice.service.firebase.FirebaseNotificationService;
import iq.earthlink.social.util.LocalizationUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class DefaultMatrixNotificationManagerTest {

    private static final String SOCIAL_MEDIA = "social.media";
    private static final String MSG_TYPE = "msgtype";
    private static final String M_TEXT = "m.text";
    private static final String SENDER = "@user.98:matrix";
    private static final String ROOM_ID = "room_id";
    private static final String ROOM_NAME = "room Name";
    private static final String SENDER_DISPLAY_NAME = "Admin User";
    private static final String EVENT_UUID = "event_UUID";
    private static final String BODY_CONTENT = "Hello friend";
    private static final String M_ROOM_MESSAGE = "m.room.message";
    private static final String PUSH_KEY = "pushKey";
    private static final String MESSAGE = "This is the message";




    @InjectMocks
    DefaultMatrixNotificationManager matrixNotificationManager;

    @Mock
    FirebaseNotificationService firebaseNotificationService;
    @Mock
    LocalizationUtil localizationUtil;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        ReflectionTestUtils.setField(matrixNotificationManager, "appId", SOCIAL_MEDIA);
    }

    @Test
    void pushNotification_receiveChatEventMessageWithWrongAppId_receiveRejectCountAndWillNotPush() {
        //given
        var messageNotification = MatrixNotification
                .builder()
                .notification(NotificationPayload
                        .builder()
                        .content(Map.of("body", BODY_CONTENT, MSG_TYPE, M_TEXT))
                        .eventId(EVENT_UUID)
                        .prio("high")
                        .sender(SENDER)
                        .roomId(ROOM_ID)
                        .roomName(ROOM_NAME)
                        .senderDisplayName(SENDER_DISPLAY_NAME)
                        .devices(new Device[]{new Device("wrong app id", PUSH_KEY)})
                        .type(M_ROOM_MESSAGE)
                        .build())
                .build();
        //when
        var response = matrixNotificationManager.pushNotification(messageNotification);
        //then
        assertThat(response.getRejected().length).isEqualTo(1);
        verify(firebaseNotificationService, times(0)).sendPushNotificationToToken(any());
    }


    @Test
    void pushNotification_receiveChatEventMessageWithCorrectAppId_sendPushNotification() {
        //given
        var messageNotification = MatrixNotification
                .builder()
                .notification(NotificationPayload
                        .builder()
                        .content(Map.of("body", BODY_CONTENT, MSG_TYPE, M_TEXT))
                        .eventId(EVENT_UUID)
                        .prio("high")
                        .sender(SENDER)
                        .roomId(ROOM_ID)
                        .roomName(ROOM_NAME)
                        .senderDisplayName(SENDER_DISPLAY_NAME)
                        .devices(new Device[]{new Device(SOCIAL_MEDIA, PUSH_KEY)})
                        .type(M_ROOM_MESSAGE)
                        .build())
                .build();
        given(localizationUtil.getLocalizedMessage(any(), any(), any())).willReturn(MESSAGE);
        given(firebaseNotificationService.sendPushNotificationToToken(any(PushNotificationRequest.class))).willReturn("ok");

        //when
        var response = matrixNotificationManager.pushNotification(messageNotification);
        //then
        assertThat(response.getRejected().length).isEqualTo(0);
        verify(firebaseNotificationService, times(1)).sendPushNotificationToToken(any());
    }

    @Test
    void pushNotification_receiveChatEventMessageWithTwoDevices_sendPushNotificationTwice() {
        //given
        var messageNotification = MatrixNotification
                .builder()
                .notification(NotificationPayload
                        .builder()
                        .content(Map.of("body", BODY_CONTENT, MSG_TYPE, M_TEXT))
                        .eventId(EVENT_UUID)
                        .prio("high")
                        .sender(SENDER)
                        .roomId(ROOM_ID)
                        .roomName(ROOM_NAME)
                        .senderDisplayName(SENDER_DISPLAY_NAME)
                        .devices(new Device[]{
                                new Device(SOCIAL_MEDIA, PUSH_KEY),
                                new Device(SOCIAL_MEDIA, "pushKey2")
                        })
                        .type(M_ROOM_MESSAGE)
                        .build())
                .build();
        given(localizationUtil.getLocalizedMessage(any(), any(), any())).willReturn(MESSAGE);
        given(firebaseNotificationService.sendPushNotificationToToken(any(PushNotificationRequest.class))).willReturn("ok");

        //when
        var response = matrixNotificationManager.pushNotification(messageNotification);
        //then
        assertThat(response.getRejected().length).isEqualTo(0);
        verify(firebaseNotificationService, times(2)).sendPushNotificationToToken(any());
    }

    @Test
    void pushNotification_firebasePushFails_returnRejectCount() {
        //given
        var messageNotification = MatrixNotification
                .builder()
                .notification(NotificationPayload
                        .builder()
                        .content(Map.of("body", BODY_CONTENT, MSG_TYPE, M_TEXT))
                        .eventId(EVENT_UUID)
                        .prio("high")
                        .sender(SENDER)
                        .roomId(ROOM_ID)
                        .roomName(ROOM_NAME)
                        .senderDisplayName(SENDER_DISPLAY_NAME)
                        .devices(new Device[]{
                                new Device(SOCIAL_MEDIA, PUSH_KEY)
                        })
                        .type(M_ROOM_MESSAGE)
                        .build())
                .build();
        given(localizationUtil.getLocalizedMessage(any(), any(), any())).willReturn(MESSAGE);
        given(firebaseNotificationService.sendPushNotificationToToken(any(PushNotificationRequest.class))).willReturn("failed");

        //when
        var response = matrixNotificationManager.pushNotification(messageNotification);
        //then
        assertThat(response.getRejected().length).isEqualTo(1);
        verify(firebaseNotificationService, times(3)).sendPushNotificationToToken(any());
    }

    @Test
    void pushNotification_receiveGroupInvite_sendPushNotification() {
        //given
        var messageNotification = MatrixNotification
                .builder()
                .notification(NotificationPayload
                        .builder()
                        .content(Map.of("membership", "invite"))
                        .eventId(EVENT_UUID)
                        .prio("high")
                        .sender(SENDER)
                        .roomId(ROOM_ID)
                        .roomName(ROOM_NAME)
                        .senderDisplayName(SENDER_DISPLAY_NAME)
                        .devices(new Device[]{
                                new Device(SOCIAL_MEDIA, PUSH_KEY)
                        })
                        .type("m.room.member")
                        .build())
                .build();
        given(localizationUtil.getLocalizedMessage(any(), any(), any())).willReturn(MESSAGE);
        given(firebaseNotificationService.sendPushNotificationToToken(any(PushNotificationRequest.class))).willReturn("ok");

        //when
        var response = matrixNotificationManager.pushNotification(messageNotification);
        //then
        assertThat(response.getRejected().length).isEqualTo(0);
        verify(firebaseNotificationService, times(1)).sendPushNotificationToToken(any());
    }
}