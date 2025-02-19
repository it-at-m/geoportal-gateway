package de.swm.lhm.geoportal.gateway.generic;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import de.swm.lhm.geoportal.gateway.generic.model.GenericLayer;
import de.swm.lhm.geoportal.gateway.product.HasProductDetails;
import de.swm.lhm.geoportal.gateway.product.model.Product;
import de.swm.lhm.geoportal.gateway.shared.model.ServiceType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class GenericLayerService implements HasProductDetails {

    private final GenericLayerRepository genericLayerRepository;
    private final ObjectMapper objectMapper;

    @Override
    public Mono<Void> enrichProduct(Product product) {
        return genericLayerRepository.findSensorServiceByProductId(product.getId(), product.getStage())
                .map(this::mapGenericLayerToProductService)
                .doOnNext(product::addGeoService)
                .then();
    }

    private Product.Service mapGenericLayerToProductService(GenericLayer genericLayer) {
        return Product.Service.builder()
                .name(genericLayer.getName())
                .accessLevel(genericLayer.getAccessLevel())
                .authLevelHigh(genericLayer.getAuthLevelHigh())
                .urls(buildUrls(genericLayer))
                .build();
    }

    private List<Product.Service.ServiceUrl> buildUrls(GenericLayer genericLayer) {
        return List.of(
                Product.Service.ServiceUrl.builder()
                        .serviceType(ServiceType.GEN)
                        .url(findUrl(genericLayer))
                        .build()
        );
    }

    private String findUrl(GenericLayer genericLayer) {
        ObjectNode object;
        try {
            object = (ObjectNode) objectMapper.readTree(genericLayer.getJson());
        } catch (ClassCastException | IOException e) {
            object = objectMapper.createObjectNode();
            log.atError()
                    .setMessage(() -> String.format("Could not load json for generic layer %s, id = %s", genericLayer.getName(), genericLayer.getId()))
                    .setCause(e)
                    .log();
        }

        if (object.has("url"))
            return object.get("url").textValue();
        return "no url provided";
    }
}