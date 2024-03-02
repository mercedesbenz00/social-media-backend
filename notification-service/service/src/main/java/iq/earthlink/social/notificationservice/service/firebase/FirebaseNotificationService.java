package iq.earthlink.social.notificationservice.service.firebase;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import iq.earthlink.social.notificationservice.data.model.PersonToken;
import iq.earthlink.social.notificationservice.model.PushNotificationRequest;
import iq.earthlink.social.notificationservice.service.token.DefaultPersonTokenManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.Map;

@Service
@Slf4j
public class FirebaseNotificationService {

    @Autowired
    private FCMService fcmService;

    @Value("${app.firebase-configuration-file}")
    private String firebaseConfigPath;

    @Autowired
    private DefaultPersonTokenManager defaultPersonTokenManager;

    @PostConstruct
    public void initialize() {
        try {
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(new ClassPathResource(firebaseConfigPath).getInputStream())).build();
            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options);
                log.info("Firebase application has been initialized");
            }
        } catch (IOException e) {
            log.error("Error initializing Firebase push notification service", e);
        }
    }

    public String sendPushNotificationToToken(PushNotificationRequest request) {
        return fcmService.sendMessageToToken(request);
    }

    public void sendPushNotificationToPerson(Long personId, String title, String message) {
        sendPushNotificationToPerson(personId, title, message, "", null);
    }

    public void sendPushNotificationToPerson(Long personId, String title, String message, String topic, Map<String, String> data) {
        Page<PersonToken> personTokens = defaultPersonTokenManager.getPushTokensByPersonId(personId, Pageable.unpaged());
        StringBuilder sb = new StringBuilder();
        for (PersonToken personToken : personTokens) {
            try {
                PushNotificationRequest request = new PushNotificationRequest();
                request.setToken(personToken.getPushToken());
                request.setTitle(title);
                request.setMessage(message);
                request.setTopic(topic);
                request.setData(data);
                String response = sendPushNotificationToToken(request);
                sb.append(personToken).append(" : ").append(response);
            } catch (Exception e) {
                sb.append(personToken.toString()).append(" : ").append(e);
                log.error(String.format("Error trying to send message to %s, %s", personId, personToken.getPushToken()), e);
            }
        }
    }
}
