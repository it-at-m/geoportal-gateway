package de.swm.lhm.geoportal.gateway.geoservice;

import de.swm.lhm.geoportal.gateway.filter.webfilter.ArcGisAuthenticationTriggerFilterTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.reactive.server.WebTestClient;

class ArcGisAuthenticationTriggerFilterIntegrationTest extends AbstractGeoServiceTest {

    @Test
    void isArcGisAuthenticationTriggerFilterUsedForGeoService() {
        mockGeoServer();
        WebTestClient.ResponseSpec responseSpec = webTestClient.get()
                .uri("/geoserver/wms")
                .header(HttpHeaders.USER_AGENT, ArcGisAuthenticationTriggerFilterTest.ARC_GIS_USER_AGENT)
                .exchange();

        responseSpec.expectStatus().isUnauthorized();
        expectNoGeoServerResponse(responseSpec);
    }
}
