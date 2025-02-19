package de.swm.lhm.geoportal.gateway.search.client;

import de.swm.lhm.geoportal.gateway.search.ElasticSearchProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.loadbalancer.reactive.ReactorLoadBalancerExchangeFilterFunction;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

@Configuration
@Slf4j
public class ElasticSearchConfig {

    @Bean
    WebClient getElasticSearchClient(
            ReactorLoadBalancerExchangeFilterFunction lbFunction,
            ElasticSearchProperties elasticSearchProperties
    ) {

        WebClient.Builder webClientBuilder = WebClient.builder()
                .filter(lbFunction);

        if (elasticSearchProperties.getUsername() != null && elasticSearchProperties.getPassword() != null){
            webClientBuilder.defaultHeaders(
                    httpHeaders -> {
                        httpHeaders.setBasicAuth(elasticSearchProperties.getUsername(), elasticSearchProperties.getPassword());
                        httpHeaders.setContentType(MediaType.APPLICATION_JSON);
                        httpHeaders.setAccept(List.of(MediaType.APPLICATION_JSON));
                    });
        } else {
            log.warn(
                    "elasticsearch calls will be made without authentication because {}.username = {}, {}.password = {},",
                    ElasticSearchProperties.ELASTICSEARCH_PROPERTIES_PREFIX, elasticSearchProperties.getUsername(),
                    ElasticSearchProperties.ELASTICSEARCH_PROPERTIES_PREFIX, elasticSearchProperties.getPassword() == null ? null : "***"
            );
        }

        return webClientBuilder.build();

    }
}
