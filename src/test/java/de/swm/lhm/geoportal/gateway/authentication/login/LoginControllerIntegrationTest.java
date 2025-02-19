package de.swm.lhm.geoportal.gateway.authentication.login;

import com.tngtech.keycloakmock.api.ServerConfig;
import com.tngtech.keycloakmock.junit5.KeycloakMockExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@RunWith(SpringRunner.class)
@ActiveProfiles("test")
@AutoConfigureWebTestClient
class LoginControllerIntegrationTest {

    @Autowired
    protected WebTestClient webTestClient;

    @RegisterExtension
    static KeycloakMockExtension mockKeyCloak =
            new KeycloakMockExtension(
                    ServerConfig.aServerConfig()
                            .withNoContextPath()
                            .withPort(9089)
                            .withDefaultRealm("public")
                            .build());

    @Test
    @WithMockUser
    void shouldRedirectToKeycloakIfLoggedInUser() {
        webTestClient.get()
                .uri("/login")
                .exchange()
                .expectStatus().isFound()
                .expectHeader().location("/oauth2/authorization/keycloak");
    }

    @Test
    @WithAnonymousUser
    void shouldRedirectToKeycloakIfAnonymousUser() {
        webTestClient.get()
                .uri("/login")
                .exchange()
                .expectStatus().isFound()
                .expectHeader().location("/oauth2/authorization/keycloak");
    }

    @Test
    @WithMockUser
    void logincheckShouldRespondWithOKWhenLoggedIn() {
        webTestClient.get().uri("/logincheck").exchange().expectStatus().isOk();
    }

    @Test
    @WithAnonymousUser
    void logincheckShouldRespondWithForbiddenWhenLoggedIn() {
        webTestClient.get().uri("/logincheck").exchange().expectStatus().isForbidden();
    }

}
