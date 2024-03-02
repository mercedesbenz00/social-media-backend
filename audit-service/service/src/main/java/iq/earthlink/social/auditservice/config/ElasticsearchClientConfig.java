package iq.earthlink.social.auditservice.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties("es")
@Getter
@Setter
@Log4j2
class ElasticsearchClientConfig {

    private String host;
    private int port;
    private String username;
    private String password;

    @Value("${elastic.index}")
    private String indexName;

    @Bean
    public String elasticIndexName(){
        return indexName;
    }

    @Bean
    public ElasticsearchClient elasticSearchClient() {
        log.info("ElasticSearch configuration details: host: {}, port: {}, username: {}, password: {}", host, port, username, password);
        final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(AuthScope.ANY,
                new UsernamePasswordCredentials(username, password));
        RestClientBuilder builder = RestClient.builder(new HttpHost(host, port))
                .setHttpClientConfigCallback(httpClientBuilder ->
                        httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider));
        // Create the low-level client
        RestClient restClient = builder.build();
        // Create the transport with a Jackson mapper
        JacksonJsonpMapper mapper = new JacksonJsonpMapper();
        mapper.objectMapper().registerModule(new JavaTimeModule());
        ElasticsearchTransport transport = new RestClientTransport(restClient, mapper);
        // And create the API client
        return new ElasticsearchClient(transport);
    }
}

