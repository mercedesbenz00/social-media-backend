package iq.earthlink.social.personservice.service;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import iq.earthlink.social.common.util.CommonConstants;
import iq.earthlink.social.common.util.SynapseUtil;
import iq.earthlink.social.notificationservice.data.dto.JsonPushTokenAction;
import iq.earthlink.social.notificationservice.data.dto.TokenActionType;
import iq.earthlink.social.personservice.dto.ListMatrixPushTokenDTO;
import iq.earthlink.social.personservice.dto.LoginAsUserResponseDTO;
import iq.earthlink.social.personservice.dto.MatrixPushTokenDTO;
import iq.earthlink.social.personservice.dto.PusherDataDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ChatAdministrationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ChatAdministrationService.class);

    @Value("${chat.synapse.url}")
    private String synapseURL;
    @Value("${chat.synapse.pusher.url}")
    private String pusherUrl;

    @Value("${chat.synapse.serverName}")
    private String synapseServerName;

    @Value("${chat.synapse.adminToken}")
    private String adminToken;
    @Value("${chat.synapse.pushAppName}")
    private String pushAppName;

    private final WebClient localApiClient;
    private static final String AUTHORIZATION = "Authorization";
    private static final String BEARER = "Bearer ";
    private static final String SYNAPSE_WEBCLIENT_ERROR = "Synapse webclient error";
    private static final String ERROR_PREFIX = "Error: ";


    public ChatAdministrationService(WebClient localApiClient) {
        this.localApiClient = localApiClient;
    }

    public void deactivateUser(Long userId) {
        String chatUserId = SynapseUtil.generateMatrixUserId(userId, synapseServerName);
        try {
            localApiClient
                    .post()
                    .uri(getDeactivateURL(chatUserId))
                    .header(AUTHORIZATION, BEARER + adminToken)
                    .retrieve()
                    .onStatus(HttpStatus::isError, clientResponse ->
                            clientResponse.bodyToMono(String.class)
                                    .flatMap(error -> {
                                        JsonObject response = (JsonObject) JsonParser.parseString(error);
                                        String errorString = response.get(CommonConstants.ERROR).toString();
                                        LOGGER.error(String.format("Failed to deactivate the user in the chat service %s", errorString));
                                        return Mono.error(new Error(ERROR_PREFIX + errorString));
                                    })
                    ).bodyToMono(String.class)
                    .block();
        } catch (Exception ex) {
            LOGGER.error(SYNAPSE_WEBCLIENT_ERROR, ex.getCause());
        }
    }

    public void reactivateUser(Long userId) {
        String chatUserId = SynapseUtil.generateMatrixUserId(userId, synapseServerName);
        Map<String, Object> bodyValues = new HashMap<>();

        bodyValues.put("deactivated", false);
        bodyValues.put("password", null);

        try {
            localApiClient
                    .put()
                    .uri(getReactivateURL(chatUserId))
                    .header(AUTHORIZATION, BEARER + adminToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(BodyInserters.fromValue(bodyValues))
                    .retrieve()
                    .onStatus(HttpStatus::isError, clientResponse ->
                            clientResponse.bodyToMono(String.class)
                                    .flatMap(error -> {
                                        JsonObject response = (JsonObject) JsonParser.parseString(error);
                                        String errorString = response.get(CommonConstants.ERROR).toString();
                                        return Mono.error(new Error(ERROR_PREFIX + errorString));
                                    })
                    ).bodyToMono(String.class)
                    .block();
        } catch (Exception ex) {
            LOGGER.error(SYNAPSE_WEBCLIENT_ERROR, ex);
        }
    }

    public void updateUserPushNotificationToken(JsonPushTokenAction action) {
        String accessToken = loginAsUser(action.getPersonId()).getAccessToken();
        if (TokenActionType.SET.equals(action.getAction())) {
            List<MatrixPushTokenDTO> tokenList = getUserPushTokens(accessToken);

            tokenList.forEach(token -> {
                if (token.getDeviceDisplayName().equals(action.getDevice())) {
                    deleteUserPushToken(accessToken, token.getPushkey());
                }
            });
            addUserPushToken(accessToken, action.getDevice(), action.getPushToken());
        } else {
            deleteUserPushToken(accessToken, action.getPushToken());
        }

    }

    public void deleteUserPushToken(String accessToken, String pushToken) {
        try {
            var body = MatrixPushTokenDTO.builder()
                    .kind(null)
                    .appId(pushAppName)
                    .pushkey(pushToken)
                    .build();

            setUserPushToken(accessToken, body);
        } catch (Exception ex) {
            LOGGER.error(SYNAPSE_WEBCLIENT_ERROR, ex);
        }
    }

    public void addUserPushToken(String accessToken, String deviceName, String pushToken) {
        var body = MatrixPushTokenDTO.builder()
                .appDisplayName("Social Media")
                .appId(pushAppName)
                .kind("http")
                .lang("en")
                .pushkey(pushToken)
                .data(PusherDataDTO.builder()
                        .defaultPayload(new HashMap<>())
                        .url(pusherUrl)
                        .build())
                .deviceDisplayName(deviceName)
                .build();

        setUserPushToken(accessToken, body);
    }

    public List<MatrixPushTokenDTO> getUserPushTokens(String accessToken) {
        ListMatrixPushTokenDTO tokenList = new ListMatrixPushTokenDTO();
        try {
            tokenList = localApiClient
                    .get()
                    .uri(synapseURL + "/_matrix/client/v3/pushers")
                    .header(AUTHORIZATION, BEARER + accessToken)
                    .retrieve()
                    .onStatus(HttpStatus::isError, clientResponse ->
                            clientResponse.bodyToMono(String.class)
                                    .flatMap(error -> {
                                        JsonObject response = (JsonObject) JsonParser.parseString(error);
                                        String errorString = response.get(CommonConstants.ERROR).toString();
                                        return Mono.error(new Error(ERROR_PREFIX + errorString));
                                    })
                    ).bodyToMono(ListMatrixPushTokenDTO.class)
                    .block();
        } catch (Exception ex) {
            LOGGER.error(SYNAPSE_WEBCLIENT_ERROR, ex);
        }
        assert tokenList != null;
        return tokenList.getPushers();
    }

    private void setUserPushToken(String accessToken, MatrixPushTokenDTO body) {
        try {
            localApiClient
                    .post()
                    .uri(synapseURL + "/_matrix/client/v3/pushers/set")
                    .header(AUTHORIZATION, BEARER + accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Mono.just(body), MatrixPushTokenDTO.class)
                    .retrieve()
                    .onStatus(HttpStatus::isError, clientResponse ->
                            clientResponse.bodyToMono(String.class)
                                    .flatMap(error -> {
                                        JsonObject response = (JsonObject) JsonParser.parseString(error);
                                        String errorString = response.get(CommonConstants.ERROR).toString();
                                        return Mono.error(new Error(ERROR_PREFIX + errorString));
                                    })
                    ).bodyToMono(Void.class)
                    .block();
        } catch (Exception ex) {
            LOGGER.error(SYNAPSE_WEBCLIENT_ERROR, ex);
        }
    }

    public LoginAsUserResponseDTO loginAsUser(Long userId) {
        String chatUserId = SynapseUtil.generateMatrixUserId(userId, synapseServerName);
        LoginAsUserResponseDTO accessToken = new LoginAsUserResponseDTO();
        try {
            accessToken = localApiClient
                    .post()
                    .uri(getUserTokenURL(chatUserId))
                    .header(AUTHORIZATION, BEARER + adminToken)
                    .retrieve()
                    .onStatus(HttpStatus::isError, clientResponse ->
                            clientResponse.bodyToMono(String.class)
                                    .flatMap(error -> {
                                        JsonObject response = (JsonObject) JsonParser.parseString(error);
                                        String errorString = response.get(CommonConstants.ERROR).toString();
                                        return Mono.error(new Error(ERROR_PREFIX + errorString));
                                    })
                    ).bodyToMono(LoginAsUserResponseDTO.class)
                    .block();
        } catch (Exception ex) {
            LOGGER.error(SYNAPSE_WEBCLIENT_ERROR, ex);
        }
        return accessToken;
    }


    private String getDeactivateURL(@Nonnull String chatUserId) {
        return UriComponentsBuilder
                .fromUriString(synapseURL)
                .path("/_synapse/admin/v1/deactivate/" + chatUserId)
                .build()
                .toUriString();
    }

    private String getReactivateURL(@Nonnull String chatUserId) {
        return UriComponentsBuilder
                .fromUriString(synapseURL)
                .path("/_synapse/admin/v2/users/" + chatUserId)
                .build()
                .toUriString();
    }

    private String getUserTokenURL(@Nonnull String chatUserId) {
        return UriComponentsBuilder
                .fromUriString(synapseURL)
                .path("/_synapse/admin/v1/users/" + chatUserId + "/login")
                .build()
                .toUriString();
    }
}
