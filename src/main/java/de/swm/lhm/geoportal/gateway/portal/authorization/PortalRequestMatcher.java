package de.swm.lhm.geoportal.gateway.portal.authorization;

import de.swm.lhm.geoportal.gateway.authorization.IAuthService;
import de.swm.lhm.geoportal.gateway.authorization.model.AuthorizationGroup;
import de.swm.lhm.geoportal.gateway.portal.PortalService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatcher;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Map;

@Component
@Setter
@Getter
@Slf4j
@RequiredArgsConstructor
@Primary
public class PortalRequestMatcher implements ServerWebExchangeMatcher {

    private final IAuthService authorizationService;
    private final PortalService portalService;

    @Override
    public Mono<MatchResult> matches(ServerWebExchange exchange) {

        if (!portalService.isPortalRequest(exchange)) {
            return MatchResult.notMatch();
        }

        String portalName = portalService.extractPortalName(exchange);

        return authorizationService.getAccessInfoGroupForPortal(portalName)
                .flatMap(accessInfoGroup -> this.matches(portalName, accessInfoGroup));

    }

    protected Mono<MatchResult> matches(String portalName, AuthorizationGroup authorizationGroup) {
        if (authorizationGroup.isPublic()) {
            log.debug("portal {} is public, no auth required", portalName);
            return MatchResult.notMatch();
        }
        log.debug("portal {} is protected, auth required", portalName);
        return MatchResult.match(
                Map.of(
                        PortalRequestAuthorizationContext.KEY_PORTAL_REQUEST_AUTH_CONTEXT,
                        PortalRequestAuthorizationContext.builder()
                                .portalName(portalName)
                                .authorizationGroup(authorizationGroup)
                                .build()
                )
        );
    }

}
