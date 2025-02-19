package de.swm.lhm.geoportal.gateway.resource;

import de.swm.lhm.geoportal.gateway.base_classes.SqlRunner;
import de.swm.lhm.geoportal.gateway.product.model.Product;
import de.swm.lhm.geoportal.gateway.shared.GatewayService;
import de.swm.lhm.geoportal.gateway.shared.GeoPortalGatewayProperties;
import de.swm.lhm.geoportal.gateway.shared.model.Stage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Import;

import java.io.IOException;

import static de.swm.lhm.geoportal.gateway.base_classes.hamcrest.HamcrestCompareJsonMatcher.equalToJSON;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

@Import({
        ResourceService.class,
        ResourceProperties.class,
        ResourceRepository.class,
        GeoPortalGatewayProperties.class,
        GatewayService.class
})
@ExtendWith({OutputCaptureExtension.class})
class ResourceServiceTest extends SqlRunner {

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    ResourceService resourceService;

    @MockBean
    ResourceProperties resourceProperties;

    @MockBean
    GatewayService gatewayService;

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

        Mockito.when(gatewayService.getExternalUrl()).thenReturn("http://localhost");
        Mockito.when(resourceProperties.getEndpoint()).thenReturn("resource");
        Mockito.when(resourceProperties.getDocumentsFolder()).thenReturn("documents");

        Product testProduct = new Product();
        testProduct.setId(1);
        testProduct.setStage(Stage.CONFIGURATION);

        resourceService.enrichProduct(testProduct).block();

        assertThat(testProduct.getFileResources(), hasSize(2));
        assertThat(testProduct.getFileResources().get(0).getUrl(), is("http://localhost/resource/test/documents/layer/column/1.pdf"));
        assertThat(testProduct.getFileResources().get(1).getUrl(), is("http://localhost/resource/test/documents/layer/column/2.pdf"));
        assertThat(testProduct.getFileResources(), equalToJSON("[{\"name\":\"layer\\\\column\\\\1.pdf\",\"unit\":\"test\",\"url\":\"http://localhost/resource/test/documents/layer/column/1.pdf\",\"accessLevel\":\"PUBLIC\",\"authLevelHigh\":false},{\"name\":\"/layer/column/2.pdf\",\"unit\":\"test\",\"url\":\"http://localhost/resource/test/documents/layer/column/2.pdf\",\"accessLevel\":\"PUBLIC\",\"authLevelHigh\":false}]"));

    }

    @Test
    void enrichProductWithError(CapturedOutput output) {

        Mockito.when(gatewayService.getExternalUrl()).thenReturn(null);

        Product testProduct = new Product();
        testProduct.setId(1);
        testProduct.setStage(Stage.CONFIGURATION);

        resourceService.enrichProduct(testProduct).block();

        assertThat(testProduct.getFileResources(), hasSize(2));
        assertThat(testProduct.getFileResources().get(0).getUrl(), is(""));
        assertThat(testProduct.getFileResources().get(1).getUrl(), is(""));

        assertThat(output.getAll(), containsString("Failed to build url for file resource layer\\column\\1.pdf, gateway external url = null, resource endpoint = null, resource documents folder = null"));
        assertThat(output.getAll(), containsString("Failed to build url for file resource /layer/column/2.pdf, gateway external url = null, resource endpoint = null, resource documents folder = null"));

    }

}