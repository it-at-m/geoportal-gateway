package de.swm.lhm.geoportal.gateway.authorization;

import org.springframework.security.authentication.AuthenticationTrustResolver;
import org.springframework.security.authentication.AuthenticationTrustResolverImpl;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.ReactiveAuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.server.authorization.AuthorizationContext;
import reactor.core.publisher.Mono;

public abstract class AbstractAuthorizationManager implements ReactiveAuthorizationManager<AuthorizationContext> {

    private final AuthenticationTrustResolver authTrustResolver = new AuthenticationTrustResolverImpl();

    @Override
    public Mono<AuthorizationDecision> check(Mono<Authentication> authentication, AuthorizationContext authorizationContext) {
        // isNotAnonymous copied from org.springframework.security.authorization.AuthenticatedReactiveAuthorizationManager
        return authentication.filter(this::isNotAnonymous)
                .map(auth -> this.getAuthorizationDecision(auth, authorizationContext))
                .defaultIfEmpty(new AuthorizationDecision(false));
    }

    private boolean isNotAnonymous(Authentication authentication) {
        return !this.authTrustResolver.isAnonymous(authentication);
    }

    protected abstract AuthorizationDecision getAuthorizationDecision(Authentication authentication, AuthorizationContext authorizationContext);

}
