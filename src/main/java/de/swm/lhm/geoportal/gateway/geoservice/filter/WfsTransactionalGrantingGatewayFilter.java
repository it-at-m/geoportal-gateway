package de.swm.lhm.geoportal.gateway.geoservice.filter;

import de.swm.lhm.geoportal.gateway.filter.gatewayfilter.FilterOrder;
import de.swm.lhm.geoportal.gateway.geoservice.GeoServicesService;
import de.swm.lhm.geoportal.gateway.geoservice.inspect.GeoServiceInspectorService;
import de.swm.lhm.geoportal.gateway.geoservice.inspect.GeoServiceRequest;
import de.swm.lhm.geoportal.gateway.geoservice.inspect.GeoServiceRequestType;
import de.swm.lhm.geoportal.gateway.geoservice.inspect.QualifiedLayerName;
import de.swm.lhm.geoportal.gateway.shared.model.ServiceType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.stream.Collectors;

@Component
@Slf4j
public class WfsTransactionalGrantingGatewayFilter extends AbstractGeoServiceGatewayFilter implements Ordered {
    private final GeoServicesService geoServicesService;

    WfsTransactionalGrantingGatewayFilter(GeoServiceInspectorService geoServiceInspectorService, GeoServicesService geoServicesService) {
        super(geoServiceInspectorService);
        this.geoServicesService = geoServicesService;
    }

    @Override
    Mono<Void> filterGeoService(ServerWebExchange exchange, GatewayFilterChain chain, GeoServiceRequest geoServiceRequest) {
        if (geoServiceRequest.is(ServiceType.WFS, GeoServiceRequestType.TRANSACTION)) {
            return Flux.fromIterable(geoServiceRequest.getLayers())
                    .flatMap(geoServicesService::layerHasWfsTEnabled)
                    .all(v -> v)
                    .switchIfEmpty(Mono.just(true)) // default. Used when layer list is empty
                    .flatMap(isWfsTEnabled -> {
                        if (Boolean.TRUE.equals(isWfsTEnabled)) {
                            // Authn is handled by GeoServiceAuthorizationManager, so we just check here if the layer
                            // has WFS-T enabled.
                            return chain.filter(exchange);
                        } else {
                            // block if WFS-T is not enabled for the layers. Request will not be forwarded
                            // to geoserver.

                            log.atInfo()
                                    .setMessage(() -> String.format(
                                            "WFS-T request to layer %s forbidden/blocked, as layer is not WFS-T enabled",
                                            geoServiceRequest.getLayers()
                                                    .stream()
                                                    .map(QualifiedLayerName::toString)
                                                    .collect(Collectors.joining(", "))))
                                    .log();
                            exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
                            return Mono.empty(); // do not forward further down the chain
                        }
                    });
        } else {
            return chain.filter(exchange);
        }
    }

    @Override
    public int getOrder() {
        return FilterOrder.REQUEST_SECOND_FILTER.getOrder();
    }
}
