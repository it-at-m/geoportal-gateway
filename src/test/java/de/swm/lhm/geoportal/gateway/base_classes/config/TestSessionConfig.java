package de.swm.lhm.geoportal.gateway.base_classes.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.session.ReactiveMapSessionRepository;
import org.springframework.session.ReactiveSessionRepository;
import org.springframework.session.Session;
import org.springframework.session.config.annotation.web.server.EnableSpringWebSession;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


@TestConfiguration
@EnableSpringWebSession
public class TestSessionConfig {

    public static final Map<String, Session> SESSION_MAP = new ConcurrentHashMap<>();

    @Bean({"reactiveSessionRepository", "sessionRepository"})
    public ReactiveSessionRepository reactiveSessionRepository() {
        return new ReactiveMapSessionRepository(TestSessionConfig.SESSION_MAP);
    }

}
