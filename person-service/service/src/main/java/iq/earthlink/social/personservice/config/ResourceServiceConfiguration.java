package iq.earthlink.social.personservice.config;

import iq.earthlink.social.classes.data.dto.PersonData;
import iq.earthlink.social.common.config.SwaggerConfig;
import iq.earthlink.social.common.config.SwaggerConfigCustomizer;
import iq.earthlink.social.personservice.dto.JsonPersonReported;
import iq.earthlink.social.personservice.person.PersonInfo;
import iq.earthlink.social.personservice.person.model.Person;
import iq.earthlink.social.personservice.person.model.ReportedPerson;
import iq.earthlink.social.personservice.person.rest.JsonPerson;
import iq.earthlink.social.personservice.person.rest.JsonPersonProfile;
import iq.earthlink.social.postservice.rest.PostRestService;
import iq.earthlink.social.postservice.story.rest.StoryRestService;
import org.dozer.DozerBeanMapperBuilder;
import org.dozer.Mapper;
import org.dozer.loader.api.BeanMappingBuilder;
import org.dozer.loader.api.FieldsMappingOptions;
import org.dozer.loader.api.TypeMappingOptions;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.servlet.view.InternalResourceViewResolver;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

@Configuration
@EnableSpringDataWebSupport
@EnableFeignClients(clients = {
        PostRestService.class,
        StoryRestService.class
})
@EnableSwagger2
@ComponentScan(basePackages = {
        "iq.earthlink.social.personservice",
        "iq.earthlink.social.common.filestorage",
        "iq.earthlink.social.exception",
        "iq.earthlink.social.util"
})
@Import({SwaggerConfig.class})
public class ResourceServiceConfiguration {
    @Bean
    public Mapper mapper(MediaFilePathConverter mediaFilePathConverter) {
        String mediaFilePathConverterStr = "mediaFilePathConverter";
        String avatar = "avatar";
        String cover = "cover";
        String verifiedAccount = "verifiedAccount";
        String isVerifiedAccount = "isVerifiedAccount";

        return DozerBeanMapperBuilder.create()
                .withCustomConverterWithId(mediaFilePathConverterStr, mediaFilePathConverter)
                .withMappingBuilder(new BeanMappingBuilder() {
                    @Override
                    protected void configure() {
                        mapping(Person.class, JsonPersonProfile.class, TypeMappingOptions.oneWay())
                                .fields(cover, cover, FieldsMappingOptions.customConverterId(mediaFilePathConverterStr))
                                .fields(avatar, avatar, FieldsMappingOptions.customConverterId(mediaFilePathConverterStr))
                                .fields(verifiedAccount, isVerifiedAccount)
                                .fields("registrationCompleted", "isRegistrationCompleted");
                        mapping(Person.class, JsonPerson.class, TypeMappingOptions.oneWay())
                                .fields(cover, cover, FieldsMappingOptions.customConverterId(mediaFilePathConverterStr))
                                .fields(avatar, avatar, FieldsMappingOptions.customConverterId(mediaFilePathConverterStr))
                                .fields(verifiedAccount, isVerifiedAccount);
                        mapping(Person.class, PersonData.class, TypeMappingOptions.oneWay())
                                .fields(avatar, avatar, FieldsMappingOptions.customConverterId(mediaFilePathConverterStr));
                        mapping(ReportedPerson.class, JsonPersonReported.class, TypeMappingOptions.oneWay())
                                .fields(avatar, avatar, FieldsMappingOptions.customConverterId(mediaFilePathConverterStr));
                    }
                }).build();
    }

    /**
     * This view resolver required by the swagger-ui.
     */
    @Bean
    public InternalResourceViewResolver defaultViewResolver() {
        return new InternalResourceViewResolver();
    }

    @Bean
    public SwaggerConfigCustomizer swaggerConfigCustomizer() {
        return docket ->
                docket.ignoredParameterTypes(PersonInfo.class)
                        .ignoredParameterTypes(Person.class);
    }

    @Bean
    public WebClient getWebClientInstance() {
        return WebClient.create();
    }
}
