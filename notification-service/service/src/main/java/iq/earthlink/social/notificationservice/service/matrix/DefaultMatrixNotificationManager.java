package iq.earthlink.social.notificationservice.service.matrix;

import iq.earthlink.social.notificationservice.data.dto.matrix.Device;
import iq.earthlink.social.notificationservice.data.dto.matrix.MatrixNotification;
import iq.earthlink.social.notificationservice.data.dto.matrix.MatrixNotificationResponse;
import iq.earthlink.social.notificationservice.data.dto.matrix.NotificationPayload;
import iq.earthlink.social.notificationservice.model.PushNotificationRequest;
import iq.earthlink.social.notificationservice.service.firebase.FirebaseNotificationService;
import iq.earthlink.social.util.LocalizationUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.Nonnull;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
@Slf4j
public class DefaultMatrixNotificationManager implements MatrixNotificationManager {

    private final FirebaseNotificationService firebaseNotificationService;
    private final LocalizationUtil localizationUtil;

    @Value("${social.appId}")
    private String appId;

    private static final String MATRIX_MESSAGE_PREFIX = "push.notification.matrix.";

    public DefaultMatrixNotificationManager(FirebaseNotificationService firebaseNotificationService, LocalizationUtil localizationUtil) {
        this.firebaseNotificationService = firebaseNotificationService;
        this.localizationUtil = localizationUtil;
    }

    @Override
    public MatrixNotificationResponse pushNotification(MatrixNotification matrixNotification) {
        NotificationPayload notification = matrixNotification.getNotification();
        Device[] devices = notification.getDevices();
        var response = new MatrixNotificationResponse();
        List<String> rejected = new ArrayList<>();
        for (Device device : devices) {
            try {
                PushNotificationRequest request = getLocalizedPushNotificationRequest(Locale.ENGLISH, device.getPushkey(), notification);

                if (request == null || !appId.equals(device.getAppId())) {
                    if (request == null) {
                        log.error("Malformed notification payload for the push key: {}", device.getPushkey());
                    } else {
                        log.error("Provided wrong app id: {} for the push key: {}", device.getAppId(), device.getPushkey());
                    }
                    rejected.add(device.getPushkey());
                    response.setRejected(rejected.toArray(new String[0]));
                    continue;
                }

                String pushResponse = "";
                for (int retryCount = 0; !pushResponse.equals("ok") && retryCount < 3; retryCount++) {
                    pushResponse = firebaseNotificationService.sendPushNotificationToToken(request);
                }
                if (!pushResponse.equals("ok")) {
                    rejected.add(device.getPushkey());
                    log.error("Failed to send push notification to token: " + device.getPushkey());
                }
            } catch (Exception ex) {
                log.error("Failed to send push notification to token: {}, error: {}",  device.getPushkey(), ex.getMessage());
                rejected.add(device.getPushkey());
            }
            response.setRejected(rejected.toArray(new String[0]));
        }

        return response;
    }

    public PushNotificationRequest getLocalizedPushNotificationRequest(Locale locale, @Nonnull String pushKey, NotificationPayload notification) {
        if (notification.getContent() == null || notification.getContent().isEmpty() || notification.getType() == null) {
            return null;
        }

        String title = localizationUtil.getLocalizedMessage(MATRIX_MESSAGE_PREFIX + notification.getType(), locale);
        String userName = notification.getSenderDisplayName() != null ? notification.getSenderDisplayName() : "Chat user";
        Map<String, Object> content = notification.getContent();
        String message = "";


        if (notification.getType().equals("m.room.member") && content.get("membership") != null) {
            String chatEventType = content.get("membership").toString();
            message = localizationUtil.getLocalizedMessage(MATRIX_MESSAGE_PREFIX + chatEventType, locale, userName);
        } else if (notification.getType().equals("m.room.message") && content.get("body") != null) {
            var msgType = content.get("msgtype");
            if (msgType != null && !msgType.toString().equals("m.text")) {
                message = userName + ": " + localizationUtil.getLocalizedMessage(MATRIX_MESSAGE_PREFIX + msgType, locale, userName);
            } else {
                message = userName + ": " + StringUtils.abbreviate(content.get("body").toString(), 60);
            }
        }
        Map<String, String> data = new HashMap<>();
        data.put("content", notification.getContent().toString());
        data.put("event_id", notification.getEventId());
        data.put("prio", notification.getPrio());
        data.put("room_id", notification.getRoomId());
        data.put("room_name", notification.getRoomName());
        data.put("sender", notification.getSender());
        data.put("sender_display_name", notification.getSenderDisplayName());
        data.put("type", notification.getType());

        data.replaceAll((key, value) -> truncateStringByMaxBytes(value, 1024));
        data.values().removeAll(Collections.singleton(null));

        return PushNotificationRequest
                .builder()
                .topic(notification.getType())
                .title(title)
                .message(message)
                .token(pushKey)
                .data(data)
                .build();
    }

    public static String truncateStringByMaxBytes(String s, int maxBytes) {
        if (s == null) {
            return null;
        }
        Charset charset = StandardCharsets.UTF_8;
        CharsetDecoder decoder = charset.newDecoder();
        byte[] sba = s.getBytes(charset);
        if (sba.length <= maxBytes) {
            return s;
        }
        ByteBuffer bb = ByteBuffer.wrap(sba, 0, maxBytes);
        CharBuffer cb = CharBuffer.allocate(maxBytes);
        decoder.onMalformedInput(CodingErrorAction.IGNORE);
        decoder.decode(bb, cb, true);
        decoder.flush(cb);
        return new String(cb.array(), 0, cb.position());
    }
}
