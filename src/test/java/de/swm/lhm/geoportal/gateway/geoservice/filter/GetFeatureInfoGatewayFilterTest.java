package de.swm.lhm.geoportal.gateway.geoservice.filter;

import com.github.tomakehurst.wiremock.stubbing.StubMapping;
import de.swm.lhm.geoportal.gateway.geoservice.PropertyFieldService;
import de.swm.lhm.geoportal.gateway.geoservice.inspect.QualifiedLayerName;
import de.swm.lhm.geoportal.gateway.geoservice.model.PropertyField;
import de.swm.lhm.geoportal.gateway.geoservice.model.PropertyFieldEscaping;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.when;

@SpringBootTest
class GetFeatureInfoGatewayFilterTest extends AbstractGeoServiceGatewayFilterTest {

    @MockBean
    PropertyFieldService propertyFieldService;

    WebTestClient.ResponseSpec requestGfi() {
        QualifiedLayerName qualifiedLayerName = QualifiedLayerName.fromString("play:gis_osm_pois_free_1_o2o");
        when(propertyFieldService.getGeoServicePropertyFieldsByNameAndWorkspace(qualifiedLayerName))
                .thenReturn(Mono.just(Map.of(
                        "osm_id", new PropertyField("schema1", "table1", "osm_id", "osm_id", true, PropertyFieldEscaping.NONE),
                        "shape", new PropertyField("schema2", "table2", "shape", "shape", true, PropertyFieldEscaping.NONE)
                )));
        when(propertyFieldService.getContainedQualifiedLayerNames(qualifiedLayerName)).thenReturn(Mono.just(List.of(qualifiedLayerName)));


        WebTestClient.ResponseSpec responseSpec = webTestClient
                .get()
                .uri("/geoserver/play/wms?SERVICE=WMS&VERSION=1.3.0&REQUEST=GetFeatureInfo&FORMAT=image%2Fpng&TRANSPARENT=true&QUERY_LAYERS=play:gis_osm_pois_free_1_o2o&CACHEID=1766091&LAYERS=play:gis_osm_pois_free_1_o2o&SINGLETILE=false&WIDTH=512&HEIGHT=512&INFO_FORMAT=text%2Fxml&FEATURE_COUNT=10&I=288&J=65&CRS=EPSG%3A25832&STYLES=&BBOX=690239.36%2C5334908.48%2C690382.72%2C5335051.840000001")
                .exchange();

        responseSpec.expectHeader()
                .valueEquals(HttpHeaders.SERVER, GEOSERVER_SERVER_HEADER);
        return responseSpec;
    }

    @Test
    void propertiesAreReducedToTheExposedOnes() throws IOException {
        StubMapping stub = serveXmlDocument(
                "geoservice/get-feature-info/gis_osm_pois_free_1_o2o.xml",
                true
        );
        WebTestClient.ResponseSpec responseSpec = requestGfi();

        responseSpec.expectBody(String.class)
                .consumeWith(result -> {
                    String body = result.getResponseBody();
                    assertThat(body, containsString("play:osm_id"));
                    assertThat(body, containsString("play:shape"));
                    assertThat(body, not(containsString("play:fclass")));
                    assertThat(body, not(containsString("play:code")));
                });
    }

    @Test
    void failOnUnsupportedContentType() throws IOException {
        StubMapping stub = serveContent("...", MediaType.APPLICATION_PDF_VALUE);
        WebTestClient.ResponseSpec responseSpec = requestGfi();
        responseSpec.expectStatus().is5xxServerError();
        responseSpec.expectBody(String.class)
                .consumeWith(result -> {
                    String body = result.getResponseBody();
                    assertThat(body, containsString(GetFeatureInfoGatewayFilter.MESSAGE_UNSUPPORTED_FORMAT));
                });
    }
}
