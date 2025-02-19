package de.swm.lhm.geoportal.gateway.portal.authorization;

import de.swm.lhm.geoportal.gateway.authorization.AbstractAuthorizationManager;
import de.swm.lhm.geoportal.gateway.authorization.IAuthService;
import de.swm.lhm.geoportal.gateway.authorization.model.AuthorizationGroup;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.server.authorization.AuthorizationContext;
import org.springframework.stereotype.Component;


@Component
@Setter
@Getter
@Slf4j
@RequiredArgsConstructor
public class PortalRequestAuthorizationManager extends AbstractAuthorizationManager {

    private final IAuthService authorizationService;

    @Override
    public AuthorizationDecision getAuthorizationDecision(Authentication authentication, AuthorizationContext authorizationContext) {

        Object potPortalRequestAuthContext = authorizationContext.getVariables()
                .getOrDefault(
                        PortalRequestAuthorizationContext.KEY_PORTAL_REQUEST_AUTH_CONTEXT,
                        new PortalRequestAuthorizationContext()
                );

        if (potPortalRequestAuthContext instanceof PortalRequestAuthorizationContext portalRequestAuthContext) {
            return AuthorizationGroup.isAuthorized(
                    portalRequestAuthContext.getAuthorizationGroup(),
                    authorizationService.getGrantedProducts(authentication),
                    authorizationService.getGrantedAuthLevelHigh(authentication)
            );
        } else {
            log.error(
                    "Expected a PortalRequestAuthContext but received an object of class {}",
                    potPortalRequestAuthContext.getClass().getSimpleName()
            );
            return new AuthorizationDecision(false);
        }
    }

}
