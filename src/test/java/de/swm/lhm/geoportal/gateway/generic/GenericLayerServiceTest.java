package de.swm.lhm.geoportal.gateway.generic;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.swm.lhm.geoportal.gateway.base_classes.SqlRunner;
import de.swm.lhm.geoportal.gateway.product.model.Product;
import de.swm.lhm.geoportal.gateway.shared.model.ServiceType;
import de.swm.lhm.geoportal.gateway.shared.model.Stage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Import;
import java.io.IOException;

import static de.swm.lhm.geoportal.gateway.base_classes.hamcrest.HamcrestCompareJsonMatcher.equalToJSON;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;


@Import({GenericLayerService.class, GenericLayerRepository.class, ObjectMapper.class})
@ExtendWith({OutputCaptureExtension.class})
class GenericLayerServiceTest extends SqlRunner {

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private GenericLayerService genericLayerService;

    @BeforeEach
    void setUp() throws IOException {
        runSql(loadFileContent("setup.sql").split(";"));
    }

    @AfterEach
    void tearDown() throws IOException {
        runSql(loadFileContent("teardown.sql").split(";"));
    }

    @Test
    void enrichProductValidJson() {
        Product testProduct = new Product();
        testProduct.setId(1);
        testProduct.setStage(Stage.CONFIGURATION);

        genericLayerService.enrichProduct(testProduct).block();

        assertThat(testProduct.getGeoServices(), hasSize(1));
        assertThat(testProduct.getGeoServices().getFirst().getName(), is("GEN-43b3a4bb"));
        assertThat(testProduct.getGeoServices().getFirst().getUrls(), hasSize(1));
        assertThat(testProduct.getGeoServices().getFirst().getUrls().getFirst().getServiceType(), is(ServiceType.GEN));
        assertThat(testProduct.getGeoServices().getFirst().getUrls().getFirst().getUrl(), is("test"));

        assertThat(
                testProduct.getGeoServices(),
                equalToJSON("[{\"name\":\"GEN-43b3a4bb\",\"workspace\":null,\"accessLevel\":\"PUBLIC\",\"authLevelHigh\":false,\"urls\":[{\"serviceType\":\"GEN\",\"url\":\"test\"}]}]")
        );

    }

    @Test
    void enrichProductInvalidJson(CapturedOutput output) {
        Product testProduct = new Product();
        testProduct.setId(2);
        testProduct.setStage(Stage.CONFIGURATION);

        genericLayerService.enrichProduct(testProduct).block();

        assertThat(testProduct.getGeoServices(), hasSize(1));
        assertThat(testProduct.getGeoServices().getFirst().getName(), is("GEN-199d4e29"));
        assertThat(testProduct.getGeoServices().getFirst().getUrls(), hasSize(1));
        assertThat(testProduct.getGeoServices().getFirst().getUrls().getFirst().getServiceType(), is(ServiceType.GEN));
        assertThat(testProduct.getGeoServices().getFirst().getUrls().getFirst().getUrl(), is("no url provided"));

        assertThat(output.getAll(), containsString("Could not load json for generic layer GEN-199d4e29, id = 2"));

    }
}