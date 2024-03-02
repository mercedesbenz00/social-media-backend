package iq.earthlink.social.common.config;

import com.google.common.collect.Lists;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.autoconfigure.endpoint.web.CorsEndpointProperties;
import org.springframework.boot.actuate.autoconfigure.endpoint.web.WebEndpointProperties;
import org.springframework.boot.actuate.autoconfigure.web.server.ManagementPortType;
import org.springframework.boot.actuate.endpoint.ExposableEndpoint;
import org.springframework.boot.actuate.endpoint.web.*;
import org.springframework.boot.actuate.endpoint.web.annotation.ControllerEndpointsSupplier;
import org.springframework.boot.actuate.endpoint.web.annotation.ServletEndpointsSupplier;
import org.springframework.boot.actuate.endpoint.web.servlet.WebMvcEndpointHandlerMapping;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RestController;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.builders.ResponseBuilder;
import springfox.documentation.service.*;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spi.service.contexts.SecurityContext;
import springfox.documentation.spring.web.plugins.Docket;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.*;

/**
 * Common configuration for services which uses Swagger
 */
@Configuration
public class SwaggerConfig {

    @Bean
    public Docket api(ObjectProvider<SwaggerConfigCustomizer> customizers,
                      @Value("${person.service.url:http://localhost:8081}") String authHost) {
        Contact contact = new Contact(
                "Creative Advanced Technologies",
                "https://www.creativeadvtech.com",
                "info@creativeadvtech.com");

        ApiInfo apiInfo = new ApiInfo(
                "Backend Social Network API",
                "Backend Social Network API",
                "0.0.1",
                "https://earthlinktele.com",
                contact,
                "Proprietary",
                "https://earthlinktele.com",
                new ArrayList<>());

        Docket docket = new Docket(DocumentationType.SWAGGER_2)
                .apiInfo(apiInfo)
                .pathMapping("/")
                .apiInfo(ApiInfo.DEFAULT)
                .forCodeGeneration(true)
                .genericModelSubstitutes(ResponseEntity.class)
                .ignoredParameterTypes(Pageable.class)
                .ignoredParameterTypes(java.sql.Date.class)
                .directModelSubstitute(LocalDate.class, java.sql.Date.class)
                .directModelSubstitute(ZonedDateTime.class, Date.class)
                .directModelSubstitute(LocalDateTime.class, Date.class)
                .useDefaultResponseMessages(false)
                .globalResponses(HttpMethod.GET, defaultGetRequestResponses())
                .globalResponses(HttpMethod.POST, defaultGetRequestResponses())
                .securityContexts(Lists.newArrayList(securityContext()))
                .securitySchemes(Collections.singletonList(securitySchema()));

        docket
                .select()
                .apis(RequestHandlerSelectors.withClassAnnotation(RestController.class))
                .paths(PathSelectors.any())
                .build()
                .tags(new Tag("Ban Api", "The Ban API allows persons manage bans against other persons"),
                        new Tag("Block Api", "The Block API allows persons manage blocks against other persons"),
                        new Tag("City Api", "The City API manages available cities"),
                        new Tag("Complaint Api", "The Complaint API allows persons manage complaints against other persons"),
                        new Tag("Follow Api", "The Follow API allows manage following relationship between persons"),
                        new Tag("Mute Api", "The Mute API allows persons manage mutes against other persons"),
                        new Tag("Notifications Settings Api", "The Notifications Settings API allows manage following notifications settings between persons"),
                        new Tag("Person Api", "The API responsible for the persons information management"),
                        new Tag("Person Media Api", "The Person Media API allows update/retrieve person media information (e.g. avatar, cover)"),
                        new Tag("Person Token Api", "The Person Token API allows managing person tokens"),

                        new Tag("Category Api", "The Category API allows manage global/person categories of the interests"),
                        new Tag("Category Media Api", "The Category Media API allows to update/retrieve category media information (e.g. avatar)"),
                        new Tag("Group Api", "The Group API allows manage the persons groups"),
                        new Tag("Group Media Api", "The Group Media API allows to update/retrieve group media information (e.g. avatar)"),
                        new Tag("Group Notification Settings Api", "The Group Notification Settings Api allows to mute and unmute notifications for a certain group"),

                        new Tag("Notification Api", "The Notification API allows persons to get notifications"),

                        new Tag("Comment Api", "The Comment API allows persons comment the posts"),
                        new Tag("Comment Complaint Api", "The Comment Complaint API allows persons to complain the posts' comments"),
                        new Tag("Comment Vote Api", "The Comment Vote API allows to upvote or downvote the post comments"),
                        new Tag("Group Post Collection Api", "The Group Post Collection API allows manage post collections for the person groups"),
                        new Tag("Post Api", "The Post API allows persons manage their or moderated posts"),
                        new Tag("Post Collection Api", "The Post Collection API allows persons manage their personal post collections"),
                        new Tag("Post Complaint Api", "The Post Complaint API allows manage complaints against the posts"),
                        new Tag("Post Notification Settings Api", "The Post Notification Settings Api allows to turn off/on notifications for a certain post"),
                        new Tag("Post Vote Api", "The Post Vote API allows to upvote or downvote the post"),
                        new Tag("Complaint Reason Api", "The Complaint Reason API allows admin user to manage post complaints"),
                        new Tag("Link Preview Api", "The Link Preview Api allows to get link preview information"),

                        new Tag("Comments Api", "The Comments API allows persons to comment anything"),
                        new Tag("Short Video Usage Stats API", "Short Video Usage Stats API allows to log user activities while watching the short video"));

        customizers.ifAvailable(swaggerConfigCustomizer -> swaggerConfigCustomizer.customize(docket));

        return docket;
    }

    private SecurityContext securityContext() {
        return SecurityContext.builder()
                .securityReferences(defaultAuth())
                .build();
    }

    private List<Response> defaultGetRequestResponses() {
        return Arrays.asList(
                new ResponseBuilder().code("400").description("Invalid data provided").build(),
                new ResponseBuilder().code("401").description("User is not authorized").build(),
                new ResponseBuilder().code("403").description("User has not enough permissions").build(),
                new ResponseBuilder().code("404").description("The resource is not found").build(),
                new ResponseBuilder().code("500").description("If any server error occurred").build()
        );
    }

    private SecurityScheme securitySchema() {
        return new ApiKey("JWT", "Authorization", "header");
    }

    private List<SecurityReference> defaultAuth() {
        AuthorizationScope authorizationScope = new AuthorizationScope("global", "accessEverything");
        AuthorizationScope[] authorizationScopes = new AuthorizationScope[1];
        authorizationScopes[0] = authorizationScope;
        return List.of(new SecurityReference("JWT", authorizationScopes));
    }


    @Bean
    public WebMvcEndpointHandlerMapping webEndpointServletHandlerMapping(WebEndpointsSupplier webEndpointsSupplier,
                                                                         ServletEndpointsSupplier servletEndpointsSupplier, ControllerEndpointsSupplier controllerEndpointsSupplier,
                                                                         EndpointMediaTypes endpointMediaTypes, CorsEndpointProperties corsProperties,
                                                                         WebEndpointProperties webEndpointProperties, Environment environment) {
        List<ExposableEndpoint<?>> allEndpoints = new ArrayList<>();
        Collection<ExposableWebEndpoint> webEndpoints = webEndpointsSupplier.getEndpoints();
        allEndpoints.addAll(webEndpoints);
        allEndpoints.addAll(servletEndpointsSupplier.getEndpoints());
        allEndpoints.addAll(controllerEndpointsSupplier.getEndpoints());
        String basePath = webEndpointProperties.getBasePath();
        EndpointMapping endpointMapping = new EndpointMapping(basePath);
        boolean shouldRegisterLinksMapping = this.shouldRegisterLinksMapping(webEndpointProperties, environment,
                basePath);
        return new WebMvcEndpointHandlerMapping(endpointMapping, webEndpoints, endpointMediaTypes,
                corsProperties.toCorsConfiguration(), new EndpointLinksResolver(allEndpoints, basePath),
                shouldRegisterLinksMapping, null);
    }

    private boolean shouldRegisterLinksMapping(WebEndpointProperties webEndpointProperties, Environment environment,
                                               String basePath) {
        return webEndpointProperties.getDiscovery().isEnabled() && (StringUtils.hasText(basePath)
                || ManagementPortType.get(environment).equals(ManagementPortType.DIFFERENT));
    }
}
