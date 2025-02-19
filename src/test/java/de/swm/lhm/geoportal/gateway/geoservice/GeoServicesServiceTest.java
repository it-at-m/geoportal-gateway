package de.swm.lhm.geoportal.gateway.geoservice;

import de.swm.lhm.geoportal.gateway.base_classes.SqlRunner;
import de.swm.lhm.geoportal.gateway.geoservice.inspect.GeoServiceInspectorService;
import de.swm.lhm.geoportal.gateway.product.model.Product;
import de.swm.lhm.geoportal.gateway.shared.GatewayService;
import de.swm.lhm.geoportal.gateway.shared.GeoPortalGatewayProperties;
import de.swm.lhm.geoportal.gateway.shared.model.ServiceType;
import de.swm.lhm.geoportal.gateway.shared.model.Stage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Import;

import java.io.IOException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

@Import({
        GeoServicesService.class,
        GeoServiceRepository.class,
        GeoServiceInspectorService.class,
        GeoServiceProperties.class,
        GatewayService.class,
        GeoPortalGatewayProperties.class
})
@ExtendWith({OutputCaptureExtension.class})
class GeoServicesServiceTest extends SqlRunner {

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private GeoServicesService geoServicesService;

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
        Product testProduct = new Product();
        testProduct.setId(1);
        testProduct.setStage(Stage.CONFIGURATION);

        geoServicesService.enrichProduct(testProduct).block();

        assertThat(testProduct.getGeoServices(), hasSize(4));

        assertThat(testProduct.getGeoServices().get(0).getName(), is("layer1"));
        assertThat(testProduct.getGeoServices().get(1).getName(), is("layer2"));
        assertThat(testProduct.getGeoServices().get(2).getName(), is("layer3"));
        assertThat(testProduct.getGeoServices().get(3).getName(), is("layer4"));

        assertThat(testProduct.getGeoServices().get(0).getWorkspace(), is("workspace1"));
        assertThat(testProduct.getGeoServices().get(1).getWorkspace(), is("workspace2"));
        assertThat(testProduct.getGeoServices().get(2).getWorkspace(), is("workspace3"));
        assertThat(testProduct.getGeoServices().get(3).getWorkspace(), is("workspace4"));

        assertThat(testProduct.getGeoServices().get(0).getUrls(), hasSize(2));
        assertThat(testProduct.getGeoServices().get(1).getUrls(), hasSize(1));
        assertThat(testProduct.getGeoServices().get(2).getUrls(), hasSize(1));
        assertThat(testProduct.getGeoServices().get(3).getUrls(), hasSize(0));

        assertThat(testProduct.getGeoServices().get(1).getUrls().getFirst().getServiceType(), is(ServiceType.WMS));
        assertThat(testProduct.getGeoServices().get(2).getUrls().getFirst().getServiceType(), is(ServiceType.WFS));
    }
}