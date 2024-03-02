package iq.earthlink.social.notificationservice.config;

import iq.earthlink.social.classes.data.dto.PersonData;
import iq.earthlink.social.common.config.SwaggerConfig;
import iq.earthlink.social.common.config.SwaggerConfigCustomizer;
import iq.earthlink.social.personservice.person.PersonInfo;
import iq.earthlink.social.personservice.person.rest.JsonPerson;
import org.dozer.DozerBeanMapper;
import org.dozer.Mapper;
import org.dozer.loader.api.BeanMappingBuilder;
import org.dozer.loader.api.TypeMappingOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

import static org.dozer.loader.api.FieldsMappingOptions.customConverter;

@Configuration
@EnableWebMvc
@Import({
        SwaggerConfig.class,
        springfox.bean.validators.configuration.BeanValidatorPluginsConfiguration.class
})
@ComponentScan(basePackages = {
        "iq.earthlink.social.exception",
        "iq.earthlink.social.security",
        "iq.earthlink.social.util"
})
public class ResourceServerConfig {

    @Bean
    public Mapper mapper() {
        DozerBeanMapper mapper = new DozerBeanMapper();
        BeanMappingBuilder mappingBuilder = new BeanMappingBuilder() {
            @Override
            protected void configure() {
                mapping(JsonPerson.class, PersonData.class, TypeMappingOptions.oneWay())
                        .fields("avatar", "avatar", customConverter(JsonMediaFilePathConverter.class));
            }
        };
        mapper.addMapping(mappingBuilder);
        return mapper;
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
        return docket -> docket.ignoredParameterTypes(PersonInfo.class);
    }
}
