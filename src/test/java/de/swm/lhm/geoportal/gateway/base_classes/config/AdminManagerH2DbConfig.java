package de.swm.lhm.geoportal.gateway.base_classes.config;

import de.swm.lhm.geoportal.gateway.GatewayApplication;
import io.r2dbc.h2.H2ConnectionConfiguration;
import io.r2dbc.h2.H2ConnectionFactory;
import io.r2dbc.pool.ConnectionPool;
import io.r2dbc.pool.ConnectionPoolConfiguration;
import io.r2dbc.spi.ConnectionFactory;
import jakarta.annotation.Nonnull;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.data.r2dbc.config.AbstractR2dbcConfiguration;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;
import org.springframework.r2dbc.connection.R2dbcTransactionManager;
import org.springframework.r2dbc.connection.TransactionAwareConnectionFactoryProxy;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.transaction.ReactiveTransactionManager;

import java.util.UUID;


@TestConfiguration
@EnableR2dbcRepositories(basePackageClasses = {GatewayApplication.class})
@Profile("h2db")
public class AdminManagerH2DbConfig extends AbstractR2dbcConfiguration {

    @Override
    @Nonnull
    @Bean
    public ConnectionFactory connectionFactory() {
        return new ConnectionPool(
                ConnectionPoolConfiguration.builder(
                        new H2ConnectionFactory(
                                H2ConnectionConfiguration.builder()
                                        .inMemory(UUID.randomUUID().toString())
                                        .username("sa")
                                        .password("")
                                        .build()
                        )
                ).build()
        );
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

}
