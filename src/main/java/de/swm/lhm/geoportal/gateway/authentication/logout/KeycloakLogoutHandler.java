package de.swm.lhm.geoportal.gateway.authentication.logout;

import de.swm.lhm.geoportal.gateway.authorization.IAuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;

import org.springframework.security.web.server.WebFilterExchange;
import org.springframework.security.web.server.authentication.logout.SecurityContextServerLogoutHandler;
import reactor.core.publisher.Mono;

import static de.swm.lhm.geoportal.gateway.authentication.logout.LogoutParameters.ID_TOKEN_ATTRIBUTE_KEY;


@RequiredArgsConstructor
@Slf4j
public class KeycloakLogoutHandler extends SecurityContextServerLogoutHandler {

    private final IAuthService authorizationService;

    @Override
    public Mono<Void> logout(WebFilterExchange exchange, Authentication authentication) {
        return authorizationService.getIdTokenValue()
                .switchIfEmpty(authorizationService.getIdTokenValue(exchange))
                .flatMap(token -> {
                    exchange.getExchange().getAttributes().put(ID_TOKEN_ATTRIBUTE_KEY, token);
                    return super.logout(exchange, authentication);
                });
    }

}
