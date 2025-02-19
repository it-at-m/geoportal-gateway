package de.swm.lhm.geoportal.gateway.sensor;

import de.swm.lhm.geoportal.gateway.base_classes.SqlRunner;
import de.swm.lhm.geoportal.gateway.product.model.Product;
import de.swm.lhm.geoportal.gateway.shared.GatewayService;
import de.swm.lhm.geoportal.gateway.shared.model.ServiceType;
import de.swm.lhm.geoportal.gateway.shared.model.Stage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Import;

import java.io.IOException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;


@Import({
        SensorLayerService.class,
        SensorLayerRepository.class,
        SensorLayerProperties.class,
        GatewayService.class
})
@ExtendWith({OutputCaptureExtension.class})
class SensorLayerServiceTest extends SqlRunner {


    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private SensorLayerService sensorLayerService;

    @MockBean
    private GatewayService gatewayService;

    @MockBean
    private SensorLayerProperties sensorLayerProperties;

    @BeforeEach
    void setUp() throws IOException {
        runSql(loadFileContent("setup.sql").split(";"));
    }

    @AfterEach
    void tearDown() throws IOException {
        runSql(loadFileContent("teardown.sql").split(";"));
    }

    @Test
    void enrichProduct() {

        Mockito.when(gatewayService.getExternalUrl()).thenReturn("test://sensor.host");
        Mockito.when(sensorLayerProperties.getEndpoint()).thenReturn("sensor");

        Product testProduct = new Product();
        testProduct.setId(1);
        testProduct.setStage(Stage.CONFIGURATION);

        sensorLayerService.enrichProduct(testProduct).block();

        assertThat(testProduct.getGeoServices(), hasSize(1));
        assertThat(testProduct.getGeoServices().getFirst().getName(), is("layer1"));
        assertThat(testProduct.getGeoServices().getFirst().getUrls(), hasSize(1));
        assertThat(testProduct.getGeoServices().getFirst().getUrls().getFirst().getServiceType(), is(ServiceType.STA));
        assertThat(testProduct.getGeoServices().getFirst().getUrls().getFirst().getUrl(), is("test://sensor.host/sensor/2e493acd-1551-4bcb-b8bb-b96f05290b87"));

    }

}