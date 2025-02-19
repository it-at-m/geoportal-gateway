package de.swm.lhm.geoportal.gateway.shared.config;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.test.context.junit.jupiter.SpringExtension;


@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = {SessionConfigTest.TestConfig.class, SessionConfig.class})
class SessionConfigTest {

    @Autowired
    private LettuceConnectionFactory lettuceConnectionFactory;

    @Test
    void testRedisProperties() {
        RedisStandaloneConfiguration redisConfiguration = lettuceConnectionFactory.getStandaloneConfiguration();

        assertThat(redisConfiguration).isNotNull();
        assertThat(redisConfiguration.getDatabase()).isEqualTo(99);
        assertThat(redisConfiguration.getHostName()).isEqualTo("notlocalhost");
        assertThat(redisConfiguration.getPort()).isEqualTo(1234);
        assertThat(redisConfiguration.getUsername()).isEqualTo("user");
        assertThat(new String(redisConfiguration.getPassword().get())).isEqualTo("password");
    }

    @Configuration
    @EnableConfigurationProperties(RedisProperties.class)
    static class TestConfig {
        @Bean
        public RedisProperties redisProperties() {
            RedisProperties redisProperties = new RedisProperties();
            redisProperties.setDatabase(99);
            redisProperties.setHost("notlocalhost");
            redisProperties.setPort(1234);
            redisProperties.setUsername("user");
            redisProperties.setPassword("password");
            return redisProperties;
        }
    }
}