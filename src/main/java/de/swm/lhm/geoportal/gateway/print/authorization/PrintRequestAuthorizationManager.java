package de.swm.lhm.geoportal.gateway.print.authorization;

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

import java.util.Set;

@Component
@Setter
@Getter
@Slf4j
@RequiredArgsConstructor
public class PrintRequestAuthorizationManager extends AbstractAuthorizationManager {

    private final IAuthService authorizationService;

    @Override
    public AuthorizationDecision getAuthorizationDecision(Authentication authentication, AuthorizationContext authorizationContext) {

        Object potPrintRequestAuthContext = authorizationContext.getVariables()
                .get(PrintRequestAuthorizationContext.KEY);

        if (potPrintRequestAuthContext == null){
            log.error("Expected a PortalRequestAuthContext but received none");
            return new AuthorizationDecision(false);
        }

        if (potPrintRequestAuthContext instanceof PrintRequestAuthorizationContext printRequestAuthContext) {
            Set<String> grantedProducts = authorizationService.getGrantedProducts(authentication);
            boolean grantedAuthLevelHigh = authorizationService.getGrantedAuthLevelHigh(authentication);

            Boolean granted = printRequestAuthContext.getLayerAuthorizationGroups().stream().allMatch(
                    authorizationGroup ->
                        AuthorizationGroup.isAuthorized(
                                authorizationGroup,
                                grantedProducts,
                                grantedAuthLevelHigh)
                                .isGranted());
            return new AuthorizationDecision(granted);

        } else {
            log.error(
                    "Expected a PortalRequestAuthContext but received an object of class {}",
                    potPrintRequestAuthContext.getClass().getSimpleName()
            );
            return new AuthorizationDecision(false);
        }
    }

}