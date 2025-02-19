package de.swm.lhm.geoportal.gateway;

import de.swm.lhm.geoportal.gateway.loadbalancer.LoadBalancerConfig;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.loadbalancer.annotation.LoadBalancerClients;

@SpringBootApplication(scanBasePackages = "de.swm.lhm.geoportal")
@Slf4j
@ConfigurationPropertiesScan
@EnableConfigurationProperties
@LoadBalancerClients(defaultConfiguration = LoadBalancerConfig.class)
public class GatewayApplication {

    @PreDestroy
    public void logShutdown() {
        log.info("Gateway is shutting down");
    }

    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }

}
