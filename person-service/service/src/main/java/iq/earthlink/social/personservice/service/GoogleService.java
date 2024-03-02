package iq.earthlink.social.personservice.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.apache.v2.ApacheHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import iq.earthlink.social.exception.ForbiddenException;
import iq.earthlink.social.personservice.authentication.dto.SSOUserModel;
import iq.earthlink.social.personservice.authentication.enumeration.ProviderName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.Arrays;

@Service
public class GoogleService {
    private static final Logger LOGGER = LoggerFactory.getLogger(GoogleService.class);

    @Value("${social.authentication.google.clientIds}")
    private String[] clientIds;
    private GoogleIdTokenVerifier verifier;

    @PostConstruct
    public void init() {
        HttpTransport httpTransport = new ApacheHttpTransport();
        GsonFactory gsonFactory = GsonFactory.getDefaultInstance();
        verifier = new GoogleIdTokenVerifier.Builder(httpTransport, gsonFactory)
                .setAudience(Arrays.asList(clientIds))
                .build();
    }

    public SSOUserModel getUserDetails(String accessToken) {
        GoogleIdToken idToken;
        try {
            idToken = verifier.verify(accessToken);
        } catch (Exception e) {
            LOGGER.error("Failed to verify the google access token", e);
            throw new ForbiddenException("error.google.invalid.access.token");
        }

        if (idToken != null) {
            GoogleIdToken.Payload payload = idToken.getPayload();

            String email = payload.getEmail();
            boolean emailVerified = payload.getEmailVerified();
            if (!emailVerified) {
                throw new ForbiddenException("error.google.email.not.verified");
            }

            String familyName = (String) payload.get("family_name");
            String givenName = (String) payload.get("given_name");
            String userId = (String) payload.get("sub");
            SSOUserModel userModel = new SSOUserModel();
            userModel.setEmail(email);
            userModel.setFirstName(givenName);
            userModel.setLastName(familyName);
            userModel.setProviderName(ProviderName.GOOGLE);
            userModel.setId(userId);
            return userModel;

        } else {
            LOGGER.info("Login via Google failed: invalid ID token");
            throw new ForbiddenException("error.google.invalid.id.token");
        }
    }
}
