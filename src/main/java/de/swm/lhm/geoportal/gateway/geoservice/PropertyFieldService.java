package de.swm.lhm.geoportal.gateway.geoservice;

import de.swm.lhm.geoportal.gateway.geoservice.inspect.QualifiedLayerName;
import de.swm.lhm.geoportal.gateway.geoservice.model.PropertyField;
import de.swm.lhm.geoportal.gateway.geoservice.model.PropertyFieldEscaping;
import de.swm.lhm.geoportal.gateway.resource.ResourceService;
import de.swm.lhm.geoportal.gateway.resource.model.FileResource;
import de.swm.lhm.geoportal.gateway.shared.GatewayService;
import de.swm.lhm.geoportal.gateway.util.ReactiveCache;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PropertyFieldService {
    private final GatewayService gatewayService;
    private final PropertyFieldRepository propertyFieldRepository;
    private final ResourceService resourceService;
    private final GeoServiceRepository geoServiceRepository;

    private final ReactiveCache<QualifiedLayerName, Map<String, PropertyField>> geoServicePropertyFieldCache = new ReactiveCache<>(200, Duration.ofSeconds(10L));
    private final ReactiveCache<QualifiedLayerName, List<QualifiedLayerName>> containedQualifiedLayersCache = new ReactiveCache<>(200, Duration.ofSeconds(10L));

    public Mono<Map<String, PropertyField>> getGeoServicePropertyFieldsByNameAndWorkspace(QualifiedLayerName qualifiedLayerName) {
        return geoServicePropertyFieldCache.get(qualifiedLayerName,
                propertyFieldRepository.findGeoServicePropertyFieldsByWorkspaceAndNameAndStage(
                                qualifiedLayerName.workspaceName(),
                                qualifiedLayerName.layerName(),
                                gatewayService.getStage())
                        .reduce(new HashMap<>(), (accum, propertyField) -> {
                            accum.put(propertyField.fieldName(), propertyField);
                            return accum;
                        })
        );
    }

    public Mono<List<QualifiedLayerName>> getContainedQualifiedLayerNames(QualifiedLayerName parentQualifiedLayerName) {
        return containedQualifiedLayersCache.get(parentQualifiedLayerName,
                geoServiceRepository.findContainedQualifiedLayerNames(
                        gatewayService.getStage(),
                        parentQualifiedLayerName
                ).collectList()
        );
    }

    public Optional<String> escapeFieldValue(String fieldValue, PropertyField propertyField) {
        if (fieldValue == null || propertyField == null) {
            return Optional.empty();
        }
        if (propertyField.escaping() == PropertyFieldEscaping.FILE || propertyField.escaping() == PropertyFieldEscaping.IMAGE) {
            FileResource fileResource = new FileResource();
            fileResource.setUnit(propertyField.schemaName());
            fileResource.setName(String.format(
                    "%s/%s/%s",
                    propertyField.tableName(),
                    propertyField.fieldName(),
                    fieldValue.trim()
            ));
            return Optional.of(resourceService.buildUrl(fileResource));
        }
        return Optional.of(fieldValue.trim());
    }

}
