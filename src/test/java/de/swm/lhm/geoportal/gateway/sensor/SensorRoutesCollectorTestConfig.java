package de.swm.lhm.geoportal.gateway.sensor;

import de.swm.lhm.geoportal.gateway.base_classes.FileLoader;
import io.r2dbc.spi.ConnectionFactory;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.r2dbc.connection.init.CompositeDatabasePopulator;
import org.springframework.r2dbc.connection.init.ConnectionFactoryInitializer;
import org.springframework.r2dbc.connection.init.ResourceDatabasePopulator;

import java.io.IOException;
import java.nio.charset.StandardCharsets;


@TestConfiguration
public class SensorRoutesCollectorTestConfig extends FileLoader {

    @Bean
    public ConnectionFactoryInitializer initializer(ConnectionFactory connectionFactory) throws IOException {

        //Sql Scripts must run before application starts

        ConnectionFactoryInitializer initializer = new ConnectionFactoryInitializer();
        initializer.setConnectionFactory(connectionFactory);

        CompositeDatabasePopulator populator = new CompositeDatabasePopulator();

        populator.addPopulators(new ResourceDatabasePopulator(new ClassPathResource("de/swm/lhm/geoportal/gateway/sensor/setup.sql")));

        String fileContent = String.format(
                loadFileContent("de/swm/lhm/geoportal/gateway/sensor/sensor-collector-setup.sql"),
                SensorRoutesCollectorTest.FROST_PORT,
                SensorRoutesCollectorTest.FROST_PORT
        );
        populator.addPopulators(new ResourceDatabasePopulator(new ByteArrayResource(fileContent.getBytes(StandardCharsets.UTF_8))));

        initializer.setDatabasePopulator(populator);

        return initializer;
    }

}
