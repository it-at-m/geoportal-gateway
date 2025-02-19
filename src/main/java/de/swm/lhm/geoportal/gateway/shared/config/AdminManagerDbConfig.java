package de.swm.lhm.geoportal.gateway.shared.config;

import de.swm.lhm.geoportal.gateway.GatewayApplication;
import de.swm.lhm.geoportal.gateway.actuator.GatewayGitProperties;
import de.swm.lhm.geoportal.gateway.actuator.GatewayInfoProperties;
import io.r2dbc.pool.ConnectionPool;
import io.r2dbc.pool.ConnectionPoolConfiguration;
import io.r2dbc.postgresql.PostgresqlConnectionConfiguration;
import io.r2dbc.postgresql.PostgresqlConnectionFactory;
import io.r2dbc.spi.ConnectionFactory;
import jakarta.annotation.Nonnull;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.r2dbc.config.AbstractR2dbcConfiguration;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;
import org.springframework.r2dbc.connection.R2dbcTransactionManager;
import org.springframework.r2dbc.connection.TransactionAwareConnectionFactoryProxy;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.transaction.ReactiveTransactionManager;


@NoArgsConstructor
@Getter
@Setter
@Slf4j
@Configuration
@ConfigurationProperties(prefix = "geoportal.admin-manager.datasource")
@EnableR2dbcRepositories(basePackageClasses = {GatewayApplication.class})
@Profile("!h2db")
public class AdminManagerDbConfig extends AbstractR2dbcConfiguration {

    String username;
    String password;
    String host;
    Integer port;
    String database;
    String schema;
    PoolConfig pool;

    @Autowired
    private GatewayInfoProperties infoProperties;
    @Autowired
    private GatewayGitProperties gitProperties;

    @Override
    @Nonnull
    @Bean
    public ConnectionFactory connectionFactory() {

        ConnectionPoolConfiguration config = ConnectionPoolConfiguration.builder(
                        new PostgresqlConnectionFactory(
                                PostgresqlConnectionConfiguration.builder()
                                        .host(host)
                                        .port(port)
                                        .database(database)
                                        .schema(schema)
                                        .username(username)
                                        .password(password)
                                        .applicationName(this.getApplicationName())
                                        .tcpKeepAlive(true)
                                        .build()
                        )
                )
                .minIdle(pool.getMinIdle())
                .initialSize(pool.getMinIdle())
                .maxSize(pool.getMaxSize())
                .build();


        log.debug(
                "Connection Pool Configuration: minIdle={}, initialSize={}, maxSize={}",
                pool.getMinIdle(),
                pool.getMinIdle(),
                pool.getMaxSize()
        );

        return new ConnectionPool(config);

    }

    private String getApplicationName(){

        String name = infoProperties.getName();
        if (name == null || name.isEmpty()) {
            return "";
        }

        name = name.replaceAll("[^a-zA-Z0-9._-]", "-");

        String version;
        try {
            version = gitProperties.getBuild().getVersion();
        } catch (NullPointerException e) {
            version = "";
        }

        if (version == null) {
            version = "";
        }

        version = version.replaceAll("[^a-zA-Z0-9._-]", "-");

        if (!version.isEmpty()) {
            version = "@%s".formatted(version);
        }

        return name + version;

    }

    @Bean
    ReactiveTransactionManager transactionManager(ConnectionFactory connectionFactory) {
        return new R2dbcTransactionManager(connectionFactory);
    }

    @Bean
    TransactionAwareConnectionFactoryProxy transactionAwareConnectionFactoryProxy(ConnectionFactory connectionFactory) {
        return new TransactionAwareConnectionFactoryProxy(connectionFactory);
    }

    @Bean({"r2dbcDatabaseClient", "databaseClient"})
    DatabaseClient databaseClient(ConnectionFactory connectionFactory) {
        return DatabaseClient.builder()
                .connectionFactory(connectionFactory)
                .namedParameters(true)
                .build();
    }

    @Data
    @NoArgsConstructor
    public static class PoolConfig {
        private int minIdle;
        private int maxSize;
    }

}