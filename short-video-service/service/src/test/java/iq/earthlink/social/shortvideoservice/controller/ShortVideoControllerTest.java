package iq.earthlink.social.shortvideoservice.controller;

import iq.earthlink.social.personservice.person.PersonInfo;
import iq.earthlink.social.personservice.person.rest.JsonPersonProfile;
import iq.earthlink.social.shortvideoservice.model.CreateShortVideoDTO;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Disabled
class ShortVideoControllerTest {

    private MockMvc mockMvc;
    private CreateShortVideoDTO shortVideo;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @BeforeEach
    public void setup() {
        initMockMvc();
        initPrincipal();
        initShortVideo();
    }

    @Test
    void addShortVideoReturnsHttpStatusOk() throws Exception {
        // Send 'Add short video' post request:
        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/short-video")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getJsonString(shortVideo)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.presignedUrl").isNotEmpty());
    }

    @Test
    void addShortVideoReturnsHttpStatusInvalidInput() throws Exception {
        // Send 'Add short video' post request without body:
        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/short-video")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is4xxClientError());
    }

    private void initShortVideo() {
        shortVideo = new CreateShortVideoDTO();
        shortVideo.setTitle("Title");
    }

    private void initMockMvc() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    private void initPrincipal() {
        PersonInfo applicationUser = JsonPersonProfile.builder().username("admin@creativeadvtech.com").build();
        Authentication authentication = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
        when(authentication.getPrincipal()).thenReturn(applicationUser);
    }

    private String getJsonString(CreateShortVideoDTO shortVideo) throws JSONException {
        JSONObject joShortVideo = new JSONObject();
        joShortVideo.put("title", shortVideo.getTitle());
        return joShortVideo.toString();
    }
}
