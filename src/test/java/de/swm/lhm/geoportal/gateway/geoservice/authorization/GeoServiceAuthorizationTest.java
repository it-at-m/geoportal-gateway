package de.swm.lhm.geoportal.gateway.geoservice.authorization;

import de.swm.lhm.geoportal.gateway.geoservice.AbstractGeoServiceTest;
import de.swm.lhm.geoportal.gateway.geoservice.inspect.QualifiedLayerName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.containing;


class GeoServiceAuthorizationTest extends AbstractGeoServiceTest {
    @ParameterizedTest
    @ValueSource(strings = {
            "/geoserver/some_workspace/wms?SERVICE=WMS&layers=",
            "/geoserver/some_workspace/wfs?SERVICE=WFS&typenames=",
            "/geoserver/gwc/service/wmts?SERVICE=WMTS&layer=",
    })
    void accessPublicLayer(String urlFragment) {
        mockGeoServer();
        WebTestClient.ResponseSpec responseSpec = webTestClient.get()
                .uri(urlFragment + PUBLIC_LAYER)
                .exchange();
        responseSpec.expectStatus().isOk();
        expectGeoServerResponse(responseSpec);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/geoserver/some_workspace/wms?SERVICE=WMS&layers=",
            "/geoserver/some_workspace/wfs?SERVICE=WFS&typenames=",
            "/geoserver/gwc/service/wmts?SERVICE=WMTS&layer=",
    })
    void accessProtectedLayerWithoutAuth(String urlFragment) {
        mockGeoServer();
        WebTestClient.ResponseSpec responseSpec = webTestClient.get()
                .uri(urlFragment + PROTECTED_LAYER)
                .exchange();
        expectRedirectToKeyCloak(responseSpec);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/geoserver/some_workspace/wms?SERVICE=WMS&layers=",
            "/geoserver/some_workspace/wfs?SERVICE=WFS&typenames=",
            "/geoserver/gwc/service/wmts?SERVICE=WMTS&layer=",
    })
    void accessProtectedLayer(String urlFragment) {
        mockGeoServer();
        WebTestClient.ResponseSpec responseSpec = webTestClient.mutateWith(
                        keyCloakConfigureGrantedProducts(List.of(PROTECTED_PRODUCT))
                )
                .get()
                .uri(urlFragment + PROTECTED_LAYER)
                .exchange();
        responseSpec.expectStatus().isOk();
        expectGeoServerResponse(responseSpec);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/geoserver/some_workspace/wms?SERVICE=WMS&layers=",
            "/geoserver/some_workspace/wfs?SERVICE=WFS&typenames=",
            "/geoserver/gwc/service/wmts?SERVICE=WMTS&layer=",
    })
    void accessProtectedLayerDeniedByAuthLevel(String urlFragment) {
        mockGeoServer();
        WebTestClient.ResponseSpec responseSpec = webTestClient.mutateWith(
                        keyCloakConfigureGrantedProducts(List.of(PROTECTED_AUTH_LEVEL_HIGH_PRODUCT), false)
                )
                .get()
                .uri(urlFragment + PROTECTED_AUTH_LEVEL_HIGH_LAYER)
                .exchange();
        responseSpec.expectStatus().isForbidden();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/geoserver/some_workspace/wms?SERVICE=WMS&layers=",
            "/geoserver/some_workspace/wfs?SERVICE=WFS&typenames=",
            "/geoserver/gwc/service/wmts?SERVICE=WMTS&layer=",
    })
    void accessProtectedLayerWithAuthLevelHigh(String urlFragment) {
        mockGeoServer();
        WebTestClient.ResponseSpec responseSpec = webTestClient.mutateWith(
                        keyCloakConfigureGrantedProducts(List.of(PROTECTED_AUTH_LEVEL_HIGH_PRODUCT), true)
                )
                .get()
                .uri(urlFragment + PROTECTED_AUTH_LEVEL_HIGH_LAYER)
                .exchange();
        responseSpec.expectStatus().isOk();
        expectGeoServerResponse(responseSpec);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/geoserver/some_workspace/wms?SERVICE=WMS&layers=",
            "/geoserver/some_workspace/wfs?SERVICE=WFS&typenames=",
            "/geoserver/gwc/service/wmts?SERVICE=WMTS&layer=",
    })
    void accessRestrictedLayerWithoutAuth(String urlFragment) {
        mockGeoServer();
        WebTestClient.ResponseSpec responseSpec = webTestClient.get()
                .uri(urlFragment + RESTRICTED_LAYER)
                .exchange();
        expectRedirectToKeyCloak(responseSpec);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/geoserver/some_workspace/wms?SERVICE=WMS&layers=",
            "/geoserver/some_workspace/wfs?SERVICE=WFS&typenames=",
            "/geoserver/gwc/service/wmts?SERVICE=WMTS&layer=",
    })
    void accessRestrictedLayer(String urlFragment) {
        mockGeoServer();
        WebTestClient.ResponseSpec responseSpec = webTestClient.mutateWith(
                        keyCloakConfigureGrantedProducts(List.of(RESTRICTED_PRODUCT))
                )
                .get()
                .uri(urlFragment + RESTRICTED_LAYER)
                .exchange();
        responseSpec.expectStatus().isOk();
        expectGeoServerResponse(responseSpec);
    }

    @Test
    void accessProtectedLayerWithoutAuthUsingPost() {
        mockGeoServer();

        WebTestClient.ResponseSpec responseSpec = webTestClient.post()
                .uri("/geoserver/some_workspace/wms")
                .headers(httpHeaders -> {
                    httpHeaders.set(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XML_VALUE);
                })
                .bodyValue(buildGetMapXml(PROTECTED_LAYER))
                .exchange();
        expectRedirectToKeyCloak(responseSpec);
    }

    @Test
    void accessProtectedLayerUsingPost() {
        mockGeoServerPostWithContentPattern(containing(PROTECTED_LAYER));
        WebTestClient.ResponseSpec responseSpec = webTestClient.mutateWith(
                        keyCloakConfigureGrantedProducts(List.of(PROTECTED_PRODUCT))
                )
                .post()
                .uri("/geoserver/some_workspace/wms")
                .headers(httpHeaders -> {
                    httpHeaders.set(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XML_VALUE);
                })
                .bodyValue(buildGetMapXml(PROTECTED_LAYER))
                .exchange();
        expectGeoServerResponse(responseSpec);
    }

    @Test
    void accessProtectedLayerUsingPostWorkspaceOmittedFromLayerName() {
        QualifiedLayerName protectedLayer = QualifiedLayerName.fromString(PROTECTED_LAYER);
        mockGeoServerPostWithContentPattern(containing(protectedLayer.layerName()));

        WebTestClient.ResponseSpec responseSpec = webTestClient
                .post()
                .uri(String.format("/geoserver/%s/wms", protectedLayer.workspaceName()))
                .headers(httpHeaders -> {
                    httpHeaders.set(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XML_VALUE);
                })
                .bodyValue(buildGetMapXml(protectedLayer.layerName()))
                .exchange();
        expectRedirectToKeyCloak(responseSpec);
    }


    @Test
    void accessUsingPostWithBrokenXml() {
        mockGeoServer();
        WebTestClient.ResponseSpec responseSpec = webTestClient.post()
                .uri("/geoserver/some_workspace/wms")
                .headers(httpHeaders -> {
                    httpHeaders.set(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XML_VALUE);
                })
                .bodyValue("</brokenxml>")
                .exchange();

        responseSpec.expectHeader().doesNotExist(HttpHeaders.SERVER); // no response from mocked geoserver
        responseSpec.expectStatus().isNotFound(); // route did not match todo: returning bad-request would be nicer
    }

    private String buildGetMapXml(String layerName) {
        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <ogc:GetMap xmlns:ogc="http://www.opengis.net/ows"
                            xmlns:gml="http://www.opengis.net/gml"
                   version="1.1.1" service="WMS">
                   <StyledLayerDescriptor version="1.0.0">
                      <NamedLayer>
                        <Name>""" + layerName + """
                                        </Name>
                                        <NamedStyle><Name>population</Name></NamedStyle>
                                      </NamedLayer>
                                   </StyledLayerDescriptor>
                                   <BoundingBox srsName="http://www.opengis.net/gml/srs/epsg.xml#4326">
                                      <gml:coord><gml:X>-130</gml:X><gml:Y>24</gml:Y></gml:coord>
                                      <gml:coord><gml:X>-55</gml:X><gml:Y>50</gml:Y></gml:coord>
                                   </BoundingBox>
                                   <Output>
                                      <Format>image/png</Format>
                                      <Size><Width>550</Width><Height>250</Height></Size>
                                   </Output>
                                </ogc:GetMap>
                """.trim();
    }
}
