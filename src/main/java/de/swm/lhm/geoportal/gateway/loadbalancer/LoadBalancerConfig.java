package de.swm.lhm.geoportal.gateway.loadbalancer;

import org.springframework.cloud.client.discovery.ReactiveDiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

// https://stackoverflow.com/questions/72162989/spring-cloud-gateway-with-discoveryclient-and-static-routes
// https://spring.io/guides/gs/spring-cloud-loadbalancer/
// https://cloud.spring.io/spring-cloud-gateway/reference/html/#reactive-loadbalancer-client-filter
// https://docs.spring.io/spring-cloud-commons/docs/current/reference/html/


@Configuration
public class LoadBalancerConfig {

    @Bean
    @Primary
    LoadBalancerReactiveClient initLoadBalancerReactiveClient(
            LoadBalancerProperties loadBalancerProperties
    ) {

        return (LoadBalancerReactiveClient) initReactiveDiscoveryClient(loadBalancerProperties);

    }

    @Bean
    ReactiveDiscoveryClient initReactiveDiscoveryClient(
            LoadBalancerProperties loadBalancerProperties
    ) {

        return new LoadBalancerReactiveClient(loadBalancerProperties);

    }

    @Bean
    public StickyLoadBalancerFilter initStickyLoadBalancerFilter(
            LoadBalancerReactiveClient loadBalancerReactiveClient
    ) {

        return new StickyLoadBalancerFilter(loadBalancerReactiveClient);

    }


}

