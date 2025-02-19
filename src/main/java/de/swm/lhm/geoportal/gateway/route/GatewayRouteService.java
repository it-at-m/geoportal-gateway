package de.swm.lhm.geoportal.gateway.route;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class GatewayRouteService {

    private final List<GatewayRoutesCollector> gatewayRoutesCollectors;

    public Flux<GatewayRoute> getAll() {
        return Flux.concat(gatewayRoutesCollectors.stream().map(this::get).toList());
    }

    private Flux<GatewayRoute> get(GatewayRoutesCollector collector) {
        return Flux.defer(() -> {
            try {
                return collector.getAllGatewayRoutes();
            } catch (Exception e) {
                log.atError()
                   .setMessage(() -> String.format("Failed to call GatewayRoutesCollector %s", collector.getClass().getSimpleName()))
                   .setCause(e)
                   .log();
                return Flux.empty();
            }
        });
    }
}