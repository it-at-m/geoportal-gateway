package de.swm.lhm.geoportal.gateway.shared.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.session.data.redis.config.annotation.web.server.EnableRedisWebSession;

// https://www.baeldung.com/spring-session-reactive

@Configuration
@EnableRedisWebSession
@RequiredArgsConstructor
@Profile("!test")
public class SessionConfig {

    @Bean
    public LettuceConnectionFactory redisConnectionFactory(RedisProperties redisProperties) {
        RedisStandaloneConfiguration redisConfiguration = new RedisStandaloneConfiguration();
        redisConfiguration.setDatabase(redisProperties.getDatabase());
        redisConfiguration.setHostName(redisProperties.getHost());
        redisConfiguration.setPort(redisProperties.getPort());
        redisConfiguration.setUsername(redisProperties.getUsername());
        redisConfiguration.setPassword(redisProperties.getPassword());

        return new LettuceConnectionFactory(redisConfiguration);
    }

}
