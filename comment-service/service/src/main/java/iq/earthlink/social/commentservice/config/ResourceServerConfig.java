package iq.earthlink.social.commentservice.config;

import com.google.common.collect.Sets;
import feign.RequestInterceptor;
import iq.earthlink.social.commentservice.dto.JsonComment;
import iq.earthlink.social.common.config.SwaggerConfig;
import iq.earthlink.social.common.config.SwaggerConfigCustomizer;
import iq.earthlink.social.common.data.model.CommentEntity;
import iq.earthlink.social.personservice.person.PersonInfo;
import iq.earthlink.social.personservice.person.rest.JsonPersonProfile;
import iq.earthlink.social.security.CustomAuthenticationDetails;
import org.dozer.DozerBeanMapperBuilder;
import org.dozer.Mapper;
import org.dozer.loader.api.BeanMappingBuilder;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.boot.autoconfigure.security.oauth2.resource.PrincipalExtractor;
import org.springframework.boot.autoconfigure.security.oauth2.resource.UserInfoTokenServices;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.RequestContextListener;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

import java.util.List;

@Configuration
@EnableWebMvc
@Import({
        SwaggerConfig.class,
        springfox.bean.validators.configuration.BeanValidatorPluginsConfiguration.class
})

@ComponentScan(basePackages = {
        "iq.earthlink.social.common.rest",
        "iq.earthlink.social.security",
        "iq.earthlink.social.common.util"
})
@EnableRabbit
public class ResourceServerConfig {
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

     /**
     * This bean required by {@link UserInfoTokenServices}.
     */
    @Bean
    public RequestContextListener requestContextListener() {
        return new RequestContextListener();
    }

    /**
     * This bean responsible for extracting {@link java.security.Principal} data.
     * Returned principal can be used as parameter in controller
     * with {@link org.springframework.security.core.annotation.AuthenticationPrincipal} annotation.
     */
    @Bean
    @SuppressWarnings("unchecked")
    public PrincipalExtractor principalExtractor() {
        return map -> JsonPersonProfile.builder()
                .id((long) (int) map.get("id"))
                .email((String) map.get("email"))
                .firstName((String) map.get("firstName"))
                .lastName((String) map.get("lastName"))
                .roles(Sets.newHashSet((List<String>) map.get("roles")))
                .displayName((String) map.get("displayName"))
                .username((String) map.get("username"))
                .build();
    }

    @Bean
    public InternalResourceViewResolver defaultViewResolver() {
        return new InternalResourceViewResolver();
    }

    @Bean
    public SwaggerConfigCustomizer swaggerConfigCustomizer() {
        return docket -> docket.ignoredParameterTypes(PersonInfo.class);
    }

    @Bean
    public Mapper mapper() {
        return DozerBeanMapperBuilder.create().withMappingBuilder(new BeanMappingBuilder() {
            @Override
            protected void configure() {
                mapping(CommentEntity.class, JsonComment.class).fields("replyTo.id", "replyTo");
            }
        }).build();
    }

    /**
     * The Feign client interceptor.
     */
    @Bean
    public RequestInterceptor jwtTokenInterceptor() {
        return template -> {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            if (authentication != null && authentication.getDetails() instanceof CustomAuthenticationDetails details) {
                template.header(HttpHeaders.AUTHORIZATION, details.getBearerToken());
            }
        };
    }
}

