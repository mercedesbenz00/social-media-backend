package iq.earthlink.social.notificationservice.data.dto;

import lombok.*;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class JsonPushTokenAction {

    Long personId;
    String pushToken;
    String device;
    TokenActionType action;
}
