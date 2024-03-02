package iq.earthlink.social.shortvideoregistryservice.config;

import iq.earthlink.social.common.config.SwaggerConfig;
import iq.earthlink.social.common.config.SwaggerConfigCustomizer;
import iq.earthlink.social.personservice.person.PersonInfo;
import iq.earthlink.social.shortvideoregistryservice.dto.CreateShortVideoMessageDTO;
import iq.earthlink.social.shortvideoregistryservice.dto.ShortVideoCategoryDTO;
import iq.earthlink.social.shortvideoregistryservice.model.Category;
import iq.earthlink.social.shortvideoregistryservice.model.ShortVideo;
import org.dozer.DozerBeanMapperBuilder;
import org.dozer.Mapper;
import org.dozer.loader.api.BeanMappingBuilder;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.context.annotation.*;
import org.springframework.data.cassandra.core.query.CassandraPageRequest;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.view.InternalResourceViewResolver;
import springfox.documentation.service.ApiInfo;

import java.util.ArrayList;

import static springfox.documentation.service.ApiInfo.DEFAULT_CONTACT;

@Profile("!test")
@Configuration
@EnableWebMvc
@Import({
        SwaggerConfig.class,
        springfox.bean.validators.configuration.BeanValidatorPluginsConfiguration.class
})
@ComponentScan(basePackages = {
        "iq.earthlink.social.common.rest",
        "iq.earthlink.social.common.filestorage",
        "iq.earthlink.social.security",
        "iq.earthlink.social.common.util"
})
@EnableRabbit
public class ResourceServerConfig{

    @Bean
    public Mapper mapper() {
        return DozerBeanMapperBuilder
                .create()
                .withMappingBuilder(new BeanMappingBuilder() {
                    @Override
                    protected void configure() {
                        mapping(CreateShortVideoMessageDTO.class,
                                ShortVideo.class)
                                .exclude("categories")
                                .exclude("friends");
                        mapping(Category.class,
                                ShortVideoCategoryDTO.class)
                                .fields("id", "categoryId");
                    }
                })
                .build();
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
                "Short Video Registry Service API",
                "Services to support metadata of short videos",
                "0.0.1",
                "https://earthlinktele.com",
                DEFAULT_CONTACT,
                "Proprietary",
                "https://earthlinktele.com",
                new ArrayList<>());

        return docket -> {
            docket
                    .apiInfo(apiInfo)
                    .ignoredParameterTypes(PersonInfo.class)
                    .ignoredParameterTypes(CassandraPageRequest.class);
            return docket;
        };
    }
}
