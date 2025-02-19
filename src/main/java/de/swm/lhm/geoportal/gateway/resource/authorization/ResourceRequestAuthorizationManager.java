package de.swm.lhm.geoportal.gateway.resource.authorization;

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
import java.util.stream.Stream;

@Component
@Setter
@Getter
@Slf4j
@RequiredArgsConstructor
public class ResourceRequestAuthorizationManager extends AbstractAuthorizationManager {

    private final IAuthService authorizationService;

    @Override
    public AuthorizationDecision getAuthorizationDecision(Authentication authentication, AuthorizationContext authorizationContext) {

        Object potResourceRequestAuthContext = authorizationContext.getVariables()
                .get(ResourceRequestAuthorizationContext.KEY);

        if (potResourceRequestAuthContext == null) {
            log.error("Expected a ResourceRequestAuthContext but received none");
            return new AuthorizationDecision(false);
        }

        if (potResourceRequestAuthContext instanceof ResourceRequestAuthorizationContext resourceRequestAuthContext) {
            Set<String> grantedProducts = authorizationService.getGrantedProducts(authentication);
            boolean grantedAuthLevelHigh = authorizationService.getGrantedAuthLevelHigh(authentication);


            Boolean granted = Stream.of(
                            resourceRequestAuthContext.getConfiguredAuthorizationGroup(),
                            resourceRequestAuthContext.getLayerAuthorizationGroup()
                    )
                    .allMatch(
                            authorizationGroup ->
                                    AuthorizationGroup
                                            .isAuthorized(
                                                    authorizationGroup,
                                                    grantedProducts,
                                                    grantedAuthLevelHigh)
                                            .isGranted());
            return new AuthorizationDecision(granted);

        } else {
            log.error(
                    "Expected a ResourceRequestAuthContext but received an object of class {}",
                    potResourceRequestAuthContext.getClass().getSimpleName()
            );
            return new AuthorizationDecision(false);
        }
    }

}