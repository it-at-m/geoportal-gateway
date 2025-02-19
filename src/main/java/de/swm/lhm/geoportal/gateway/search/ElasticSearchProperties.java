package de.swm.lhm.geoportal.gateway.search;

import de.swm.lhm.geoportal.gateway.loadbalancer.LoadBalancerProperties;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;


@Configuration
@ConfigurationProperties(prefix = ElasticSearchProperties.ELASTICSEARCH_PROPERTIES_PREFIX)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ElasticSearchProperties {

    public static final String ELASTICSEARCH_PROPERTIES_PREFIX = LoadBalancerProperties.LOAD_BALANCER_PROPERTIES_PREFIX + ".services." + ElasticSearchProperties.SERVICE_ID;
    public static final String SERVICE_ID = "elastic-search";
    private String username;
    private String password;
    private String addressIndexName = "addresses";
    // the urls of elastic search are set via the load balancer
}
