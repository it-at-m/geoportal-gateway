package de.swm.lhm.geoportal.gateway.geoservice.authorization;

import com.github.tomakehurst.wiremock.stubbing.StubMapping;
import de.swm.lhm.geoportal.gateway.geoservice.AbstractGeoServiceTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

@TestPropertySource(properties = {
        "spring.security.oauth2.client.provider.keycloak.token-uri=http://localhost:" + AbstractGeoServiceTest.WIREMOCK_PORT + "/token",
        "spring.security.oauth2.client.provider.keycloak.user-info-uri=http://localhost:" + AbstractGeoServiceTest.WIREMOCK_PORT + "/user-info"
})
class GeoServiceHttpBasicAuthTest extends AbstractGeoServiceTest {

    @Test
    void successfulBasicAuthWithAuthorized() {
        mockGeoServer();

        withAuthenticatingKeyCloakEndpoints(() -> {
            WebTestClient.ResponseSpec responseSpec = webTestClient.get()
                    .uri("/geoserver/wms?SERVICE=WMS&layers=" + PROTECTED_LAYER)
                    .headers(headers -> headers.setBasicAuth("someuser", "somepassword"))
                    .exchange();
            expectGeoServerResponse(responseSpec);
        });
    }

    @Test
    void successfulBasicAuthWithNotAuthorized() {
        mockGeoServer();

        withAuthenticatingKeyCloakEndpoints(() -> {
            webTestClient.get()
                    .uri("/geoserver/wms?SERVICE=WMS&layers=" + RESTRICTED_LAYER)
                    .headers(headers -> headers.setBasicAuth("someuser", "somepassword"))
                    .exchange()
                    .expectStatus()
                    .isForbidden();
        });
    }

    @Test
    void deniedBasicAuth() {
        mockGeoServer();
        withDenyingKeyCloakEndpoints(() -> {
            WebTestClient.ResponseSpec responseSpec = webTestClient.get()
                    .uri("/geoserver/wms?SERVICE=WMS&layers=" + PROTECTED_LAYER)
                    .headers(headers -> headers.setBasicAuth("someuser", "somepassword"))
                    .exchange();
            
            responseSpec.expectStatus().isUnauthorized(); // todo: forbidden would be more according to the spec
        });
    }

    @Test
    void deniedByAuthLevel() {
        mockGeoServer();
        withAuthenticatingKeyCloakEndpoints(() -> {
            // http-basic auth does not meet the requirements for auth-level HIGH, so this shall be denied
            // although the user is allowed to access the produkt
            WebTestClient.ResponseSpec responseSpec = webTestClient.get()
                    .uri("/geoserver/wms?SERVICE=WMS&layers=" + PROTECTED_AUTH_LEVEL_HIGH_LAYER)
                    .headers(headers -> headers.setBasicAuth("someuser", "somepassword"))
                    .exchange();

            responseSpec.expectStatus().isForbidden();
        });
    }

    /**
     * Mocks some keycloak endpoints using wiremock as KeyCloakMock does not support this functionality.
     *
     * This essentially allows all user/password combinations and grands them access to a fixed set of
     * products
     */
    void withAuthenticatingKeyCloakEndpoints(Runnable action) {
        StubMapping tokenStubMapping = stubFor(post(urlPathMatching("/token"))
                .willReturn(aResponse()
                        .withBody(
                                """
                                        {
                                          "access_token": "whatever",
                                          "expires_in": 18000,
                                          "refresh_expires_in": 1800,
                                          "refresh_token": "alsowhatever",
                                          "token_type": "bearer",
                                          "not-before-policy": 1697729421,
                                          "session_state": "e8eae1b2-ccae-46be-b6a8-4cb2a84b8b9d",
                                          "scope": "profile email"
                                        }
                                        """
                        )
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withStatus(HttpStatus.OK.value())
                )
        );

        StubMapping userInfoStubMapping = stubFor(get(urlPathMatching("/user-info"))
                .willReturn(aResponse()
                        .withBody("{\"authorities\": [\"" + PROTECTED_PRODUCT + "\", \"" + PROTECTED_AUTH_LEVEL_HIGH_PRODUCT + "\"] }")
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withStatus(HttpStatus.OK.value())
                )
        );

        try {
            action.run();
        } finally {
            removeStub(tokenStubMapping);
            removeStub(userInfoStubMapping);
        }
    }

    /**
     * Mocks some keycloak endpoints using wiremock as KeyCloakMock does not support this functionality.
     *
     * This essentially denies all user/password combinations.
     */
    void withDenyingKeyCloakEndpoints(Runnable action) {
        StubMapping tokenStubMapping = stubFor(post(urlPathMatching("/token"))
                .willReturn(aResponse()
                        .withBody("{}")
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withStatus(HttpStatus.FORBIDDEN.value())
                )
        );
        try {
            action.run();
        } finally {
            removeStub(tokenStubMapping);
        }
    }
}
