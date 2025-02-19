package de.swm.lhm.geoportal.gateway.authentication.basic_auth;

import de.swm.lhm.geoportal.gateway.authentication.keycloak.KeyCloakService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;


@Component
@RequiredArgsConstructor
public class BasicAuthenticationManager implements ReactiveAuthenticationManager {

    private final KeyCloakService keyCloakService;

    @Override
    public Mono<Authentication> authenticate(Authentication authentication) {

        if (! (authentication instanceof UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken)) {
            throw new AccessDeniedException("failed to access http-basic credentials");
        }

        String userName = usernamePasswordAuthenticationToken.getName();
        String password = (String) usernamePasswordAuthenticationToken.getCredentials();

        Authentication unauthenticatedToken = getUnauthenticatedToken(authentication);

        return keyCloakService.authenticateAndGetPrincipal(userName, password)
                .onErrorReturn(unauthenticatedToken)
                .defaultIfEmpty(unauthenticatedToken);
    }

    private Authentication getUnauthenticatedToken(Authentication authentication) {
        UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(
                authentication.getPrincipal(),
                null,
                AuthorityUtils.NO_AUTHORITIES
        );
        token.setAuthenticated(false);
        return token;
    }
}
