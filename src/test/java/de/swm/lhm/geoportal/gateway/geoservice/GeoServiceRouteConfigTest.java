package de.swm.lhm.geoportal.gateway.geoservice;

import org.junit.jupiter.api.Test;

class GeoServiceRouteConfigTest extends AbstractGeoServiceTest {

    @Test
    void geoserverAllowedRoutes() {
        mockGeoServer();

        webTestClient.get().uri("/geoserver/web/dfg").exchange().expectStatus().isNotFound();
        webTestClient.get().uri("/geoserver/rest/dfg").exchange().expectStatus().isNotFound();
        expectGeoServerResponseOk(
                webTestClient.get().uri("/geoserver/some_workspace/wms?SERVICE=WMS").exchange()
        );
        expectGeoServerResponseOk(
            webTestClient.get().uri("/geoserver/some_workspace/wms?SERVICE=WMS").exchange()
        );
        expectGeoServerResponseOk(
            webTestClient.get().uri("/geoserver/some_workspace/wfs?SERVICE=WFS").exchange()
        );
        expectGeoServerResponseOk(
            webTestClient.get().uri("/geoserver/gwc/service/wmts?SERVICE=WMTS").exchange()
        );
    }
}
