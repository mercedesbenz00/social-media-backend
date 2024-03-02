package iq.earthlink.social.shortvideousagestatsservice.config;

import iq.earthlink.social.util.ExceptionUtil;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.provider.error.OAuth2AuthenticationEntryPoint;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static javax.servlet.http.HttpServletResponse.SC_UNAUTHORIZED;

public class CustomOauth2AuthenticationEntryPoint extends OAuth2AuthenticationEntryPoint {

    public static final String AUTH_ERROR_PREFIX = "Invalid Token: ";

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException, ServletException {
        if (authException instanceof InsufficientAuthenticationException) {
            String message = AUTH_ERROR_PREFIX + authException.getMessage();
            ExceptionUtil.writeExceptionToResponse(response, ExceptionUtil.UNAUTHORIZED_REQUEST, message, SC_UNAUTHORIZED);
        } else {
            super.commence(request, response, authException);
        }
    }

}
