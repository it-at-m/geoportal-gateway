package de.swm.lhm.geoportal.gateway.geoservice.gfi;

import de.swm.lhm.geoportal.gateway.geoservice.PropertyFieldService;
import de.swm.lhm.geoportal.gateway.geoservice.inspect.QualifiedLayerName;
import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Filters geoservers test GFI format as implemented in
 * https://github.com/geoserver/geoserver/blob/467186fe8f60f2f0dd247e95412593212facc4fc/src/wms/src/main/java/org/geoserver/wms/featureinfo/TextFeatureInfoOutputFormat.java#L86
 */
@Component
public class TextGfiFilter extends AbstractGfiFilter {

    private static final Pattern RE_COLUMN_NAME = Pattern.compile("^\\s*(?<column>[^=\\s]+)\\s*=\\s*(?<value>.*)");

    // the layername in the line is prefixed with the workspace namespace uri, and not the workspace name,
    // so this needs to be resolved using the layername resolver
    private static final Pattern RE_LAYER_RESULTS = Pattern.compile(
            "^\\s*Results\\s+for\\s+FeatureType\\s+'\\s*[^:]*:?(?<layer>[^']+)\\s*'",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern RE_SPLIT_LINES = Pattern.compile("\\r\\n|\\n|\\r");

    public TextGfiFilter(PropertyFieldService propertyFieldService) {
        super(propertyFieldService);
    }

    @Override
    public boolean supportsMediaType(MediaType mediaType) {
        if (mediaType == null) {
            return false;
        }
        return MediaType.TEXT_PLAIN.isCompatibleWith(mediaType);
    }

    @Override Mono<String> filterBody(LayerNameResolver layerNameResolver, String body, ReferencedColumnValueTransformer referencedColumnValueTransformer) {
        /* example document:

        Results for FeatureType 'playgound:gis_osm_pois_free_1_o2o':
        --------------------------------------------
        osm_id = 4873640911
        code = 2593
        fclass = vending_any
        name = null
        shape = [GEOMETRY (Point) with 1 points]
        --------------------------------------------
         */

        String[] inputBodyLines = RE_SPLIT_LINES.split(body);
        AtomicBoolean whithinColumnBlock = new AtomicBoolean(false);
        AtomicReference<QualifiedLayerName> currentLayerName = new AtomicReference<>();

        return Flux.fromStream(Arrays.stream(inputBodyLines))
                .flatMap(line -> {
                    String strippedLine = line.strip();
                    if (StringUtils.isBlank(strippedLine)) {
                        return Mono.just(line);
                    } else if (strippedLine.startsWith("-------------------")) {
                        whithinColumnBlock.set(!whithinColumnBlock.get());
                        return Mono.just(line);
                    } else if (whithinColumnBlock.get()) {
                        if (currentLayerName.get() != null) {

                            Matcher matcher = RE_COLUMN_NAME.matcher(strippedLine);
                            if (matcher.find()) {
                                ReferencedColumnValue refColumn = new ReferencedColumnValue(
                                        currentLayerName.get(),
                                        matcher.group("column").toLowerCase(Locale.ROOT),
                                        matcher.group("value")
                                );
                                return referencedColumnValueTransformer.accept(refColumn)
                                        .map(filteredValue -> String.format("%s = %s", refColumn.columnName(), filteredValue));
                            }
                        }
                        return Mono.empty(); // omitted from output
                    } else {
                        Matcher matcher = RE_LAYER_RESULTS.matcher(strippedLine);
                        if (matcher.find()) {
                            Optional<QualifiedLayerName> newCurrentLayerName = layerNameResolver.resolveFromNonQualifiedLayerName(matcher.group("layer"));
                            if (newCurrentLayerName.isPresent()) {
                                currentLayerName.set(newCurrentLayerName.get());
                            } else {
                                currentLayerName.set(null);
                            }
                        }
                        return Mono.just(line);
                    }
                })
                .collectList()
                .map(outputBodyLines -> String.join("\n", outputBodyLines));
    }

}
