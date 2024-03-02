package iq.earthlink.social.postservice.post.linkpreview;

import com.google.gson.Gson;
import iq.earthlink.social.exception.BadRequestException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class DefaultLinkPreviewService implements LinkPreviewService {

    @Value("${social.postservice.link-preview.api-key}")
    private String apiKey;

    @Value("${social.postservice.link-preview.api-url}")
    private String apiUrl;

    @Override
    public LinkPreviewResponse getLinkPreview(@RequestParam String url) {
        String requestUrl = UriComponentsBuilder.fromUriString(apiUrl)
                .queryParam("key", apiKey)
                .queryParam("q", url)
                .toUriString();

        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> responseEntity;
        try {
            responseEntity = restTemplate.getForEntity(requestUrl, String.class);
        } catch (HttpClientErrorException ex) {
            throw new BadRequestException("LinkPreview API error", ex);
        }
        String responseString = responseEntity.getBody();

        Gson gson = new Gson();
        return gson.fromJson(responseString, LinkPreviewResponse.class);
    }
}
