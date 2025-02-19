package de.swm.lhm.geoportal.gateway.authentication.logout;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.server.WebFilterExchange;
import org.springframework.security.web.server.authentication.logout.ServerLogoutSuccessHandler;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;


@RequiredArgsConstructor
@Component
public class KeycloakRedirectLogoutSuccessHandler implements ServerLogoutSuccessHandler {

    private final KeycloakServerRedirectStrategy redirectStrategy;

    @Override
    public Mono<Void> onLogoutSuccess(WebFilterExchange exchange, Authentication authentication) {
		return this.redirectStrategy.sendRedirect(exchange.getExchange());
    }

}
