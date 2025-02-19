package de.swm.lhm.geoportal.gateway.authentication.login;

import de.swm.lhm.geoportal.gateway.shared.GatewayService;
import jakarta.annotation.Nonnull;
import org.springframework.core.ResolvableType;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatcher;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatchers;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import static org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestRedirectFilter.DEFAULT_AUTHORIZATION_REQUEST_BASE_URI;


public class LoginPageFilter implements WebFilter {

    private final ServerWebExchangeMatcher matcher;
    private final URI loginRedirectUrl;

    public LoginPageFilter() {
        this.matcher = ServerWebExchangeMatchers.pathMatchers(HttpMethod.GET, "/login");
        this.loginRedirectUrl = URI.create(getLoginRedirectUrl());
    }

    private String getLoginRedirectUrl(){
        Map<String, String> oAuthLinks = getOAuthLinks();
        if (oAuthLinks.containsKey("keycloak"))
            return oAuthLinks.get("keycloak");
        throw new IllegalArgumentException("could not detect url to keycloak login");
    }

    private Map<String, String> getOAuthLinks() {

        // code is a copy of org.springframework.security.config.web.server.ServerHttpSecurity.OAuth2LoginSpec.getLinks

        Map<String, String> result = new HashMap<>();

        String[] names = GatewayService.getApplicationContext().getBeanNamesForType(ResolvableType.forClassWithGenerics(Iterable.class, ClientRegistration.class));
        Object potRegistrations = GatewayService.getApplicationContext().getBean(names[0]);

        if (potRegistrations instanceof Iterable<?> registrations) {
            registrations.iterator()
                    .forEachRemaining(r -> {
                        if (
                                r instanceof ClientRegistration clientRegistration
                                        && AuthorizationGrantType.AUTHORIZATION_CODE.equals(clientRegistration.getAuthorizationGrantType())
                        ) {
                            result.put(
                                    clientRegistration.getRegistrationId(),
                                    // path is taken from org.springframework.security.config.web.server.ServerHttpSecurity.OAuth2LoginSpec.getLinks
                                    // and can also be found in
                                    // - org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestRedirectFilter.DEFAULT_AUTHORIZATION_REQUEST_BASE_URI
                                    // - org.springframework.security.config.http.OAuth2LoginBeanDefinitionParser.DEFAULT_AUTHORIZATION_REQUEST_BASE_URI
                                    // See https://github.com/spring-projects/spring-security/blob/ffd4a0ff574e060738fe495c2ee368510b254fcc/docs/modules/ROOT/pages/servlet/oauth2/login/advanced.adoc#oauth-20-login-page
                                    "%s/%s".formatted(DEFAULT_AUTHORIZATION_REQUEST_BASE_URI, clientRegistration.getRegistrationId())
                            );
                        }
                    });
        }

        return result;
    }

    @Override
    @Nonnull
    public Mono<Void> filter(@Nonnull ServerWebExchange exchange, WebFilterChain chain) {

        // copied from org.springframework.security.web.server.ui.LoginPageGeneratingWebFilter.filter

        return this.matcher.matches(exchange)
                .filter(ServerWebExchangeMatcher.MatchResult::isMatch)
                .switchIfEmpty(chain.filter(exchange).then(Mono.empty()))
                .flatMap(result -> this.createRedirect(exchange));
    }

    private Mono<Void> createRedirect(ServerWebExchange exchange) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.FOUND);
        response.getHeaders().setLocation(loginRedirectUrl);
        return response.setComplete();
    }


}
