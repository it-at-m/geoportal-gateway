package de.swm.lhm.geoportal.gateway.route;

import reactor.core.publisher.Flux;

public interface GatewayRoutesCollector {

    Flux<GatewayRoute> getAllGatewayRoutes();

}
