package iq.earthlink.social.shortvideoregistryservice.config;

import com.fasterxml.classmate.TypeResolver;
import iq.earthlink.social.common.config.SwaggerConfig;
import iq.earthlink.social.shortvideoregistryservice.dto.CreateShortVideoMessageDTO;
import iq.earthlink.social.shortvideoregistryservice.dto.ShortVideoCategoryDTO;
import iq.earthlink.social.shortvideoregistryservice.model.Category;
import iq.earthlink.social.shortvideoregistryservice.model.ShortVideo;
import iq.earthlink.social.shortvideoregistryservice.model.ShortVideoConfiguration;
import iq.earthlink.social.shortvideoregistryservice.repository.*;
import iq.earthlink.social.shortvideoregistryservice.repository.migration.SchemaMigrationRepository;
import iq.earthlink.social.shortvideoregistryservice.util.SecurityContextUtils;
import org.dozer.DozerBeanMapperBuilder;
import org.dozer.Mapper;
import org.dozer.loader.api.BeanMappingBuilder;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.*;
import org.springframework.data.cassandra.core.CassandraTemplate;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

@Profile("test")
@Configuration
@EnableWebMvc
@EnableGlobalMethodSecurity(securedEnabled = true, prePostEnabled = true)
@Import({
        SwaggerConfig.class,
        springfox.bean.validators.configuration.BeanValidatorPluginsConfiguration.class
})
@ComponentScan(basePackages = {
        "iq.earthlink.social.common.rest",
        "iq.earthlink.social.common.filestorage",
        "iq.earthlink.social.common.util"
})
@EnableRabbit
@EnableWebSecurity
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

    @MockBean
    private ShortVideoConfigurationRepository mockShortVideoConfigurationRepository;

    @MockBean
    private ShortVideoRepository shortVideoRepository;

    @MockBean
    private ShortVideosByAuthorRepository shortVideosByAuthorRepository;

    @MockBean
    ShortVideosOfCategoryRepository shortVideosOfCategoryRepository;

    @MockBean
    ShortVideoConfiguration shortVideoConfiguration;

    @MockBean
    private CategoryRepository categoryRepository;

    @MockBean
    private SecurityContextUtils securityContextUtils;

    @MockBean
    private CassandraTemplate cassandraTemplate;

    @MockBean ShortVideosOfFriendsRepository shortVideosOfFriendsRepository;

    @MockBean ShortVideoAuthorFriendsRepository shortVideoAuthorFriendsRepository;

    @MockBean ShortVideoVoteRepository shortVideoVoteRepository;

    @MockBean ShortVideoStatsRepository shortVideoStatsRepository;

    @MockBean SchemaMigrationRepository schemaMigrationRepository;
}
