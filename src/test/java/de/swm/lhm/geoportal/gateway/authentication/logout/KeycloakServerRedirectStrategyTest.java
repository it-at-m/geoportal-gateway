package de.swm.lhm.geoportal.gateway.authentication.logout;

import de.swm.lhm.geoportal.gateway.authentication.LoginLogoutProperties;
import de.swm.lhm.geoportal.gateway.authentication.keycloak.KeyCloakProviderProperties;
import de.swm.lhm.geoportal.gateway.shared.GatewayService;
import org.apache.http.client.utils.URIBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static de.swm.lhm.geoportal.gateway.authentication.logout.KeycloakServerRedirectStrategy.HTTP_STATUS;
import static de.swm.lhm.geoportal.gateway.authentication.logout.KeycloakServerRedirectStrategy.ID_TOKEN_ERROR_MESSAGE;
import static de.swm.lhm.geoportal.gateway.authentication.logout.LogoutParameters.ID_TOKEN_ATTRIBUTE_KEY;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;


@ExtendWith(OutputCaptureExtension.class)
class KeycloakServerRedirectStrategyTest {

    @Mock
    private KeyCloakProviderProperties keycloakProviderProperties;

    @Mock
    private GatewayService gatewayService;

    @Mock
    private LoginLogoutProperties loginLogoutProperties;

    @Mock
    private ServerWebExchange exchange;

    @Mock
    private ServerHttpResponse response;

    @Mock
    private HttpHeaders headers;

    @InjectMocks
    private KeycloakServerRedirectStrategy keycloakServerRedirectStrategy;

    private static final String REDIRECT_URI = "http://localhost/logout/redirect";
    private static final String FAKE_ID_TOKEN = "fake-id-token";

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);

        when(exchange.getResponse()).thenReturn(response);
        when(response.getHeaders()).thenReturn(headers);
        when(response.setComplete()).thenReturn(Mono.empty());

        // Configure the headers mock to handle setLocation
        doAnswer(invocation -> {
            URI uri = invocation.getArgument(0);
            when(headers.getLocation()).thenReturn(uri);
            return null;
        }).when(headers).setLocation(any(URI.class));
    }

    @Test
    void sendRedirect_ShouldRedirectSuccessfully() {
        // Arrange
        URI externalUrl = URI.create("http://localhost");
        URI logoutUri = URI.create("http://keycloak/logout");

        LoginLogoutProperties.LogoutRedirect logoutRedirect = new LoginLogoutProperties.LogoutRedirect();
        logoutRedirect.setEndpoint("/logout/redirect");
        logoutRedirect.setUriKey("redirect_uri");

        when(gatewayService.getExternalUrl()).thenReturn(externalUrl.toString());
        when(keycloakProviderProperties.getLogoutUri()).thenReturn(logoutUri.toString());
        when(loginLogoutProperties.getLogoutRedirect()).thenReturn(logoutRedirect);

        // Act
        Mono<Void> result = keycloakServerRedirectStrategy.sendRedirect(exchange);

        // Assert
        StepVerifier.create(result)
                .expectSubscription()
                .verifyComplete();

        verify(response).setStatusCode(HTTP_STATUS);
        verify(headers).setLocation(any(URI.class));

        // Capture and inspect the actual URI set in the location header
        ArgumentCaptor<URI> uriCaptor = ArgumentCaptor.forClass(URI.class);
        verify(headers).setLocation(uriCaptor.capture());
        URI actualUri = uriCaptor.getValue();

        assertThat(actualUri, is(notNullValue()));
        assertThat(actualUri.getScheme(), is(equalTo(logoutUri.getScheme())));
        assertThat(actualUri.getHost(), is(equalTo(logoutUri.getHost())));
        assertThat(actualUri.getPath(), is(equalTo(logoutUri.getPath())));

        // Validate query parameters
        Map<String, List<String>> queryParams = parseQueryParams(actualUri);
        assertThat(queryParams, hasKey(logoutRedirect.getUriKey()));
        List<String> redirectUris = queryParams.get(logoutRedirect.getUriKey());
        assertThat(redirectUris, hasSize(1));
        assertThat(redirectUris, hasItem(equalTo(REDIRECT_URI)));

    }

    @Test
    void sendRedirect_ShouldHandleIdToken() {

        // Arrange
        URI externalUrl = URI.create("http://localhost");
        URI logoutUri = URI.create("http://keycloak/logout");

        LoginLogoutProperties.LogoutRedirect logoutRedirect = new LoginLogoutProperties.LogoutRedirect();
        logoutRedirect.setEndpoint("/logout/redirect");
        logoutRedirect.setUriKey("redirect_uri");
        logoutRedirect.setIdTokenKey("id_token");
        logoutRedirect.setSendIdToken(true);

        when(gatewayService.getExternalUrl()).thenReturn(externalUrl.toString());
        when(keycloakProviderProperties.getLogoutUri()).thenReturn(logoutUri.toString());
        when(loginLogoutProperties.getLogoutRedirect()).thenReturn(logoutRedirect);
        when(exchange.getAttribute(ID_TOKEN_ATTRIBUTE_KEY)).thenReturn(FAKE_ID_TOKEN);

        // Act
        Mono<Void> result = keycloakServerRedirectStrategy.sendRedirect(exchange);

        // Assert
        StepVerifier.create(result)
                .expectSubscription()
                .verifyComplete();

        verify(response).setStatusCode(HTTP_STATUS);
        verify(headers).setLocation(any(URI.class));

        // Capture and inspect the actual URI set in the location header
        ArgumentCaptor<URI> uriCaptor = ArgumentCaptor.forClass(URI.class);
        verify(headers).setLocation(uriCaptor.capture());
        URI actualUri = uriCaptor.getValue();

        assertThat(actualUri, is(notNullValue()));
        assertThat(actualUri.getScheme(), is(equalTo(logoutUri.getScheme())));
        assertThat(actualUri.getHost(), is(equalTo(logoutUri.getHost())));
        assertThat(actualUri.getPath(), is(equalTo(logoutUri.getPath())));

        // Validate query parameters
        Map<String, List<String>> queryParams = parseQueryParams(actualUri);

        assertThat(queryParams, hasKey(logoutRedirect.getUriKey()));
        List<String> redirectUris = queryParams.get(logoutRedirect.getUriKey());
        assertThat(redirectUris, hasSize(1));
        assertThat(redirectUris, hasItem(equalTo(REDIRECT_URI)));

        assertThat(queryParams, hasKey(logoutRedirect.getIdTokenKey()));
        List<String> idTokens = queryParams.get(logoutRedirect.getIdTokenKey());
        assertThat(idTokens, hasSize(1));
        assertThat(idTokens, hasItem(equalTo(FAKE_ID_TOKEN)));

    }

    @Test
    void buildUrl_ShouldThrowExceptionOnURISyntaxException() {
        // Arrange
        URI invalidUri = URI.create("//invalid-uri");  // Deliberately crafted to cause URISyntaxException

        when(gatewayService.getExternalUrl()).thenReturn(invalidUri.toString());
        when(keycloakProviderProperties.getLogoutUri()).thenReturn(invalidUri.toString());

        // Act & Assert
        Mono<URI> result = keycloakServerRedirectStrategy.buildUrl(exchange);

        StepVerifier.create(result)
                .expectError()
                .verify();
    }

    @Test
    void sendRedirect_ShouldHandleTokenNull(CapturedOutput capturedOutput) {

        // Arrange
        URI externalUrl = URI.create("http://localhost");
        URI logoutUri = URI.create("http://keycloak/logout");

        LoginLogoutProperties.LogoutRedirect logoutRedirect = new LoginLogoutProperties.LogoutRedirect();
        logoutRedirect.setEndpoint("/logout/redirect");
        logoutRedirect.setUriKey("redirect_uri");
        logoutRedirect.setIdTokenKey("id_token");
        logoutRedirect.setSendIdToken(true);

        when(gatewayService.getExternalUrl()).thenReturn(externalUrl.toString());
        when(keycloakProviderProperties.getLogoutUri()).thenReturn(logoutUri.toString());
        when(loginLogoutProperties.getLogoutRedirect()).thenReturn(logoutRedirect);

        when(exchange.getAttribute(ID_TOKEN_ATTRIBUTE_KEY)).thenReturn(null);

        // Act
        Mono<Void> result = keycloakServerRedirectStrategy.sendRedirect(exchange);

        // Assert
        StepVerifier.create(result)
                .expectSubscription()
                .verifyComplete();

        verify(response).setStatusCode(HTTP_STATUS);
        verify(headers).setLocation(any(URI.class));

        // Capture and inspect the actual URI set in the location header
        ArgumentCaptor<URI> uriCaptor = ArgumentCaptor.forClass(URI.class);
        verify(headers).setLocation(uriCaptor.capture());
        URI actualUri = uriCaptor.getValue();

        assertThat(actualUri, is(notNullValue()));
        assertThat(actualUri.getScheme(), is(equalTo(logoutUri.getScheme())));
        assertThat(actualUri.getHost(), is(equalTo(logoutUri.getHost())));
        assertThat(actualUri.getPath(), is(equalTo(logoutUri.getPath())));

        // Validate query parameters
        Map<String, List<String>> queryParams = parseQueryParams(actualUri);

        assertThat(queryParams, hasKey(logoutRedirect.getUriKey()));
        List<String> redirectUris = queryParams.get(logoutRedirect.getUriKey());
        assertThat(redirectUris, hasSize(1));
        assertThat(redirectUris, hasItem(equalTo(REDIRECT_URI)));

        assertThat(queryParams, not(hasKey(logoutRedirect.getIdTokenKey())));
        assertThat(capturedOutput.getAll(), containsString(ID_TOKEN_ERROR_MESSAGE));
    }


    /**
     * Helper method to parse query parameters from a URI.
     *
     * @param uri The {@link URI} to parse.
     * @return A map of query parameters and their values.
     */
    private Map<String, List<String>> parseQueryParams(URI uri) {
        return new URIBuilder(uri)
                .getQueryParams()
                .stream()
                .collect(Collectors.groupingBy(
                        org.apache.http.NameValuePair::getName,
                        Collectors.mapping(org.apache.http.NameValuePair::getValue, Collectors.toList())
                ));
    }
}