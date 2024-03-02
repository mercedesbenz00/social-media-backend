package iq.earthlink.social.postservice.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.redisson.spring.cache.RedissonSpringCacheManager;
import org.redisson.spring.data.connection.RedissonConnectionFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.io.IOException;

@EnableCaching
@Configuration
public class CachingConfig {
    @Bean
    public RedisTemplate<Long, Object> redisTemplate(RedissonClient redissonClient) {
        RedisTemplate<Long, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(redissonConnectionFactory(redissonClient));
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new Jackson2JsonRedisSerializer<>(Object.class));
        template.setValueSerializer(new Jackson2JsonRedisSerializer<>(Object.class));
        return template;
    }

    @Bean
    public RedisTemplate<String, Long> redisTemplateLong(RedissonClient redissonClient) {
        RedisTemplate<String, Long> template = new RedisTemplate<>();
        template.setConnectionFactory(redissonConnectionFactory(redissonClient));
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new Jackson2JsonRedisSerializer<>(Long.class));
        template.setValueSerializer(new Jackson2JsonRedisSerializer<>(Long.class));
        return template;
    }

    @Bean(destroyMethod="shutdown")
    RedissonClient redisson(@Value("classpath:/redisson.yaml") Resource configFile) throws IOException {
        Config config = Config.fromYAML(configFile.getInputStream());
        return Redisson.create(config);
    }

    @Bean
    public RedisConnectionFactory redissonConnectionFactory(RedissonClient redissonClient) {
        return new RedissonConnectionFactory(redissonClient);
    }

    @Bean
    CacheManager cacheManager(RedissonClient redissonClient){
        return new RedissonSpringCacheManager(redissonClient, "classpath:/cache-config.yaml");
    }
}
