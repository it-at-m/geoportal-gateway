package de.swm.lhm.geoportal.gateway.route;


import de.swm.lhm.geoportal.gateway.filter.gatewayfilter.UrlParameterAsciiReescapingGatewayFilter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.BooleanSpec;
import org.springframework.cloud.gateway.route.builder.Buildable;
import org.springframework.cloud.gateway.route.builder.PredicateSpec;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.stereotype.Service;

import reactor.core.publisher.Flux;

import java.net.URI;


@RequiredArgsConstructor
@Service
@Slf4j
public class GatewayRouteLocator implements RouteLocator {

    private final RouteLocatorBuilder routeLocatorBuilder;
    private final GatewayRouteService gatewayRouteService;

    @Override
    public Flux<Route> getRoutes() {
        RouteLocatorBuilder.Builder routesBuilder = routeLocatorBuilder.routes();
        return gatewayRouteService.getAll()
                .map(apiRoute ->
                        routesBuilder.route(
                                String.valueOf(apiRoute.getRouteId()),
                                predicateSpec -> setPredicateSpec(apiRoute, predicateSpec)
                        )
                )
                .collectList()
                .flatMapMany(builders -> routesBuilder.build().getRoutes());
    }

    private Buildable<Route> setPredicateSpec(GatewayRoute gatewayRoute, PredicateSpec predicateSpec) {

        BooleanSpec booleanSpec = predicateSpec.path(gatewayRoute.getPath());

        if (gatewayRoute.getStripPrefix() != null && gatewayRoute.getStripPrefix() > 0){
            booleanSpec.filters(gatewayFilterSpec -> gatewayFilterSpec.stripPrefix(gatewayRoute.getStripPrefix()));
        }

        // https://github.com/spring-cloud/spring-cloud-gateway/issues/881
        // By default, the path component of the URL is intentionally ignored
        URI uri = URI.create(gatewayRoute.getUrl());
        if (!uri.getPath().isEmpty()){
            booleanSpec.filters(gatewayFilterSpec -> gatewayFilterSpec.prefixPath(uri.getPath()));
        }

        if (gatewayRoute.getMethod() != null) {
            booleanSpec.and().method(gatewayRoute.getMethod());
        }

        if (gatewayRoute.getGatewayFilters() != null && !gatewayRoute.getGatewayFilters().isEmpty()) {
            booleanSpec.filters(gatewayFilterSpec -> {
                for (var gatewayFilter : gatewayRoute.getGatewayFilters()) {
                    gatewayFilterSpec = gatewayFilterSpec.filter(gatewayFilter);
                }
                return gatewayFilterSpec;
            });
        }

        log.info("Registering route {} with url '{}'", gatewayRoute.getRouteId(), gatewayRoute.getUrl());
        return booleanSpec.uri(gatewayRoute.getUrl());
    }
}
