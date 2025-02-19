package de.swm.lhm.geoportal.gateway.exception;

import com.tngtech.keycloakmock.api.ServerConfig;
import com.tngtech.keycloakmock.junit5.KeycloakMockExtension;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.List;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@RunWith(SpringRunner.class)
@ActiveProfiles("test")
@AutoConfigureWebTestClient
class GlobalExceptionHandlerIntegrationTest {

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
    void shouldShowErrorPageForInternalServerError() {
        webTestClient
                .get()
                .uri("/portal/idontexist")
                .headers(httpHeaders -> httpHeaders.setAccept(List.of(MediaType.TEXT_HTML)))
                .exchange()
                .expectStatus()
                .is5xxServerError()
                .expectHeader().contentType(MediaType.TEXT_HTML)
                .expectBody(String.class)
                .consumeWith(body -> Assertions.assertThat(body.getResponseBody()).contains("Something unexpected happened"));
    }

    @Test
    void shouldShowJsonForInternalServerError() {
        webTestClient
                .get()
                .uri("/portal/idontexist")
                .headers(httpHeaders -> httpHeaders.setAccept(List.of(MediaType.APPLICATION_JSON)))
                .exchange()
                .expectStatus()
                .is5xxServerError()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody(String.class)
                .consumeWith(body -> Assertions.assertThat(body.getResponseBody()).contains("Internal Server Error"));
    }

    @Test
    void shouldShowErrorPageForForbiddenError() {
        webTestClient.get()
                .uri("/resource/icon/../../application-test.properties")
                .headers(httpHeaders -> httpHeaders.setAccept(List.of(MediaType.TEXT_HTML)))
                .exchange()
                .expectStatus().isForbidden()
                .expectBody(String.class).consumeWith(body -> Assertions.assertThat(body.getResponseBody()).contains("FORBIDDEN"));
    }

    @Test
    void shouldShowNotFoundErrorPage() {
        webTestClient.get()
                .uri("/resource/legend/non-existing/path")
                .headers(httpHeaders -> httpHeaders.setAccept(List.of(MediaType.TEXT_HTML)))
                .exchange()
                .expectStatus().isNotFound()
                .expectBody(String.class).consumeWith(body -> Assertions.assertThat(body.getResponseBody()).contains("Sorry but the page you are looking for does not exist"));
    }

}
