package de.swm.lhm.geoportal.gateway.print;

import de.swm.lhm.geoportal.gateway.geoservice.HostReplacer;
import de.swm.lhm.geoportal.gateway.print.filter.JsonReplacePublicHostnamesGatewayFilter;
import de.swm.lhm.geoportal.gateway.util.ExtendedURIBuilder;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@RequiredArgsConstructor
@Configuration
public class PrintRouteConfig {
    public static final String MAPFISH_ROUTE_NAME = "mapfish";
    private final HostReplacer hostReplacer;
    private final PrintProperties printProperties;

    @Value("${geoportal.mapfish.url}")
    private String mapfishUrl;

    @Bean
    public RouteLocator printRouteLocator(RouteLocatorBuilder routeLocatorBuilder) {

        return routeLocatorBuilder.routes()
                .route(MAPFISH_ROUTE_NAME, r -> r
                        .path(String.format("%s/**", getPrintEndpoint()))
                        .filters(f -> f.filters(new JsonReplacePublicHostnamesGatewayFilter(hostReplacer)))
                        .uri(mapfishUrl))
                .build();
    }

    @SneakyThrows
    private String getPrintEndpoint() {
        ExtendedURIBuilder uriBuilder = new ExtendedURIBuilder("/");
        uriBuilder.addPath(printProperties.getEndpoint());
        return uriBuilder.build().getPath();
    }
}
