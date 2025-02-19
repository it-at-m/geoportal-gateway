package de.swm.lhm.geoportal.gateway.geoservice.filter;

import de.swm.lhm.geoportal.gateway.geoservice.inspect.GeoServiceInspectorService;
import de.swm.lhm.geoportal.gateway.geoservice.inspect.GeoServiceRequest;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Drops the Authorization headers intended for the gateway from the request before forwarding the request to geoserver.
 *
 * This avoids triggering geoserver to attempt to authenticate the request which would fail.
 *
 * This filter is supposed to run after ArcGisAuthenticationTriggerFilter to ensure the Authorization header is removed only
 * after the ArcGIS-request has been checked for it. This order should be enforced by ArcGisAuthenticationTriggerFilter being
 * a WebFilter.
 */
@Component
public class DropAuthHeaderGeoServiceGatewayFilter extends AbstractGeoServiceGatewayFilter {
    DropAuthHeaderGeoServiceGatewayFilter(GeoServiceInspectorService geoServiceInspectorService) {
        super(geoServiceInspectorService);
    }

    @Override
    Mono<Void> filterGeoService(ServerWebExchange exchange, GatewayFilterChain chain, GeoServiceRequest geoServiceRequest) {
        ServerWebExchange mutatedExchange = exchange.mutate()
                .request(requestBuilder ->
                        requestBuilder.headers(headers -> {
                            headers.remove(HttpHeaders.AUTHORIZATION);
                            headers.remove(HttpHeaders.COOKIE);
                        })
                )
                .build();
        return chain.filter(mutatedExchange);
    }
}
