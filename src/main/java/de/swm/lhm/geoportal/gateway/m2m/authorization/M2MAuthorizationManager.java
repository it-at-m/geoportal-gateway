package de.swm.lhm.geoportal.gateway.m2m.authorization;

import de.swm.lhm.geoportal.gateway.authorization.AbstractAuthorizationManager;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.server.authorization.AuthorizationContext;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class M2MAuthorizationManager extends AbstractAuthorizationManager {
    @Override
    protected AuthorizationDecision getAuthorizationDecision(Authentication authentication, AuthorizationContext authorizationContext) {
        // being authenticated is sufficient
        return new AuthorizationDecision(true);
    }
}
