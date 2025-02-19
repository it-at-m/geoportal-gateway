package de.swm.lhm.geoportal.gateway.print.authorization;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.swm.lhm.geoportal.gateway.authorization.IAuthService;
import de.swm.lhm.geoportal.gateway.authorization.model.AuthorizationGroup;
import de.swm.lhm.geoportal.gateway.print.model.PrintRequest;
import de.swm.lhm.geoportal.gateway.util.DataBufferUtils;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatcher;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;



@Component
@Setter
@Getter
@Slf4j
@RequiredArgsConstructor
public class PrintRequestMatcher implements ServerWebExchangeMatcher {
    private final ObjectMapper objectMapper;
    private final IAuthService authorizationService;

    @Override
    public Mono<MatchResult> matches(ServerWebExchange exchange) {
        if (!isPostPrintRequest(exchange)) {
            return MatchResult.notMatch();
        }


        Mono<PrintRequest> printRequest = extractBody(exchange);

        return printRequest.map(PrintRequest::getLayerNamesForAuthorization)
                .flatMap(authorizationService::getAccessInfoGroupForGeoServiceLayers)
                .flatMap(this::matches);
    }

    private boolean isPostPrintRequest(ServerWebExchange exchange) {
        String requestPath = exchange.getRequest().getPath().value();
        HttpMethod httpMethod = exchange.getRequest().getMethod();
        return requestPath.startsWith("/printserver/") && httpMethod.equals(HttpMethod.POST);
    }

    private Mono<MatchResult> matches(List<AuthorizationGroup> layerAuthorizationGroups) {
        if (layerAuthorizationGroups.stream().allMatch(AuthorizationGroup::isPublic)) {
            log.debug("no auth for printing required, all layers are public");
            return MatchResult.notMatch();
        }
        log.debug("auth for printing required, some layers are protected");
        return MatchResult.match(
                Map.of(PrintRequestAuthorizationContext.KEY,
                        PrintRequestAuthorizationContext.builder()
                                .layerAuthorizationGroups(layerAuthorizationGroups)
                                .build())
        );
    }

    private Mono<PrintRequest> extractBody(ServerWebExchange exchange) {
        return DataBufferUtils.copyAsObject(exchange.getRequest().getBody(), PrintRequest.class);
    }
}

