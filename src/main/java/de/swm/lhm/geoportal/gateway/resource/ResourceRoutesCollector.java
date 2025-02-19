package de.swm.lhm.geoportal.gateway.resource;

import de.swm.lhm.geoportal.gateway.route.GatewayRoute;
import de.swm.lhm.geoportal.gateway.route.GatewayRoutesCollector;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@ConditionalOnProperty("geoportal.resource.enable-webserver")
@Service
@RequiredArgsConstructor
@Slf4j
public class ResourceRoutesCollector implements GatewayRoutesCollector {
    private final ResourceProperties resourceProperties;
    @Override
    public Flux<GatewayRoute> getAllGatewayRoutes() {
        if (StringUtils.isBlank(resourceProperties.getEndpoint()) || StringUtils.isBlank(resourceProperties.getWebserverPath())){
            log.error("Resource webserver can not be enabled, because endpoint or webserver path is not set");
            return Flux.empty();
        }
        return Flux.just(GatewayRoute.builder()
                .routeId("resourceWebServer")
                .path(resourceProperties.getEndpoint() + "/**")
                .stripPrefix(numberOfPathSegments(resourceProperties.getEndpoint()))
                .url(resourceProperties.getWebserverPath())
                .build());
    }

    private int numberOfPathSegments(String path){
        return path.split("/").length - 1;
    }
}
