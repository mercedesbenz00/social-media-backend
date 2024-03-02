package iq.earthlink.social.postservice.config;

import feign.RequestInterceptor;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.composite.CompositeMeterRegistry;
import iq.earthlink.social.classes.data.dto.ComplaintRequest;
import iq.earthlink.social.classes.data.dto.PersonData;
import iq.earthlink.social.common.config.I18NConfig;
import iq.earthlink.social.common.config.SwaggerConfig;
import iq.earthlink.social.common.config.SwaggerConfigCustomizer;
import iq.earthlink.social.groupservice.group.rest.MembersRestService;
import iq.earthlink.social.personservice.person.PersonInfo;
import iq.earthlink.social.personservice.person.rest.JsonPerson;
import iq.earthlink.social.postservice.post.comment.Comment;
import iq.earthlink.social.postservice.post.comment.complaint.CommentComplaint;
import iq.earthlink.social.postservice.post.complaint.model.PostComplaint;
import iq.earthlink.social.postservice.post.complaint.model.Reason;
import iq.earthlink.social.postservice.post.model.Post;
import iq.earthlink.social.postservice.post.rest.*;
import iq.earthlink.social.security.CustomAuthenticationDetails;
import org.dozer.DozerBeanMapperBuilder;
import org.dozer.Mapper;
import org.dozer.loader.api.BeanMappingBuilder;
import org.dozer.loader.api.FieldsMappingOptions;
import org.dozer.loader.api.TypeMappingOptions;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

import static org.dozer.loader.api.FieldsMappingOptions.copyByReference;
import static org.dozer.loader.api.FieldsMappingOptions.customConverter;

@Configuration
@EnableWebMvc
@EnableFeignClients(
        basePackageClasses = {
                MembersRestService.class,
        })
@Import({
        SwaggerConfig.class,
        I18NConfig.class,
        springfox.bean.validators.configuration.BeanValidatorPluginsConfiguration.class
})
@ComponentScan(basePackages = {
        "iq.earthlink.social.common.filestorage",
        "iq.earthlink.social.exception",
        "iq.earthlink.social.security",
        "iq.earthlink.social.util"
})
@EnableRabbit
@EnableAsync
public class ResourceServerConfig {

    @Bean
    public Mapper mapper(MediaFilePathConverter mediaFilePathConverter) {
        final String AVATAR = "avatar";
        return DozerBeanMapperBuilder.create()
                .withCustomConverterWithId("mediaFilePathConverter", mediaFilePathConverter)
                .withMappingBuilder(new BeanMappingBuilder() {
                    @Override
                    protected void configure() {
                        mapping(Comment.class, JsonComment.class)
                                .fields("post.id", "postId")
                                .fields("replyTo.commentUuid", "replyTo", copyByReference())
                                .fields("post.userGroupId", "userGroupId");

                        mapping(CommentComplaint.class, JsonCommentComplaint.class)
                                .fields("comment.id", "commentId");

                        mapping(PostComplaint.class, JsonPostComplaint.class)
                                .fields("post.id", "postId");

                        mapping(Reason.class, JsonReasonWithLocalization.class, TypeMappingOptions.oneWay())
                                .fields("localizations", "localizations", customConverter(ReasonLocalizationConverter.class));

                        mapping(PersonInfo.class, PersonData.class, TypeMappingOptions.oneWay())
                                .fields(AVATAR, AVATAR, FieldsMappingOptions.customConverterId("mediaFilePathConverter"));

                        mapping(JsonPerson.class, PersonData.class, TypeMappingOptions.oneWay())
                                .fields(AVATAR, AVATAR, customConverter(JsonMediaFilePathConverter.class));

                        mapping(Post.class, JsonPost.class).fields("state.displayName", "stateDisplayName");

                        mapping(JsonPostComplaintData.class, ComplaintRequest.class)
                                .fields("reason.id", "reasonId")
                                .fields("reasonOther", "reason");

                        mapping(JsonCommentComplaintData.class, ComplaintRequest.class)
                                .fields("reason.id", "reasonId")
                                .fields("reasonOther", "reason");
                    }
                }).build();
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
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

    @Bean
    public InternalResourceViewResolver defaultViewResolver() {
        return new InternalResourceViewResolver();
    }

    @Bean
    public SwaggerConfigCustomizer swaggerConfigCustomizer() {
        return docket -> docket.ignoredParameterTypes(PersonInfo.class);
    }

    @Bean
    public WebClient getWebClientInstance() {
        return WebClient.create();
    }

    @Bean
    public MeterRegistry getMeterRegistry() {
        return new CompositeMeterRegistry();
    }
}
