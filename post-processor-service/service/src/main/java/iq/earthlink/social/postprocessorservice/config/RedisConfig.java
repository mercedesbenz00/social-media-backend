package iq.earthlink.social.postprocessorservice.config;

import iq.earthlink.social.postprocessorservice.dto.RecentPost;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisClusterConfiguration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.util.List;

@Configuration
public class RedisConfig {
    @Value("${spring.redis.host}")
    private String redisHost;

    @Value("${spring.redis.port}")
    private int redisPort;

    @Value("${spring.redis.password}")
    private String redisPassword;

    @Value("${spring.redis.database}")
    private int redisDatabase;

    @Value("${spring.redis.cluster.nodes}")
    private List<String> clusterNodes;

    @Bean
    @ConditionalOnProperty(name = "spring.redis.mode", havingValue = "cluster")
    public RedisConnectionFactory redisConnectionFactoryCluster() {
        var redisConfiguration = new RedisClusterConfiguration(clusterNodes);
        redisConfiguration.setPassword(redisPassword);
        return new LettuceConnectionFactory(redisConfiguration);
    }

    @Bean
    @ConditionalOnProperty(name = "spring.redis.mode", havingValue = "standalone", matchIfMissing = true)
    public RedisConnectionFactory redisConnectionFactoryStandalone() {
        var redisConfiguration = new RedisStandaloneConfiguration(redisHost, redisPort);
        redisConfiguration.setPassword(redisPassword);
        redisConfiguration.setDatabase(redisDatabase);
        return new LettuceConnectionFactory(redisConfiguration);
    }

    @Bean
    public RedisTemplate<String, RecentPost> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, RecentPost> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new Jackson2JsonRedisSerializer<>(Object.class));
        template.setValueSerializer(new Jackson2JsonRedisSerializer<>(Object.class));
        return template;
    }
}
