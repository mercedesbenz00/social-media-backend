package iq.earthlink.social.shortvideoservice.config;

import com.fasterxml.classmate.TypeResolver;
import iq.earthlink.social.commentservice.dto.JsonComment;
import iq.earthlink.social.commentservice.dto.JsonCommentData;
import iq.earthlink.social.commentservice.rest.CommentRestService;
import iq.earthlink.social.common.config.SwaggerConfig;
import iq.earthlink.social.common.config.SwaggerConfigCustomizer;
import iq.earthlink.social.personservice.person.PersonInfo;
import iq.earthlink.social.personservice.rest.FollowingRestService;
import iq.earthlink.social.shortvideoregistryservice.rest.ShortVideoRegistryRestService;
import iq.earthlink.social.shortvideoservice.model.ErrorDTO;
import iq.earthlink.social.shortvideoservice.model.ShortVideoCommentDTO;
import org.dozer.DozerBeanMapperBuilder;
import org.dozer.Mapper;
import org.dozer.loader.api.BeanMappingBuilder;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.view.InternalResourceViewResolver;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;

import java.util.ArrayList;

import static springfox.documentation.service.ApiInfo.DEFAULT_CONTACT;

@Configuration
@EnableWebMvc
@Import({
        SwaggerConfig.class,
        springfox.bean.validators.configuration.BeanValidatorPluginsConfiguration.class
})
@EnableFeignClients(
        basePackageClasses = {
                FollowingRestService.class,
                ShortVideoRegistryRestService.class,
                CommentRestService.class
        })
@ComponentScan(basePackages = {
        "iq.earthlink.social.common.rest",
        "iq.earthlink.social.common.filestorage",
        "iq.earthlink.social.security",
        "iq.earthlink.social.common.util"
})
public class ResourceServerConfig{


    private final TypeResolver resolver;

    public ResourceServerConfig(TypeResolver resolver) {
        this.resolver = resolver;
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Bean
    public InternalResourceViewResolver defaultViewResolver() {
        return new InternalResourceViewResolver();
    }

    @Bean
    public SwaggerConfigCustomizer swaggerConfigCustomizer() {
        ApiInfo apiInfo = new ApiInfo(
                "Short Video Service API",
                "Services to support short videos",
                "0.0.1",
                "https://earthlinktele.com",
                DEFAULT_CONTACT,
                "Proprietary",
                "https://earthlinktele.com",
                new ArrayList<>());

        return docket -> {
            docket
                    .apiInfo(apiInfo)
                    .additionalModels(resolver.resolve(ErrorDTO.class))
                    .ignoredParameterTypes(PersonInfo.class);
            docket.select()
                    .apis(RequestHandlerSelectors.basePackage("iq.earthlink.social.shortvideoservice"))
                    .build();
            return docket;
        };
    }

    @Bean
    public Mapper mapper() {
        return DozerBeanMapperBuilder
                .create()
                .withMappingBuilder(new BeanMappingBuilder() {
                    @Override
                    protected void configure() {
                        mapping(ShortVideoCommentDTO.class,
                                JsonCommentData.class)
                                .fields("videoId", "objectId");
                        mapping(JsonComment.class,
                                ShortVideoCommentDTO.class)
                                .fields("objectId", "videoId");
                    }
                })
                .build();
    }
}
