package de.swm.lhm.geoportal.gateway.geoservice.filter;

import com.github.tomakehurst.wiremock.stubbing.StubMapping;
import de.swm.lhm.geoportal.gateway.geoservice.GeoServiceProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.io.IOException;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;


@TestPropertySource(properties = {
    "geoportal.geoserver.blocked-request-types[0]=DescribeFeatureType", // for WFS
    "geoportal.geoserver.blocked-request-types[1]=GetLegendGraphic", // for WMS
    "geoportal.geoserver.blocked-request-types[2]=GetTile" // for WMTS
})
public class GetCapabilitiesGatewayFilterTest extends AbstractGeoServiceGatewayFilterTest {


    @Autowired
    GeoServiceProperties geoServiceProperties;

    @Test
    void filterWfsWithoutRestricted() throws IOException {
        filterCapabilitiesDocumentWithoutRestricted(WFS_ENDPOINT, WFS_DOCUMENT);
    }

    @Test
    void filterWfsWithRestricted() throws IOException {
        filterCapabilitiesDocumentWithRestricted(WFS_ENDPOINT, WFS_DOCUMENT);
    }

    @Test
    void filterWmsWithoutRestricted() throws IOException {
        filterCapabilitiesDocumentWithoutRestricted(WMS_ENDPOINT, WMS_DOCUMENT);
    }

    @Test
    void filterWmsWithRestricted() throws IOException {
        filterCapabilitiesDocumentWithRestricted(WMS_ENDPOINT, WMS_DOCUMENT);
    }

    @Test
    void filterWmtsWithoutRestricted() throws IOException {
        filterCapabilitiesDocumentWithoutRestricted(WMTS_ENDPOINT, WMTS_DOCUMENT);
    }

    @Test
    void filterWmtsWithRestricted() throws IOException {
        filterCapabilitiesDocumentWithRestricted(WMTS_ENDPOINT, WMTS_DOCUMENT);
    }

    void filterCapabilitiesDocumentWithoutRestricted(String geoserverPath, String capabilitiesDocumentResource) throws IOException {
        StubMapping stub = serveXmlDocument(capabilitiesDocumentResource);

        WebTestClient.ResponseSpec responseSpec = webTestClient
                .mutateWith(
                        keyCloakConfigureGrantedProducts(List.of(PROTECTED_PRODUCT))
                )
                .get()
                .uri(geoserverPath)
                .exchange();

        responseSpec.expectHeader()
                .valueEquals(HttpHeaders.SERVER, GEOSERVER_SERVER_HEADER);

        responseSpec.expectBody(String.class)
                .consumeWith(result -> {
                    String body = result.getResponseBody();
                    assertThat(body, containsString(PROTECTED_LAYER));
                    assertThat(body, not(containsString(RESTRICTED_LAYER)));
                    checkContainsNoBlockedRequests(body);
                });
    }

    void filterCapabilitiesDocumentWithRestricted(String geoserverPath, String capabilitiesDocumentResource) throws IOException {
        StubMapping stub = serveXmlDocument(capabilitiesDocumentResource);

        WebTestClient.ResponseSpec responseSpec = webTestClient
                .mutateWith(
                        keyCloakConfigureGrantedProducts(List.of(PROTECTED_PRODUCT, RESTRICTED_PRODUCT))
                )
                .get()
                .uri(geoserverPath)
                .exchange();

        responseSpec.expectHeader()
                .valueEquals(HttpHeaders.SERVER, GEOSERVER_SERVER_HEADER);

        responseSpec.expectBody(String.class)
                .consumeWith(result -> {
                    String body = result.getResponseBody();
                    assertThat(body, containsString(PROTECTED_LAYER));
                    assertThat(body, containsString(RESTRICTED_LAYER));
                    checkContainsNoBlockedRequests(body);
                    checkContainsNoUnsupportedGfiFormats(body);
                });
    }


    void checkContainsNoBlockedRequests(String responseBody) {
        for (String blockedRequest: geoServiceProperties.getBlockedRequestTypes()) {
            assertThat(responseBody, not(containsStringIgnoringCase(blockedRequest)));
        }
    }

    void checkContainsNoUnsupportedGfiFormats(String responseBody) {
        assertThat(responseBody, not(containsStringIgnoringCase("application/unsupported-gfi-format")));
    }
}
