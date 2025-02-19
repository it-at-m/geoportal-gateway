package de.swm.lhm.geoportal.gateway.geoservice.filter;

import de.swm.lhm.geoportal.gateway.geoservice.inspect.GeoServiceInspectorService;
import de.swm.lhm.geoportal.gateway.geoservice.inspect.GeoServiceRequest;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

abstract class AbstractGeoServiceGatewayFilter implements GeoServiceGatewayFilter {

    protected final GeoServiceInspectorService geoServiceInspectorService;

    AbstractGeoServiceGatewayFilter(GeoServiceInspectorService geoServiceInspectorService) {
        this.geoServiceInspectorService = geoServiceInspectorService;
    }

    abstract Mono<Void> filterGeoService(ServerWebExchange exchange, GatewayFilterChain chain, GeoServiceRequest geoServiceRequest);

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return geoServiceInspectorService.inspectRequestAndCache(exchange)
                .flatMap(geoServiceRequest ->
                        filterGeoService(exchange, chain, geoServiceRequest)
                                // was geoservice request, transform to Mono<void> to be able to
                                // detect if the filter was applied.
                                .then(Mono.just(true))
                )
                .switchIfEmpty(
                        // was no geoservice request, apply the rest of the filter chain
                        chain.filter(exchange)
                                // same type as above
                                .then(Mono.just(false))
                )
                .then(); // back to void type
    }
}
