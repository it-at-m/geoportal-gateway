package de.swm.lhm.geoportal.gateway.metadata;

import de.swm.lhm.geoportal.gateway.base_classes.SqlRunner;
import de.swm.lhm.geoportal.gateway.product.model.Product;
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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;


@Import({
        MetadataProperties.class,
        MetadataService.class,
        MetadataRepository.class
})
@ExtendWith({OutputCaptureExtension.class})
class MetadataServiceTest extends SqlRunner {

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    MetadataService metadataService;

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    MetadataProperties metadataProperties;

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

        Product testProduct = run("testUrl", "testParam");
        assertThat(testProduct.getMetadataUrl(), is("testUrl?testParam=cc8f7e3c-ee50-4714-ae25-8da5384ebfc2"));

    }

    @Test
    void enrichProductWithError(CapturedOutput output) {

        Product testProduct = run(null, null);

        assertThat(testProduct.getMetadataUrl(), is(""));
        assertThat(output.getAll(), containsString("ERROR"));
        assertThat(output.getAll(), containsString("Failed to build url for metadata cc8f7e3c-ee50-4714-ae25-8da5384ebfc2, metadataDetailUrl=null, metadataIdParameter=null"));

    }

    private Product run(String detailUrl, String idParameter){

        Product testProduct = new Product();
        testProduct.setId(1);
        testProduct.setMetadataId(1);
        testProduct.setStage(Stage.CONFIGURATION);

        metadataProperties.setDetailUrl(detailUrl);
        metadataProperties.setIdParameter(idParameter);

        metadataService.enrichProduct(testProduct).block();

        return testProduct;
    }





}
