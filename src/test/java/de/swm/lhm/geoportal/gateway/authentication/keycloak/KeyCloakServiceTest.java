package de.swm.lhm.geoportal.gateway.authentication.keycloak;

import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import de.swm.lhm.geoportal.gateway.shared.exceptions.LoggedResponseStatusException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.containsStringIgnoringCase;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;

@ExtendWith(WireMockExtension.class)
@ExtendWith(OutputCaptureExtension.class)
class KeyCloakServiceTest {

    private KeyCloakService keyCloakService;

    @RegisterExtension
    static WireMockExtension wireMock = WireMockExtension.newInstance()
            .options(WireMockConfiguration.wireMockConfig().dynamicPort())
            .build();

    @BeforeEach
    void setUp() {
        WebClient.Builder webClientBuilder = WebClient.builder();

        KeyCloakProviderProperties keyCloakProviderProperties = new KeyCloakProviderProperties();
        keyCloakProviderProperties.setTokenUri("%s/token".formatted(wireMock.baseUrl()));
        keyCloakProviderProperties.setUserInfoUri("%s/userinfo".formatted(wireMock.baseUrl()));

        KeyCloakRegistrationProperties keyCloakRegistrationProperties = new KeyCloakRegistrationProperties();
        keyCloakRegistrationProperties.setClientId("test-client-id");
        keyCloakRegistrationProperties.setClientSecret("test-client-secret");

        keyCloakService = new KeyCloakService(webClientBuilder, keyCloakProviderProperties, keyCloakRegistrationProperties);
    }

    @Test
    void getClientCredentialsGrantedAccessTokenShouldHandleError(CapturedOutput output) {
        wireMock.stubFor(post(urlEqualTo("/token"))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.INTERNAL_SERVER_ERROR.value())
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"error\":\"internal_server_error\",\"error_description\":\"Internal Server Error\"}")));

        Mono<String> result = keyCloakService.getClientCredentialsGrantedAccessToken();

        StepVerifier.create(result)
                .expectErrorSatisfies(throwable -> {
                    assertThat(throwable, instanceOf(LoggedResponseStatusException.class));
                    LoggedResponseStatusException exception = (LoggedResponseStatusException) throwable;
                    assertThat(exception.getStatusCode(), is(HttpStatus.INTERNAL_SERVER_ERROR));
                    assertThat(exception.getReason(), containsStringIgnoringCase("Server error during token request to Keycloak"));
                })
                .verify();

        assertThat(output.toString(), containsString("Server error during token request to Keycloak"));
        assertThat(output.toString(), containsString("internal_server_error"));
        assertThat(output.toString(), containsString("[Context: getClientCredentialsGrantedAccessToken]"));
    }

    @Test
    void getClientCredentialsGrantedAccessToken() {
        wireMock.stubFor(post(urlEqualTo("/token"))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"access_token\":\"mockAccessToken\"}")));

        keyCloakService.getClientCredentialsGrantedAccessToken()
                .as(StepVerifier::create)
                .expectNext("mockAccessToken")
                .verifyComplete();
    }

    @Test
    void authenticateWithCredentialsShouldReturnAccessToken() {
        wireMock.stubFor(post(urlEqualTo("/token"))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"access_token\":\"mockAccessToken\"}")));

        Mono<LoginResponse> result = keyCloakService.authenticateWithCredentials("test-user", "test-password");

        result.as(StepVerifier::create)
              .expectNextMatches(loginResponse -> "mockAccessToken".equals(loginResponse.getAccessToken()))
              .verifyComplete();
    }

    @Test
    void authenticateWithCredentialsShouldHandleError(CapturedOutput output) {
        wireMock.stubFor(post(urlEqualTo("/token"))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.UNAUTHORIZED.value())
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"error\":\"unauthorized\",\"error_description\":\"Invalid credentials\"}")));

        Mono<LoginResponse> result = keyCloakService.authenticateWithCredentials("wrong-user", "wrong-password");

        result.as(StepVerifier::create)
              .expectErrorSatisfies(throwable -> {
                  assertThat(throwable, instanceOf(LoggedResponseStatusException.class));
                  assertThat(throwable.getMessage(), containsStringIgnoringCase("User wrong-user was not successfully authenticated by keycloak"));
              })
              .verify();

        assertThat(output.toString(), containsStringIgnoringCase("User wrong-user was not successfully authenticated by Keycloak"));
        assertThat(output.toString(), containsString("unauthorized"));
        assertThat(output.toString(), containsString("[Context: authenticateWithCredentials]"));
    }

    @Test
    void getUserInfoShouldReturnUserInfo() {
        wireMock.stubFor(get(urlEqualTo("/userinfo"))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"sub\":\"test-user\",\"user_name\":\"Test User\"}")));

        Mono<UserInfo> result = keyCloakService.getUserInfo("mockAccessToken");

        result.as(StepVerifier::create)
              .expectNextMatches(userInfo -> "test-user".equals(userInfo.getSubject()) && "Test User".equals(userInfo.getUserName()))
              .verifyComplete();
    }

    @Test
    void getUserInfoShouldHandleError(CapturedOutput output) {
        wireMock.stubFor(get(urlEqualTo("/userinfo"))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.UNAUTHORIZED.value())
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"error\":\"unauthorized\",\"error_description\":\"Invalid token\"}")));

        Mono<UserInfo> result = keyCloakService.getUserInfo("invalidAccessToken");

        result.as(StepVerifier::create)
                .expectNextMatches(userInfo -> userInfo.getSubject() == null && userInfo.getUserName() == null)
                .verifyComplete();

        assertThat(output.toString(), containsStringIgnoringCase("Failed to fetch user infos from Keycloak"));
        assertThat(output.toString(), containsString("unauthorized"));
        assertThat(output.toString(), containsString("[Context: getUserInfo]"));
    }

    @Test
    void authenticateAndGetPrincipalShouldReturnPrincipal() {
        wireMock.stubFor(post(urlEqualTo("/token"))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"access_token\":\"mockAccessToken\"}")));

        wireMock.stubFor(get(urlEqualTo("/userinfo"))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"sub\":\"test-user\",\"name\":\"Test User\"}")));

        Mono<Authentication> result = keyCloakService.authenticateAndGetPrincipal("test-user", "test-password");

        result.as(StepVerifier::create)
              .expectNextMatches(principal -> "test-user".equals(principal.getName()))
              .verifyComplete();
    }

    @Test
    void handleInternalServerErrorLogMessage(CapturedOutput output) {
        wireMock.stubFor(get(urlEqualTo("/userinfo"))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.INTERNAL_SERVER_ERROR.value())
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"error\":\"internal_server_error\",\"error_description\":\"Internal Server Error\"}")));

        Mono<UserInfo> result = keyCloakService.getUserInfo("invalidAccessToken");

        StepVerifier.create(result)
              .expectNextMatches(userInfo -> userInfo.getSubject() == null && userInfo.getUserName() == null)
              .verifyComplete();

        assertThat(output.toString(), containsString("Server error when fetching user infos from Keycloak"));
        assertThat(output.toString(), containsString("Internal Server Error"));
        assertThat(output.toString(), containsString("[Context: getUserInfo]"));
    }

}