package iq.earthlink.social.notificationservice.service.notification;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.hibernate.validator.constraints.Range;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Data
@AllArgsConstructor
@NoArgsConstructor
@ConfigurationProperties("social.notificationservice")
@Component
@Validated
public class NotificationProperties {
    @NonNull
    @Range(min = 1, max = 10000)
    private String lastNotificationsIntervalDays;
}
