package iq.earthlink.social.personservice.security;

import iq.earthlink.social.exception.HasArguments;
import iq.earthlink.social.util.ExceptionUtil;
import iq.earthlink.social.util.LocalizationUtil;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;
import java.util.UUID;

import static java.util.Arrays.stream;
import static javax.servlet.http.HttpServletResponse.SC_UNAUTHORIZED;

@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final SecurityProvider securityProvider;
    private final LocalizationUtil localizationUtil;

    public JwtAuthenticationFilter(
            SecurityProvider securityProvider,
            LocalizationUtil localizationUtil) {
        this.securityProvider = securityProvider;
        this.localizationUtil = localizationUtil;
    }

    @Override
    protected void doFilterInternal(@NotNull HttpServletRequest request, @NotNull HttpServletResponse response, @NotNull FilterChain filterChain) throws ServletException, IOException {
        String refreshTokenUrl = "/api/v1/persons/token/refresh";
        if (refreshTokenUrl.equals(request.getRequestURI())) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = getToken(request);

        try {
            if (StringUtils.hasText(token) && securityProvider.isValidToken(token)) {
                UUID uuid = UUID.fromString(securityProvider.getSubjectFromJWT(token));
                String[] roles = securityProvider.getRolesFromJWT(token);

                Collection<SimpleGrantedAuthority> authorities = new ArrayList<>();
                stream(roles).forEach(role -> authorities.add(new SimpleGrantedAuthority(role)));
                UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(uuid, null, authorities);
                SecurityContextHolder.getContext().setAuthentication(authenticationToken);
            }
        } catch (Exception ex) {
            log.error("Error logging in: {}", ex.getMessage());
            String message = ex instanceof HasArguments hasArguments ? localizationUtil.getLocalizedMessage(ex.getMessage(), hasArguments.getArgs())
                    : localizationUtil.getLocalizedMessage(ex.getMessage());
            ExceptionUtil.writeExceptionToResponse(response, ExceptionUtil.UNAUTHORIZED_REQUEST, message, SC_UNAUTHORIZED);
        }
        filterChain.doFilter(request, response);
    }

    private String getToken(HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        if (Objects.isNull(token) || !token.startsWith("Bearer ")) {
            return null;
        }
        return token.substring(7);
    }

}
