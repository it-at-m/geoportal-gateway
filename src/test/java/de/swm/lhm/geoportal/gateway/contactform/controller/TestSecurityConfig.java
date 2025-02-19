package de.swm.lhm.geoportal.gateway.contactform.controller;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

@TestConfiguration
@EnableWebFluxSecurity
public class TestSecurityConfig {

    @Order(1)
    @Bean
    SecurityWebFilterChain filterChain(ServerHttpSecurity http) {
        return http.authorizeExchange(
                authorizeExchangeSpec -> authorizeExchangeSpec.anyExchange().permitAll()
        ).build();
    }

}

