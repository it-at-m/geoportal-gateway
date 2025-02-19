package de.swm.lhm.geoportal.gateway.geoservice.filter;

import com.github.tomakehurst.wiremock.stubbing.StubMapping;
import de.swm.lhm.geoportal.gateway.geoservice.inspect.QualifiedLayerName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.io.IOException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;

class DescribeFeatureTypeGatewayFilterTest extends AbstractGeoServiceGatewayFilterTest {

    @Test
    void filterWorkspaceDocumentWorkspaceQualified() throws IOException {
        filterDocument(
                "/geoserver/myws/wfs?request=DescribeFeatureType&service=WFS",
                "geoservice/describe-feature-type/workspace-myws.xsd"
        );
    }

    @Test
    void filterTopLevelDocument() throws IOException {
        filterDocument(
                "/geoserver/wfs?request=DescribeFeatureType&service=WFS",
                "geoservice/describe-feature-type/top-level-wfs-entrypoint.xsd"
        );
    }

    @Test
    void filterWorkspaceDocumentNonWorkspaceQualifiedPath() throws IOException {
        StubMapping stub = serveXmlDocument("geoservice/describe-feature-type/workspace-myws.xsd", true);

        WebTestClient.ResponseSpec responseSpec = webTestClient
                .get()
                .uri(
                        "/geoserver/wfs?request=DescribeFeatureType&service=WFS&typename="
                                + PUBLIC_LAYER
                                + ","
                                + RESTRICTED_LAYER
                )
                .exchange();

        // directly referencing the restricted layer requires auth & authn
        expectRedirectToKeyCloak(responseSpec);
    }


    void filterDocument(String geoserverPath, String capabilitiesDocumentResource) throws IOException {
        StubMapping stub = serveXmlDocument(capabilitiesDocumentResource, true);

        WebTestClient.ResponseSpec responseSpec = webTestClient
                .get()
                .uri(geoserverPath)
                .exchange();

        responseSpec.expectHeader()
                .valueEquals(HttpHeaders.SERVER, GEOSERVER_SERVER_HEADER);

        responseSpec.expectBody(String.class)
                .consumeWith(result -> {
                    String body = result.getResponseBody();

                    // only test on the layernames to avoid messing with urlencoded names
                    // and find referencing XSD complex-types

                    QualifiedLayerName qPublicLayer = QualifiedLayerName.fromString(PUBLIC_LAYER);
                    assertThat(body, containsString(qPublicLayer.layerName()));

                    QualifiedLayerName qRestrictedLayer = QualifiedLayerName.fromString(RESTRICTED_LAYER);
                    assertThat(body, not(containsString(qRestrictedLayer.layerName())));
                });
    }
}
