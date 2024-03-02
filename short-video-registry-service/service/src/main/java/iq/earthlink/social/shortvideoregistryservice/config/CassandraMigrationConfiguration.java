package iq.earthlink.social.shortvideoregistryservice.config;

import com.datastax.driver.core.Cluster;
import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.CqlSessionBuilder;
import org.cognitor.cassandra.migration.MigrationTask;
import org.cognitor.cassandra.migration.spring.CassandraMigrationAutoConfiguration;
import org.cognitor.cassandra.migration.spring.CassandraMigrationConfigurationProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Profile("!test")
@Configuration
@EnableConfigurationProperties({CassandraMigrationConfigurationProperties.class})
@ConditionalOnClass({Cluster.class})
public class CassandraMigrationConfiguration extends CassandraMigrationAutoConfiguration {

    @Autowired
    public CassandraMigrationConfiguration(CassandraMigrationConfigurationProperties properties) {
        super(properties);
    }

    @Bean(initMethod = "migrate")
    @ConditionalOnMissingBean(MigrationTask.class)
    public MigrationTask migrationTask(CqlSessionBuilder sessionBuilder) {
        CqlSession cqlSession = sessionBuilder.build();
        return super.migrationTask(cqlSession);
    }
}

