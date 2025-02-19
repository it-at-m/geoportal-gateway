package de.swm.lhm.geoportal.gateway.sensor;

import de.swm.lhm.geoportal.gateway.product.HasProductDetails;
import de.swm.lhm.geoportal.gateway.product.model.Product;
import de.swm.lhm.geoportal.gateway.sensor.model.SensorLayer;
import de.swm.lhm.geoportal.gateway.shared.GatewayService;
import de.swm.lhm.geoportal.gateway.shared.model.ServiceType;
import de.swm.lhm.geoportal.gateway.util.ExtendedURIBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.net.URISyntaxException;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SensorLayerService implements HasProductDetails {

    private final SensorLayerRepository sensorLayerRepository;
    private final SensorLayerProperties sensorLayerProperties;
    private final GatewayService gatewayService;

    @Override
    public Mono<Void> enrichProduct(Product product) {
        return sensorLayerRepository.findSensorServiceByProductId(product.getId(), product.getStage())
                .map(this::mapSenorServiceToProductService)
                .doOnNext(product::addGeoService)
                .then();
    }

    private Product.Service mapSenorServiceToProductService(SensorLayer sensorLayer) {
        return Product.Service.builder()
                .name(sensorLayer.getName())
                .accessLevel(sensorLayer.getAccessLevel())
                .authLevelHigh(sensorLayer.getAuthLevelHigh())
                .urls(buildUrls(sensorLayer))
                .build();
    }

    private List<Product.Service.ServiceUrl> buildUrls(SensorLayer sensorLayer) {
        return List.of(
                Product.Service.ServiceUrl.builder()
                        .serviceType(ServiceType.STA)
                        .url(buildUrl(sensorLayer))
                        .build()
        );
    }

    private String buildUrl(SensorLayer sensorLayer) {
        try {
            return new ExtendedURIBuilder(gatewayService.getExternalUrl())
                    .addPath(this.sensorLayerProperties.getEndpoint())
                    .addPath(sensorLayer.getId())
                    .toString();
        } catch (URISyntaxException e) {
            log.atError()
               .setMessage(() -> String.format("Failed to build service url for sensor service %s, id = %s", sensorLayer.getName(), sensorLayer.getId()))
               .setCause(e)
               .log();
            return "";
        }
    }
}