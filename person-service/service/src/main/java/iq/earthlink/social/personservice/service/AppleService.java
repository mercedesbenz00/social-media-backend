package iq.earthlink.social.personservice.service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import iq.earthlink.social.exception.BadRequestException;
import iq.earthlink.social.exception.ForbiddenException;
import iq.earthlink.social.exception.RestApiException;
import iq.earthlink.social.personservice.authentication.dto.SSOUserModel;
import iq.earthlink.social.personservice.authentication.enumeration.ProviderName;
import iq.earthlink.social.personservice.data.dto.AppleIDTokenPayload;
import iq.earthlink.social.personservice.data.dto.AppleResponse;
import iq.earthlink.social.personservice.person.rest.JsonSSORequest;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.security.PrivateKey;
import java.security.interfaces.ECPrivateKey;
import java.time.Instant;
import java.util.*;

@Service
public class AppleService {
    private static final Logger LOGGER = LoggerFactory.getLogger(AppleService.class);

    @Value("${social.authentication.appleKeyId}")
    private String APPLE_KEY_ID;

    @Value("${social.authentication.appleTeamId}")
    private String APPLE_TEAM_ID;

    @Value("${social.authentication.appleURI}")
    private String APPLE_API_URI;

    private Environment env;

    private final WebClient webClient;

    public AppleService(Environment env, WebClient webClient) {
        this.env = env;
        this.webClient = webClient;
    }


    public SSOUserModel getUserDetails(JsonSSORequest data) {
        SSOUserModel userModel = new SSOUserModel();
        MultiValueMap<String, String> formData = generateFormData(data);
        String tokenVerificationUrl = getTokenVerificationUrl();

        AppleResponse appleResponse = webClient
                .post()
                .uri(tokenVerificationUrl)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData(formData))
                .retrieve()
                .onStatus(HttpStatus::isError, clientResponse ->
                        clientResponse.bodyToMono(String.class) // error body as String or other class
                                .flatMap(error -> {
                                    JsonObject response = (JsonObject) JsonParser.parseString(error);
                                    String errorMessage = response.get("error").getAsString();
                                    String errorDescription = response.get("error_description").getAsString();
                                    List<String> errors = Collections.singletonList(errorDescription);
                                    return Mono.error(new BadRequestException("error.apple.request.".concat(errorMessage), errors, new Object()));
                                })
                )
                .bodyToMono(AppleResponse.class)
                .block();

        if (appleResponse != null) {
            AppleIDTokenPayload idTokenPayload = parseIdToken(appleResponse.getIdToken());
            userModel.setId(idTokenPayload.getSub());
            userModel.setProviderName(ProviderName.APPLE);

            if (Boolean.TRUE.equals(idTokenPayload.getEmailVerified()))
                userModel.setEmail(idTokenPayload.getEmail());
            else {
                LOGGER.error("Could not login via Apple: Email is not verified");
                throw new ForbiddenException("error.apple.email.not.verified");
            }

            //At first user authentication UI/Mobile should retrieve first name and last name in callback and UI should pass it to the backend
            if (data.getFirstName() != null) {
                userModel.setFirstName(data.getFirstName());
            }
            if (data.getLastName() != null) {
                userModel.setLastName(data.getLastName());
            }
        }

        return userModel;
    }


    public MultiValueMap<String, String> generateFormData(JsonSSORequest data) {
        String token;
        try {
            token = generateClientSecret(data.getClientId());
        } catch (IOException ex) {
            LOGGER.error("Error while generating apple client secret with clientId {}. Error details: {}", data.getClientId(), ex.getMessage());
            throw new RestApiException(HttpStatus.INTERNAL_SERVER_ERROR, "error.apple.client.secret");
        }

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("client_id", data.getClientId());
        formData.add("client_secret", token);
        formData.add("grant_type", "authorization_code");
        formData.add("code", data.getAccessToken());

        return formData;
    }

    public AppleIDTokenPayload parseIdToken(String idToken) {
        String payload = idToken.split("\\.")[1];// 0 is header we ignore it for now
        String decoded = new String(Base64.getDecoder().decode(payload));

        return new Gson().fromJson(decoded, AppleIDTokenPayload.class);
    }

    public String generateClientSecret(String clientId) throws IOException {
        PrivateKey pKey = generatePrivateKey();

        Algorithm algorithm = Algorithm.ECDSA256(null, (ECPrivateKey) pKey);

        Map<String, Object> header = new HashMap<>();
        header.put("KEY_ID", APPLE_KEY_ID);

        return JWT.create()
                .withHeader(header)
                .withIssuer(APPLE_TEAM_ID)
                .withAudience(APPLE_API_URI)
                .withSubject(clientId)
                .withIssuedAt(Date.from(Instant.now()))
                .withExpiresAt(new Date(System.currentTimeMillis() + (1000 * 60 * 5)))
                .sign(algorithm);
    }


    private PrivateKey generatePrivateKey() throws IOException {
        String[] activeProfiles = env.getActiveProfiles();
        Resource certificate;
        if (Arrays.asList(activeProfiles).contains("production")) {
            certificate = new ClassPathResource("apple/cert_prod.p8");
        } else {
            certificate = new ClassPathResource("apple/cert.p8");
        }
        Reader reader = new InputStreamReader(certificate.getInputStream());
        final PEMParser pemParser = new PEMParser(reader);
        final JcaPEMKeyConverter converter = new JcaPEMKeyConverter();
        final PrivateKeyInfo object = (PrivateKeyInfo) pemParser.readObject();
        final PrivateKey pKey = converter.getPrivateKey(object);
        pemParser.close();
        return pKey;
    }

    private String getTokenVerificationUrl() {
        return UriComponentsBuilder
                .fromUriString(APPLE_API_URI)
                .path("/auth/token")
                .build()
                .toUriString();
    }
}
