package de.swm.lhm.geoportal.gateway.portal;

import de.swm.lhm.geoportal.gateway.base_classes.SqlRunner;
import de.swm.lhm.geoportal.gateway.product.model.Product;
import de.swm.lhm.geoportal.gateway.shared.GatewayService;
import de.swm.lhm.geoportal.gateway.shared.GeoPortalGatewayProperties;
import de.swm.lhm.geoportal.gateway.shared.model.Stage;
import de.swm.lhm.geoportal.gateway.unit.UnitRepository;
import de.swm.lhm.geoportal.gateway.unit.UnitService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Import;

import java.io.IOException;

import static de.swm.lhm.geoportal.gateway.base_classes.hamcrest.HamcrestCompareJsonMatcher.equalToJSON;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;


@Import({
        PortalService.class,
        PortalRepository.class,
        PortalProperties.class,
        UnitService.class,
        UnitRepository.class,
        GatewayService.class,
        GeoPortalGatewayProperties.class
})
@ExtendWith({OutputCaptureExtension.class})
class PortalServiceTest extends SqlRunner {

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private PortalService portalService;

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

        portalService.enrichProduct(testProduct).block();

        assertThat(testProduct.getPortals(), hasSize(3));

        assertThat(testProduct.getPortals().getFirst().getUnit(), is("firstUnit"));
        assertThat(testProduct.getPortals().get(1).getUnit(), is(nullValue()));
        assertThat(testProduct.getPortals().get(2).getUnit(), is("thirdUnit"));

        assertThat(
                testProduct.getPortals(),
                equalToJSON("[{\"name\":\"portal1\",\"title\":null,\"url\":\"http://localhost/portal1\",\"unit\":\"firstUnit\",\"accessLevel\":\"PUBLIC\",\"authLevelHigh\":false},{\"name\":\"portal2\",\"title\":null,\"url\":\"http://localhost/portal2\",\"unit\":null,\"accessLevel\":\"PUBLIC\",\"authLevelHigh\":false},{\"name\":\"portal3\",\"title\":null,\"url\":\"http://localhost/portal3\",\"unit\":\"thirdUnit\",\"accessLevel\":\"PUBLIC\",\"authLevelHigh\":false}]")
        );

    }
}