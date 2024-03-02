package iq.earthlink.social.security;

import org.springframework.security.web.authentication.WebAuthenticationDetails;

import javax.servlet.http.HttpServletRequest;

public class CustomAuthenticationDetails extends WebAuthenticationDetails {

    private String bearerToken;

    public CustomAuthenticationDetails(HttpServletRequest request) {
        super(request);
        this.bearerToken = request.getHeader("Authorization");
    }

    public String getBearerToken() {
        return bearerToken;
    }

    public void setBearerToken(String bearerToken) {
        this.bearerToken = bearerToken;
    }
}