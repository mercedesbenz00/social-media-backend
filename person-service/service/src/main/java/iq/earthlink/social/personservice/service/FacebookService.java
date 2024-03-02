package iq.earthlink.social.personservice.service;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import iq.earthlink.social.exception.BadRequestException;
import iq.earthlink.social.personservice.authentication.dto.SSOUserModel;
import iq.earthlink.social.personservice.authentication.enumeration.ProviderName;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;
import java.util.Objects;


@Service
public class FacebookService {


    private final WebClient webClient;

    @Value("${social.authentication.facebookURI}")
    private String FACEBOOK_API_URI;

    public FacebookService(WebClient webClient) {
        this.webClient = webClient;
    }

    public SSOUserModel getUserDetails(String accessToken, String fields) {

        String facebookAPIUrl = getFacebookAPIUrl(accessToken, fields);

        SSOUserModel ssoUserModel = webClient
                .get()
                .uri(facebookAPIUrl).retrieve()
                .onStatus(HttpStatus::isError, clientResponse ->
                        clientResponse.bodyToMono(String.class) // error body as String or other class
                                .flatMap(error -> {
                                    JsonObject response = (JsonObject) JsonParser.parseString(error);
                                    JsonObject errorElement = (JsonObject) response.get("error");
                                    List<String> errors = Collections.singletonList(errorElement.get("message").getAsString());
                                    String code = errorElement.get("code").getAsString();
                                    return Mono.error(new BadRequestException("error.facebook.request.".concat(code), errors, new Object()));
                                })
                )
                .bodyToMono(SSOUserModel.class)
                .block();

        if (Objects.nonNull(ssoUserModel))
            ssoUserModel.setProviderName(ProviderName.FACEBOOK);

        return ssoUserModel;

    }

    private String getFacebookAPIUrl(@Nonnull String accessToken, String fields) {
        return UriComponentsBuilder
                .fromUriString(FACEBOOK_API_URI)
                .path("/v4.0/me")
                .queryParam("access_token", accessToken)
                .queryParam("fields", fields)
                .build()
                .toUriString();
    }
}
