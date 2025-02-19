package de.swm.lhm.geoportal.gateway.portal.authorization;

import de.swm.lhm.geoportal.gateway.portal.BasePortalControllerTest;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.List;

class PortalAuthorizationTest extends BasePortalControllerTest {

    @Test
    void publicPortal() {

        webTestClient.get()
                .uri("/portal/" + PUBLIC_PORTAL)
                .exchange()
                .expectStatus().isPermanentRedirect()
                .expectHeader().location("/portal/" + PUBLIC_PORTAL + "/");

    }

    @Test
    void protectedPortal() {

        WebTestClient.ResponseSpec responseSpec = webTestClient.get()
                .uri("/portal/" + PROTECTED_PORTAL)
                .exchange();
        expectRedirectToKeyCloak(responseSpec);

        webTestClient.mutateWith(
                        keyCloakConfigureGrantedProducts(List.of(PROTECTED_PRODUCT))
                )
                .get()
                .uri("/portal/" + PROTECTED_PORTAL)
                .exchange()
                .expectStatus().isPermanentRedirect()
                .expectHeader().location("/portal/" + PROTECTED_PORTAL + "/");

        webTestClient.mutateWith(
                        keyCloakConfigureGrantedProducts(List.of(PROTECTED_PRODUCT))
                )
                .get()
                .uri("/portal/" + RESTRICTED_PORTAL)
                .exchange()
                .expectStatus().isForbidden();

    }

    @Test
    void protectedAuthLevelHighPortal() {

        WebTestClient.ResponseSpec responseSpec = webTestClient.get()
                .uri("/portal/" + PROTECTED_AUTH_LEVEL_HIGH_PORTAL)
                .exchange();
        expectRedirectToKeyCloak(responseSpec);

        webTestClient.mutateWith(
                        keyCloakConfigureGrantedProducts(List.of(PROTECTED_AUTH_LEVEL_HIGH_PRODUCT), true)
                )
                .get()
                .uri("/portal/" + PROTECTED_AUTH_LEVEL_HIGH_PORTAL)
                .exchange()
                .expectStatus().isPermanentRedirect()
                .expectHeader().location("/portal/" + PROTECTED_AUTH_LEVEL_HIGH_PORTAL + "/");

        webTestClient.mutateWith(
                        keyCloakConfigureGrantedProducts(List.of(PROTECTED_AUTH_LEVEL_HIGH_PRODUCT), true)
                )
                .get()
                .uri("/portal/" + RESTRICTED_PORTAL)
                .exchange()
                .expectStatus().isForbidden();

    }

    @Test
    void restrictedPortal() {

        WebTestClient.ResponseSpec responseSpec = webTestClient.get()
                .uri("/portal/" + RESTRICTED_PORTAL)
                .exchange();
        expectRedirectToKeyCloak(responseSpec);

        webTestClient.mutateWith(
                        keyCloakConfigureGrantedProducts(List.of(RESTRICTED_PRODUCT))
                )
                .get()
                .uri("/portal/" + RESTRICTED_PORTAL)
                .exchange()
                .expectStatus().isPermanentRedirect()
                .expectHeader().location("/portal/" + RESTRICTED_PORTAL + "/");

        webTestClient.mutateWith(
                        keyCloakConfigureGrantedProducts(List.of(RESTRICTED_PRODUCT))
                )
                .get()
                .uri("/portal/" + PUBLIC_PORTAL)
                .exchange()
                .expectStatus().isPermanentRedirect()
                .expectHeader().location("/portal/" + PUBLIC_PORTAL + "/");

    }

}