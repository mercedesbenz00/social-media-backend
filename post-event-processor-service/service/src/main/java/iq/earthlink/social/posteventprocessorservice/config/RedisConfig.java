package iq.earthlink.social.posteventprocessorservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import redis.clients.jedis.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
    @ConditionalOnProperty(name = "spring.redis.mode", havingValue = "standalone", matchIfMissing = true)
    public JedisPooled jedisClient() {
        JedisClientConfig jedisClientConfig = DefaultJedisClientConfig.builder()
                .password(redisPassword)
                .database(redisDatabase)
                .build();

        HostAndPort hostAndPort = new HostAndPort(redisHost, redisPort);
        return new JedisPooled(hostAndPort, jedisClientConfig);
    }

    @Bean
    @ConditionalOnProperty(name = "spring.redis.mode", havingValue = "cluster")
    public JedisCluster jedisCluster() {
        Set<HostAndPort> nodes = new HashSet<>();
        for (String node : clusterNodes) {
            String[] parts = node.split(":");
            nodes.add(new HostAndPort(parts[0], Integer.parseInt(parts[1])));
        }
        JedisClientConfig jedisClientConfig = DefaultJedisClientConfig.builder()
//                .password(redisPassword)
                .build();
        return new JedisCluster(nodes, jedisClientConfig);
    }
}
