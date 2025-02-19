package de.swm.lhm.geoportal.gateway.authentication.logout;

import de.swm.lhm.geoportal.gateway.authentication.LoginLogoutProperties;
import de.swm.lhm.geoportal.gateway.authentication.keycloak.KeyCloakProviderProperties;
import de.swm.lhm.geoportal.gateway.shared.GatewayService;
import de.swm.lhm.geoportal.gateway.util.ExtendedURIBuilder;
import de.swm.lhm.geoportal.gateway.util.TokenUtils;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.utils.URIBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.net.URISyntaxException;

import static de.swm.lhm.geoportal.gateway.authentication.logout.LogoutParameters.ID_TOKEN_ATTRIBUTE_KEY;


@Slf4j
@RequiredArgsConstructor
@Component
public class KeycloakServerRedirectStrategy {

    static final HttpStatus HTTP_STATUS = HttpStatus.FOUND;
    static final String ID_TOKEN_ERROR_MESSAGE = "Could not obtain id token from authorizationService.getTokenValue()";

    private final KeyCloakProviderProperties keycloakProviderProperties;
    private final GatewayService gatewayService;
    private final LoginLogoutProperties loginLogoutProperties;

    /**
     * Handles the redirect for Keycloak logout.
     *
     * @param exchange The {@link ServerWebExchange} object.
     * @return A {@link Mono} signaling completion.
     */
    public Mono<Void> sendRedirect(ServerWebExchange exchange) {
        Assert.notNull(exchange, "exchange cannot be null");

        return buildUrl(exchange)
                .flatMap(redirectUri -> {
                    ServerHttpResponse response = exchange.getResponse();
                    response.setStatusCode(HTTP_STATUS);
                    logUri(redirectUri);
                    response.getHeaders().setLocation(redirectUri);
                    return response.setComplete();
                })
                .doOnError(error -> log.error("Error during redirect", error));
    }

    /**
     * Constructs the Keycloak logout URL.
     *
     * @return A {@link Mono} with the constructed {@link URI}.
     */
    Mono<URI> buildUrl(ServerWebExchange exchange) {

        try {
            String redirectUrl = new ExtendedURIBuilder(gatewayService.getExternalUrl())
                    .addPath(loginLogoutProperties.getLogoutRedirect().getEndpoint())
                    .build()
                    .toString();

            URIBuilder keycloakLogoutUrl = new ExtendedURIBuilder(keycloakProviderProperties.getLogoutUri())
                    .addParameter(loginLogoutProperties.getLogoutRedirect().getUriKey(), redirectUrl);

            if (loginLogoutProperties.getLogoutRedirect().isSendIdToken()) {
                return addIdToken(keycloakLogoutUrl, exchange)
                        .map(this::buildUri);
            } else {
                return Mono.just(buildUri(keycloakLogoutUrl));
            }
        } catch (Exception e) {
            return Mono.error(e);
        }

    }

    /**
     * Adds the ID token to the logout URL.
     *
     * @param uriBuilder The {@link URIBuilder} object.
     * @return A {@link Mono} with the updated {@link URIBuilder}.
     */
    private Mono<URIBuilder> addIdToken(URIBuilder uriBuilder, ServerWebExchange exchange) {
        return Mono.defer(() -> {
            String idToken = exchange.getAttribute(ID_TOKEN_ATTRIBUTE_KEY);

            if (StringUtils.isBlank(idToken)) {
                log.error(ID_TOKEN_ERROR_MESSAGE);
            } else {
                uriBuilder.addParameter(
                        loginLogoutProperties.getLogoutRedirect().getIdTokenKey(),
                        idToken
                );
            }

            return Mono.just(uriBuilder);
        });
    }

    /**
     * Helper method to safely build a URI from URIBuilder.
     *
     * @param uriBuilder The {@link URIBuilder} to build.
     * @return The generated {@link URI}.
     */
    @SneakyThrows
    private URI buildUri(URIBuilder uriBuilder) {
        return uriBuilder.build();
    }

    /**
     * Logs the provided URI at the debug level, with masking to hide sensitive information.
     * If the URI contains parameters with the ID token key, the ID token will be masked to
     * prevent sensitive information from being logged.
     * <p>
     * This method first checks if debug logging is enabled. If it is, it attempts to mask
     * the ID token in the URI and logs the masked URI. If there is an error during the
     * masking process (e.g., invalid URI syntax), an error message is logged.
     *
     * @param redirectUri The URI to be masked and logged.
     */
    private void logUri(URI redirectUri) {
        if (log.isDebugEnabled()) {
            try {
                URI maskedUri = TokenUtils.maskUri(redirectUri, loginLogoutProperties.getLogoutRedirect().getIdTokenKey());
                log.debug("Sending redirect to {}", maskedUri);
            } catch (URISyntaxException e) {
                log.error("Error masking ID Token in URL", e);
            }
        }
    }
}