package de.swm.lhm.geoportal.gateway.geoservice.filter;

import de.swm.lhm.geoportal.gateway.geoservice.AbstractGeoServiceTest;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

@TestPropertySource(properties = {
        "geoportal.geoserver.blocked-request-types[0]=DescribeFeatureType", // for WFS
        "geoportal.geoserver.blocked-request-types[1]=GetLegendGraphic", // for WMS
        "geoportal.geoserver.blocked-request-types[2]=GetTile" // for WMTS
})
class BlockedGeoServiceRequestsGatewayFilterTest extends AbstractGeoServiceTest {

    void testBlockedRequest(String path) {
        mockGeoServer();
        WebTestClient.ResponseSpec responseSpec = webTestClient
                .get()
                .uri(path)
                .exchange();

        expectNoGeoServerResponse(responseSpec);

        responseSpec.expectStatus()
                .isForbidden();
    }

    @Test
    void wfsRequestBlocked() {
        testBlockedRequest("/geoserver/wfs?service=wfs&request=DescribeFeatureType");
    }

    @Test
    void wmsRequestBlocked() {
        testBlockedRequest("/geoserver/wms?service=wms&request=GetLegendGraphic");
    }

    @Test
    void wmtsRequestBlocked() {
        testBlockedRequest("/geoserver/gwc/service/wmts?service=wmts&request=GetTile");
    }
}
