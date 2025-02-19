package de.swm.lhm.geoportal.gateway.sensor;

import de.swm.lhm.geoportal.gateway.filter.gatewayfilter.ReplaceStringInBodyGatewayFilter;
import de.swm.lhm.geoportal.gateway.filter.gatewayfilter.UrlParameterAsciiReescapingGatewayFilter;
import de.swm.lhm.geoportal.gateway.route.GatewayRoute;
import de.swm.lhm.geoportal.gateway.route.GatewayRoutesCollector;
import de.swm.lhm.geoportal.gateway.sensor.model.SensorLayer;
import de.swm.lhm.geoportal.gateway.shared.GatewayService;
import de.swm.lhm.geoportal.gateway.util.ExtendedURIBuilder;
import de.swm.lhm.geoportal.gateway.util.messagebody.MessageBodyEncodingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;


@ConditionalOnProperty(
        value = "geoportal.gateway.sensor.routes.enabled",
        havingValue = "true",
        matchIfMissing = true
)
@Service
@RequiredArgsConstructor
@Slf4j
public class SensorRoutesCollector implements GatewayRoutesCollector {

    private final SensorLayerRepository sensorLayerRepository;
    private final SensorLayerProperties sensorLayerProperties;
    private final GatewayService gatewayService;
    private final UrlParameterAsciiReescapingGatewayFilter urlParameterAsciiReescapingGatewayFilter = new UrlParameterAsciiReescapingGatewayFilter();
    private final MessageBodyEncodingService messageBodyEncodingService;

    @Override
    public Flux<GatewayRoute> getAllGatewayRoutes() {
        return sensorLayerRepository.findAllByStage(gatewayService.getStage())
                .map(this::mapSensorServiceToGatewayRoute);
    }

    private GatewayRoute mapSensorServiceToGatewayRoute(SensorLayer sensorLayer) {
        GatewayRoute.GatewayRouteBuilder builder = GatewayRoute.builder()
                .routeId(sensorLayer.getId())
                .path(buildRoutePath(sensorLayer.getId()))
                .url(sensorLayer.getUrl())
                .stripPrefix(2);

        List<GatewayFilter> gatewayFilters = new ArrayList<>();

        if (sensorLayerProperties.isReescapeUrlParameterAscii()) {
            log.debug("Enabling re-escaping of URL parameters for sensor layer with id {}", sensorLayer.getId());
            gatewayFilters.add(urlParameterAsciiReescapingGatewayFilter);
        }

        if (sensorLayerProperties.isReplaceOriginatingUrl()) {
            log.debug("Enabling replacing originating internal URLs with external URLs for sensor layer with id {}", sensorLayer.getId());
            // In case sensor data is streamed, this filter is probably not suitable,
            // as it tries to read the whole body into memory.
            gatewayFilters.add(new ReplaceStringInBodyGatewayFilter(
                    messageBodyEncodingService,
                    removeTrailingSlashes(sensorLayer.getUrl()),
                    removeTrailingSlashes(buildSensorExternalUrl(sensorLayer.getId()))
            ));
        }

        return builder.gatewayFilters(gatewayFilters).build();
    }

    private String removeTrailingSlashes(String url) {
        if (url == null) {
            return null;
        }
        return url.replaceAll("/+$", "");
    }

    private ExtendedURIBuilder createUrlBuilder(String baseUrl, String sensorId) {
        try {
            return new ExtendedURIBuilder(baseUrl)
                    .addPath(sensorLayerProperties.getEndpoint())
                    .addPath(sensorId);
        } catch (URISyntaxException e) {
            throw new SensorLayerException(
                    String.format(
                            "Failed to build route path for sensor layer with id %s, endpoint = %s ",
                            sensorId,
                            sensorLayerProperties.getEndpoint()
                    ),
                    e
            );
        }
    }

    private String buildRoutePath(String sensorId) {
        return createUrlBuilder("", sensorId)
                .addPath("**")
                .getPath();
    }

    private String buildSensorExternalUrl(String sensorId) {
        return createUrlBuilder(gatewayService.getExternalUrl(), sensorId).toString();
    }

}
