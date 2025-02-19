package de.swm.lhm.geoportal.gateway.geoservice.authorization;

import de.swm.lhm.geoportal.gateway.authorization.IAuthService;
import de.swm.lhm.geoportal.gateway.geoservice.inspect.GeoServiceInspectorService;
import de.swm.lhm.geoportal.gateway.geoservice.inspect.GeoServiceRequest;
import de.swm.lhm.geoportal.gateway.shared.GatewayService;
import de.swm.lhm.geoportal.gateway.shared.model.Stage;
import de.swm.lhm.geoportal.gateway.style_preview.StylePreviewService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatcher;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.util.Map;

@Component
@Setter
@Getter
@Slf4j
@RequiredArgsConstructor
public class GeoServiceRequestMatcher implements ServerWebExchangeMatcher {

    private final GeoServiceInspectorService geoServiceInspectorService;
    private final IAuthService authorizationService;
    private final StylePreviewService stylePreviewService;
    private final GatewayService gatewayService;

    @Override
    public Mono<MatchResult> matches(ServerWebExchange exchange) {

        return geoServiceInspectorService.inspectRequestAndCache(exchange)
                .flatMap(geoServiceRequest -> analyzeGeoServiceRequest(geoServiceRequest, exchange))

                // requests without an extracted service type are be blocked in routing
                // as these are not part of the supported API.
                //
                // As routing uses the same GeoServiceInspectorService these are not passed to the loadbalancer in
                // the routing. So we do not need check for authorization in this matcher/manager implementation.
                .switchIfEmpty(MatchResult.notMatch());
    }

    private Mono<MatchResult> analyzeGeoServiceRequest(GeoServiceRequest geoServiceRequest, ServerWebExchange exchange) {
        GeoServiceAuthorizationContext.GeoServiceAuthorizationContextBuilder builder = GeoServiceAuthorizationContext.builder();

        if (gatewayService.getStage() == Stage.CONFIGURATION) {
            // style previews are only supported and allowed for stage config
            builder.stylePreview(stylePreviewService.getReferencedStylePreviews(exchange));
        }

        if (geoServiceRequest.getLayers().isEmpty()) {
            // no layers found in the request -> can be accessed
            return MatchResult.notMatch();
        }

        return Flux.fromIterable(geoServiceRequest.getLayers())
                // collect access infos for all layers of the request
                .flatMap(layerName -> authorizationService.getAccessInfoGroupForGeoServiceLayer(layerName)
                        .map(authorizationGroup -> Tuples.of(layerName, authorizationGroup)))
                // preserve only the ones which require authentication
                .filter(authorizationTuple -> !authorizationTuple.getT2().isPublic())
                .collectMap(Tuple2::getT1, Tuple2::getT2)
                .flatMap(layerMap -> {
                    if (layerMap.isEmpty()) {
                        // no layers requiring authorization contained in request
                        return MatchResult.notMatch();
                    } else {
                        return returnMatch(
                                builder
                                        .layerAuthorizationGroups(layerMap)
                                        .build()
                        );
                    }
                });
    }

    private Mono<MatchResult> returnMatch(GeoServiceAuthorizationContext geoServiceAuthorizationContext) {
        return MatchResult.match(
                Map.of(GeoServiceAuthorizationContext.MATCHRESULT_KEY, geoServiceAuthorizationContext)
        );
    }
}
