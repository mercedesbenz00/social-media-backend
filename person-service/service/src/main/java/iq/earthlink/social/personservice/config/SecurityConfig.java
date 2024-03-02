package iq.earthlink.social.personservice.config;

import iq.earthlink.social.personservice.security.JwtAuthenticationFilter;
import iq.earthlink.social.personservice.security.SecurityProvider;
import iq.earthlink.social.util.LocalizationUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import static org.springframework.security.config.http.SessionCreationPolicy.STATELESS;

@EnableWebSecurity
@EnableMethodSecurity(proxyTargetClass = true)
@RequiredArgsConstructor
@Configuration
public class SecurityConfig {

    @Bean
    protected SecurityFilterChain filterChain(HttpSecurity http, SecurityProvider securityProvider, LocalizationUtil localizationUtil) throws Exception {
        http.sessionManagement()
                .sessionCreationPolicy(STATELESS)
                .and().cors()
                .and().csrf().disable()
                .authorizeHttpRequests()
                .requestMatchers(
                        "/internal/**",
                        "/swagger-ui/**",
                        "/actuator/**",
                        "/webjars/**",
                        "/swagger-resources/**",
                        "/v2/api-docs",
                        "/api/v1/persons/register*",
                        "/api/v1/persons/login",
                        "/api/v1/persons/login/facebook",
                        "/api/v1/persons/login/google",
                        "/api/v1/persons/login/apple",
                        "/api/v1/persons/token/refresh",
                        "/api/v1/persons/email/confirm",
                        "/api/v1/persons/email/resend",
                        "/api/v1/persons/check-username",
                        "/api/v1/persons/forgot-password",
                        "/api/v1/persons/reset-password",
                        "/_matrix-internal/identity/v1/check_credentials").permitAll()
                .requestMatchers(HttpMethod.GET,
                        "/api/v1/cities",
                        "/api/v1/persons/*/avatar",
                        "/api/v1/persons/*/cover").permitAll()
                .anyRequest()
                .authenticated()
                .and()
                .addFilterBefore(new JwtAuthenticationFilter(securityProvider, localizationUtil), UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling()
                .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED));
        return http.build();
    }

    @Bean
    public FilterRegistrationBean<CorsFilter> customCorsFilter(CORSProperties corsProperties) {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(corsProperties.getAllowCredentials());
        config.setAllowedOrigins(corsProperties.getAllowedOrigins());
        config.setAllowedMethods(corsProperties.getAllowedMethods());
        config.setAllowedHeaders(corsProperties.getAllowedHeaders());
        source.registerCorsConfiguration("/**", config);
        FilterRegistrationBean<CorsFilter> bean = new FilterRegistrationBean<>(new CorsFilter(source));
        bean.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return bean;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
