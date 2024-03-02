package iq.earthlink.social.groupservice.config;

import iq.earthlink.social.classes.data.dto.PersonData;
import iq.earthlink.social.common.config.SwaggerConfig;
import iq.earthlink.social.common.config.SwaggerConfigCustomizer;
import iq.earthlink.social.groupservice.category.Category;
import iq.earthlink.social.groupservice.category.CategoryModel;
import iq.earthlink.social.groupservice.group.UserGroup;
import iq.earthlink.social.groupservice.group.dto.JsonGroupMember;
import iq.earthlink.social.groupservice.group.member.GroupMember;
import iq.earthlink.social.groupservice.group.rest.*;
import iq.earthlink.social.personservice.person.PersonInfo;
import iq.earthlink.social.personservice.person.rest.JsonPerson;
import org.dozer.DozerBeanMapperBuilder;
import org.dozer.Mapper;
import org.dozer.loader.api.BeanMappingBuilder;
import org.dozer.loader.api.FieldsMappingOptions;
import org.dozer.loader.api.TypeMappingOptions;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
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
        "iq.earthlink.social.common.filestorage",
        "iq.earthlink.social.exception",
        "iq.earthlink.social.security",
        "iq.earthlink.social.util"
})
@EnableRabbit
public class ResourceServerConfig {

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Bean
    public Mapper mapper(MediaFilePathConverter mediaFilePathConverter) {
        final String MEDIA_FILE_PATH_CONVERTER = "mediaFilePathConverter";
        final String COVER = "cover";
        final String AVATAR = "avatar";
        return DozerBeanMapperBuilder
                .create()
                .withCustomConverterWithId(MEDIA_FILE_PATH_CONVERTER, mediaFilePathConverter)
                .withMappingBuilder(
                        new BeanMappingBuilder() {
                            @Override
                            protected void configure() {
                                mapping(UserGroup.class, UserGroupDto.class, TypeMappingOptions.oneWay())
                                        .fields(COVER, COVER, FieldsMappingOptions.customConverterId(MEDIA_FILE_PATH_CONVERTER))
                                        .fields(AVATAR, AVATAR, FieldsMappingOptions.customConverterId(MEDIA_FILE_PATH_CONVERTER));
                                mapping(UserGroup.class, UserGroupPermissionDto.class, TypeMappingOptions.oneWay())
                                        .fields(AVATAR, AVATAR, FieldsMappingOptions.customConverterId(MEDIA_FILE_PATH_CONVERTER));
                                mapping(CategoryModel.class, JsonCategory.class, TypeMappingOptions.oneWay())
                                        .fields(COVER, COVER, FieldsMappingOptions.customConverterId(MEDIA_FILE_PATH_CONVERTER))
                                        .fields(AVATAR, AVATAR, FieldsMappingOptions.customConverterId(MEDIA_FILE_PATH_CONVERTER));
                                mapping(Category.class, JsonCategory.class, TypeMappingOptions.oneWay())
                                        .fields(COVER, COVER, FieldsMappingOptions.customConverterId(MEDIA_FILE_PATH_CONVERTER))
                                        .fields(AVATAR, AVATAR, FieldsMappingOptions.customConverterId(MEDIA_FILE_PATH_CONVERTER));
                                mapping(GroupMember.class, JsonGroupMember.class)
                                        .fields("group.id", "groupId");
                                mapping(GroupMember.class, JsonGroupMemberWithNotificationSettings.class)
                                        .fields("group.id", "groupId");
                                mapping(Category.class, JsonCategoryWithLocalization.class, TypeMappingOptions.oneWay())
                                        .fields("localizations", "localizations", customConverter(CategoryLocalizationConverter.class));
                                mapping(JsonPerson.class, JsonPersonInfo.class)
                                        .fields("id", "personId");
                                mapping(JsonPerson.class, PersonData.class, TypeMappingOptions.oneWay())
                                        .fields(AVATAR, AVATAR, customConverter(JsonMediaFilePathConverter.class));
                                mapping(PersonInfo.class, PersonData.class, TypeMappingOptions.oneWay())
                                        .fields(AVATAR, AVATAR, customConverter(JsonMediaFilePathConverter.class));
                            }
                        }
                )
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
}
