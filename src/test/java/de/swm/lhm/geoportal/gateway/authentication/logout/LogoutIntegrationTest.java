package de.swm.lhm.geoportal.gateway.authentication.logout;

import com.tngtech.keycloakmock.api.ServerConfig;
import com.tngtech.keycloakmock.junit5.KeycloakMockExtension;
import de.swm.lhm.geoportal.gateway.authentication.LoginLogoutProperties;
import de.swm.lhm.geoportal.gateway.authentication.LoginLogoutProperties.LoginSuccess;
import de.swm.lhm.geoportal.gateway.authentication.LoginLogoutProperties.LogoutRedirect;
import de.swm.lhm.geoportal.gateway.authorization.IAuthService;
import de.swm.lhm.geoportal.gateway.base_classes.config.TestSessionConfig;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.web.server.WebFilterExchange;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static de.swm.lhm.geoportal.gateway.authentication.logout.KeycloakServerRedirectStrategy.ID_TOKEN_ERROR_MESSAGE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ExtendWith(SpringExtension.class)
@ExtendWith({OutputCaptureExtension.class})
@ActiveProfiles("test")
@AutoConfigureWebTestClient
@Import({LogoutIntegrationTest.TestConfig.class, TestSessionConfig.class})
class LogoutIntegrationTest {

    @Autowired
    protected WebTestClient webTestClient;

    @Autowired
    private LoginLogoutProperties loginLogoutProperties;

    @MockBean
    private IAuthService authorizationService;

    @RegisterExtension
    static KeycloakMockExtension mockKeyCloak =
            new KeycloakMockExtension(
                    ServerConfig.aServerConfig()
                            .withNoContextPath()
                            .withPort(9089)
                            .withDefaultRealm("public")
                            .build());

    static Stream<ConfigurationAndToken> provideConfigurationsAndTokens() {
        return Stream.of(
                new ConfigurationAndToken(new LogoutRedirect("/test-endpoint1", "test_redirect1", true, "test_token1"), "test_token"),
                new ConfigurationAndToken(new LogoutRedirect("/test-endpoint1", "test_redirect1", true, "test_token1"), null),
                new ConfigurationAndToken(new LogoutRedirect("/test-endpoint2", "test_redirect2", false, "test_token2"), "test_token"),
                new ConfigurationAndToken(new LogoutRedirect("/test-endpoint2", "test_redirect2", false, "test_token2"), null)
        );
    }

    @ParameterizedTest
    @MethodSource("provideConfigurationsAndTokens")
    void shouldRedirectToKeycloakWithDifferentConfigs(ConfigurationAndToken configurationAndToken, CapturedOutput capturedOutput) {

        LogoutRedirect logoutRedirect = configurationAndToken.logoutRedirect;
        String tokenValue = configurationAndToken.tokenValue;

        Mono<String> tokenMono = Mono.justOrEmpty(tokenValue);

        lenient().when(authorizationService.getIdTokenValue()).thenReturn(tokenMono);
        lenient().when(authorizationService.getIdTokenValue(any(WebFilterExchange.class))).thenReturn(tokenMono);
        lenient().when(authorizationService.getIdTokenValue(any(ServerWebExchange.class))).thenReturn(tokenMono);

        // Set dynamic properties
        loginLogoutProperties.setLogoutRedirect(logoutRedirect);

        // Perform the logout operation and capture the response
        WebTestClient.ResponseSpec responseSpec = webTestClient.mutate()
                .build()
                .get()
                .uri("/logout")
                .exchange()
                .expectStatus().isFound();

        // Extract the Location header from the response
        URI locationHeader = responseSpec.returnResult(String.class)
                .getResponseHeaders()
                .getLocation();

        assertThat(locationHeader, is(not(nullValue())));

        // Parse the Location URI
        UriComponents uriComponents = UriComponentsBuilder.fromUri(locationHeader).build();

        // Extract query parameters
        Map<String, List<String>> queryParams = uriComponents.getQueryParams();

        if (logoutRedirect.isSendIdToken() && tokenValue != null) {
            assertThat(queryParams, hasKey(logoutRedirect.getIdTokenKey()));
            List<String> tokens = queryParams.get(logoutRedirect.getIdTokenKey());
            assertThat(tokens, hasSize(1));
            assertThat(tokens, hasItem(equalTo(tokenValue)));
        } else if (logoutRedirect.isSendIdToken() && tokenValue == null) {
            assertThat(queryParams, not(hasKey(logoutRedirect.getIdTokenKey())));
            assertThat(capturedOutput.getAll(), containsString(ID_TOKEN_ERROR_MESSAGE));
        } else {
            assertThat(queryParams, not(hasKey(logoutRedirect.getIdTokenKey())));
        }

        // Verify that the getIdTokenValue was called with exchange
        verify(authorizationService,  atMost(2)).getIdTokenValue(any(WebFilterExchange.class));


    }

    @TestConfiguration
    static class TestConfig {
        @Bean
        public LoginLogoutProperties loginLogoutProperties() {
            // Configure default mock properties here
            LoginLogoutProperties loginLogoutProperties = new LoginLogoutProperties();
            loginLogoutProperties.setLogincheckEndpoint("/test-login-check");
            loginLogoutProperties.setLoginSuccess(new LoginSuccess("/login-success", "<html></html>"));
            return loginLogoutProperties;
        }
    }

    record ConfigurationAndToken(LogoutRedirect logoutRedirect, String tokenValue) {
    }

}