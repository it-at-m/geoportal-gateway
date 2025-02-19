package de.swm.lhm.geoportal.gateway.geoservice;

import de.swm.lhm.geoportal.gateway.geoservice.filter.GeoServiceGatewayFilter;
import de.swm.lhm.geoportal.gateway.geoservice.inspect.GeoServiceInspectorService;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;


@Configuration
@RequiredArgsConstructor
public class GeoServiceRouteConfig {

    @Bean
    public RouteLocator geoserverRouteLocator(
            RouteLocatorBuilder builder,
            GeoServiceInspectorService geoServiceInspectorService,
            GeoServiceProperties geoServiceProperties,
            List<GeoServiceGatewayFilter> geoServiceGatewayFilters
    ) {
        List<GatewayFilter> gatewayFilters = geoServiceGatewayFilters.stream()
                .map(GatewayFilter.class::cast)
                .toList();

        return builder.routes()
                .route("geoserver", r -> r
                        .path(geoServiceInspectorService.getGeoServicePathPrefix() + "/**")
                        .and()
                        .asyncPredicate(geoServiceInspectorService::isSupportedGeoServiceRequest)
                        .filters(f -> f.filters(gatewayFilters))
                        .uri(geoServiceProperties.getUrl()))
                .build();
    }

}
