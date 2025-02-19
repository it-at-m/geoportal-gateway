package de.swm.lhm.geoportal.gateway.geoservice.filter;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.io.IOException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;

@TestPropertySource(properties = {
        "geoportal.geoserver.hostname-mapping=http://maps6.geosolutionsgroup.com,http://maps.somewhereelse.de",
})
class HostnameReplacingGatewayFilterTest extends AbstractGeoServiceGatewayFilterTest {

    @Test
    void capabilitiesHostNamesAreReplaced() throws IOException {
        serveXmlDocument(WMS_DOCUMENT);
        WebTestClient.ResponseSpec responseSpec = webTestClient
                .get()
                .uri(WMS_ENDPOINT)
                .exchange();

        responseSpec.expectHeader()
                .valueEquals(HttpHeaders.SERVER, GEOSERVER_SERVER_HEADER);

        expectReplaced(responseSpec);
    }

    @Test
    void describeFeatureTypeHostNamesAreReplaced() throws IOException {
        serveXmlDocument("geoservice/describe-feature-type/ne-ne_50m_urban_areas.xsd", true);
        WebTestClient.ResponseSpec responseSpec = webTestClient
                .get()
                .uri("/geoserver/wfs?service=WFS&request=DescribeFeatureType&typename=ne:ne_50m_urban_areasType")
                .exchange();

        responseSpec.expectHeader()
                .valueEquals(HttpHeaders.SERVER, GEOSERVER_SERVER_HEADER);

        expectReplaced(responseSpec);
    }


    private void expectReplaced(WebTestClient.ResponseSpec responseSpec) {
        responseSpec.expectBody(String.class)
                .consumeWith(result -> {
                    String body = result.getResponseBody();
                    assertThat(body, containsString("http://maps.somewhereelse.de"));
                    assertThat(body, not(containsString("http://maps6.geosolutionsgroup.com")));
                });
    }
}
