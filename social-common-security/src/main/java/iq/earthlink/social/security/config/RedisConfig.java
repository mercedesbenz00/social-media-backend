package iq.earthlink.social.security.config;

import iq.earthlink.social.security.BlockedToken;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;

@Configuration
@EnableRedisRepositories(basePackageClasses = BlockedToken.class)
public class RedisConfig {
}