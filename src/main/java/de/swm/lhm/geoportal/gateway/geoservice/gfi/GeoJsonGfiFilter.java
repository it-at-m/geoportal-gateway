package de.swm.lhm.geoportal.gateway.geoservice.gfi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import de.swm.lhm.geoportal.gateway.geoservice.PropertyFieldService;
import de.swm.lhm.geoportal.gateway.geoservice.inspect.QualifiedLayerName;
import de.swm.lhm.geoportal.gateway.util.MediaTypeExt;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuples;

import java.io.IOException;
import java.io.StringReader;
import java.util.Optional;

@Component
@Slf4j
public class GeoJsonGfiFilter extends AbstractGfiFilter {

    private static final String FEATURES_KEY_NAME = "features";
    private static final String ID_KEY_NAME = "id";
    private static final String PROPERTIES_KEY_NAME = "properties";
    private static final String GEOMETRY_NAME_KEY_NAME = "geometry_name";
    private static final String GEOMETRY_KEY_NAME = "geometry";

    final ObjectMapper objectMapper = new ObjectMapper();

    public GeoJsonGfiFilter(PropertyFieldService propertyFieldService) {
        super(propertyFieldService);
    }

    @Override
    public boolean supportsMediaType(MediaType mediaType) {
        if (mediaType == null) {
            return false;
        }
        return mediaType.isCompatibleWith(MediaType.APPLICATION_JSON)
                || mediaType.isCompatibleWith(MediaTypeExt.APPLICATION_VND_GEO_JSON)
                || mediaType.isCompatibleWith(MediaTypeExt.APPLICATION_GEO_JSON);
    }

    @Override
    @SneakyThrows
    Mono<String> filterBody(LayerNameResolver layerNameResolver, String body, ReferencedColumnValueTransformer referencedColumnValueTransformer) {
        try (StringReader reader = new StringReader(body)) {
            JsonNode documentNode = objectMapper.readValue(reader, JsonNode.class);

            if (documentNode instanceof ObjectNode documentObjectNode) {
                return featuresFlux(documentObjectNode)
                        .flatMap(featureNode -> transformFeature(layerNameResolver, featureNode, referencedColumnValueTransformer))
                        .reduce(objectMapper.createArrayNode(), (accum, featureNode) -> {
                            accum.add(featureNode);
                            return accum;
                        })
                        .map(features -> {
                            documentObjectNode.replace(FEATURES_KEY_NAME, features);
                            return documentObjectNode.toString();
                        });
            } else {
                return Mono.just(body);
            }
        } catch (IOException e) {
            log.error("parsing geojson failed", e);
            throw e;
        }
    }

    private Flux<ObjectNode> featuresFlux(ObjectNode documentObjectNode) {
        JsonNode features = documentObjectNode.get(FEATURES_KEY_NAME);
        if (features instanceof ArrayNode featuresArrayNode) {
            return Flux.fromIterable(featuresArrayNode)
                    .filter(ObjectNode.class::isInstance)
                    .cast(ObjectNode.class);
        } else {
            return Flux.empty();
        }
    }

    private Mono<ObjectNode> transformFeature(LayerNameResolver layerNameResolver, ObjectNode feature, ReferencedColumnValueTransformer referencedColumnValueTransformer) {
        if (feature.get(PROPERTIES_KEY_NAME) instanceof ObjectNode properties && feature.get(ID_KEY_NAME) instanceof TextNode idTextNode) {
            Optional<QualifiedLayerName> layerName = layerNameResolver.resolveLayerNameFromFeatureId(idTextNode.asText());
            if (layerName.isPresent()) {
                return Flux.fromIterable(properties::fieldNames)
                        .flatMap(propertyKey -> {
                                    JsonNode propertyValue = properties.get(propertyKey);
                                    return switch (propertyValue) {
                                        case TextNode tn ->
                                                referencedColumnValueTransformer.accept(new ReferencedColumnValue(layerName.get(), propertyKey, tn.textValue()))
                                                        .flatMap(v -> Mono.just(Tuples.of((JsonNode) new TextNode(v), propertyKey)));
                                        // all other node datatypes can not be transformed, only filtered
                                        default ->
                                                referencedColumnValueTransformer.accept(new ReferencedColumnValue(layerName.get(), propertyKey, ""))
                                                        .flatMap(v -> Mono.just(Tuples.of(propertyValue, propertyKey)));
                                    };
                                }
                        )
                        .reduce(objectMapper.createObjectNode(), (newProperties, propertyTuple) -> {
                            newProperties.set(propertyTuple.getT2(), propertyTuple.getT1());
                            return newProperties;
                        })
                        .map(newProperties -> {
                            if (feature.has(GEOMETRY_NAME_KEY_NAME) && !newProperties.has(feature.get(GEOMETRY_NAME_KEY_NAME).textValue())) {
                                feature.remove(GEOMETRY_NAME_KEY_NAME);
                                feature.remove(GEOMETRY_KEY_NAME);
                            }
                            feature.replace(PROPERTIES_KEY_NAME, newProperties);
                            return feature;
                        });
            }
        }
        return Mono.just(feature);
    }

}
