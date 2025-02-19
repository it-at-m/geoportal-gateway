package de.swm.lhm.geoportal.gateway.geoservice.gfi;

import de.swm.lhm.geoportal.gateway.geoservice.PropertyFieldService;
import de.swm.lhm.geoportal.gateway.geoservice.inspect.GeoServiceRequest;
import de.swm.lhm.geoportal.gateway.geoservice.model.PropertyField;
import de.swm.lhm.geoportal.gateway.util.ReactiveUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuples;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RequiredArgsConstructor
abstract class AbstractGfiFilter implements GfiFilter {
    // Springs MediaType.parseMediatype can not parse Mediatypes with subtypes ("text/xml; subtype=gml/3.1.1"), or formats like
    // "application/vnd.ogc.gml/3.1.1" as announced by geoserver. So this regex is used first
    private static final Pattern RE_MEDIATYPE = Pattern.compile("^\s*(?<type>[^\s;/]+)/(?<subtype>[^\s;/]+)");
    private final PropertyFieldService propertyFieldService;

    @Override public boolean supportsFormat(String contentType) {
        if (contentType == null) {
            return false;
        }

        Matcher mMediatype = RE_MEDIATYPE.matcher(contentType);
        if (!mMediatype.find()) {
            return false;
        }

        try {
            return supportsMediaType(new MediaType(mMediatype.group("type"), mMediatype.group("subtype")));
        } catch (InvalidMediaTypeException e) {
            return false;
        }
    }

    abstract Mono<String> filterBody(LayerNameResolver layerNameResolver, String body, ReferencedColumnValueTransformer referencedColumnValueTransformer);


    @Override
    public Mono<String> filterGetFeatureInfoBody(GeoServiceRequest geoServiceRequest, String body) {

        // collect all layers contained in the request and the recursively nested layers contained in those layers.
        // stuff everything in layernameresolver and apply the filter
        return Flux.fromIterable(geoServiceRequest.getLayers())
                .flatMap(rootLayerName -> propertyFieldService.getContainedQualifiedLayerNames(rootLayerName)
                        .map(childLayerNames -> Tuples.of(rootLayerName, childLayerNames))
                )
                .reduce(new LayerNameResolver(), (resolver, tuple) -> {
                            resolver.add(tuple.getT1(), tuple.getT2());
                            return resolver;
                        }
                )
                .flatMap(layerNameResolver -> filterBody(layerNameResolver, body, (referencedColumnValue) ->
                        // resolve to the root layer for this field. This is important as GFI fields are attached
                        // to the root layer of layer groups (topmost layer of the hierarchy)
                        ReactiveUtils.optionalToMono(layerNameResolver.getRootLayer(referencedColumnValue.qualifiedLayerName()))
                                .flatMap(rootLayerName -> filterMapLayerColumnValue(referencedColumnValue.replaceQualifiedLayerName(rootLayerName)))));
    }

    private Mono<String> filterMapLayerColumnValue(ReferencedColumnValue referencedColumnValue) {
        return propertyFieldService.getGeoServicePropertyFieldsByNameAndWorkspace(referencedColumnValue.qualifiedLayerName())
                .flatMap(fieldMap -> {
                    PropertyField propertyField = fieldMap.get(referencedColumnValue.columnName());
                    if (propertyField == null) {
                        return Mono.empty(); // filtered out
                    } else {
                        return Mono.just(
                                propertyFieldService.escapeFieldValue(referencedColumnValue.columnValue(), propertyField)
                                        .orElse("")
                        );
                    }
                });
    }
}
