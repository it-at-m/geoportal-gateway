package de.swm.lhm.geoportal.gateway.route;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@TestConfiguration
public class MockRouteCollectorConfig {

    public static final AtomicInteger count = new AtomicInteger(0);
    public static final List<Integer> oldCount = Collections.synchronizedList(new ArrayList<>());
    public static final int port = 900;

    public static GatewayRoute buildRouteForId(int id){
        return GatewayRoute.builder()
                .routeId("" + id)
                .path("/route-" + id + "/**")
                .url("http://locahost:"+ port + id)
                .stripPrefix(0)
                .build();
    }

    public static GatewayRoute getCurrentRoute(){
        return buildRouteForId(count.get());
    }

    public static List<GatewayRoute> getOldRoutes(){

        return oldCount.stream()
                .map(MockRouteCollectorConfig::buildRouteForId)
                .toList();
    }

    @Bean
    GatewayRoutesCollector getMockRouteCollector(){
        return new MockRouteCollectorGateway();
    }

    public static class MockRouteCollectorGateway implements GatewayRoutesCollector {

        @Override
        public Flux<GatewayRoute> getAllGatewayRoutes() {

            int newRouteId = count.incrementAndGet();

            int oldRouteId = newRouteId - 1;
            if (oldRouteId > 0)
                oldCount.add(oldRouteId);

            return Flux.just(buildRouteForId(newRouteId));
        }

    }

}
