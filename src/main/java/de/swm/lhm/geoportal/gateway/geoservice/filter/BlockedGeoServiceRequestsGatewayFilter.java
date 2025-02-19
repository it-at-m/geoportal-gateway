package de.swm.lhm.geoportal.gateway.geoservice.filter;

import de.swm.lhm.geoportal.gateway.filter.gatewayfilter.FilterOrder;
import de.swm.lhm.geoportal.gateway.geoservice.inspect.GeoServiceInspectorService;
import de.swm.lhm.geoportal.gateway.geoservice.inspect.GeoServiceRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
@Slf4j
public class BlockedGeoServiceRequestsGatewayFilter extends AbstractGeoServiceGatewayFilter implements Ordered {
    BlockedGeoServiceRequestsGatewayFilter(GeoServiceInspectorService geoServiceInspectorService) {
        super(geoServiceInspectorService);
    }

    @Override
    Mono<Void> filterGeoService(ServerWebExchange exchange, GatewayFilterChain chain, GeoServiceRequest geoServiceRequest) {
        boolean isBlocked = geoServiceRequest.getRequestType()
                .map(geoServiceInspectorService::isBlockedRequestType)
                .orElse(false);

        if (isBlocked) {
            if (log.isDebugEnabled()) {
                log.atDebug()
                        .setMessage(() -> String.format(
                                "Blocked geoservice request type %s to %s",
                                geoServiceRequest.getRequestType(),
                                exchange.getRequest().getPath().value()))
                        .log();
            }

            exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
            return Mono.empty(); // do not forward further down the chain
        } else {
            return chain.filter(exchange);
        }
    }

    @Override
    public int getOrder() {
        return FilterOrder.REQUEST_SECOND_FILTER.getOrder();
    }
}
