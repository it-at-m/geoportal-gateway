package de.swm.lhm.geoportal.gateway.geoservice.gfi;

import de.swm.lhm.geoportal.gateway.geoservice.inspect.QualifiedLayerName;
import de.swm.lhm.geoportal.gateway.util.MediaTypeExt;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

class GeoJsonGfiFilterTest extends AbstractGfiFilterTest {

    @Test
    void supportsGeoServerAnnouncedFormats() {
        // formats announced in geoserver capabilities
        GeoJsonGfiFilter filter = new GeoJsonGfiFilter(mockPropertyFieldService());
        assertThat(filter.supportsFormat(MediaType.APPLICATION_JSON_VALUE), is(true));
        assertThat(filter.supportsFormat(MediaTypeExt.APPLICATION_GEO_JSON.toString()), is(true));
        assertThat(filter.supportsFormat(MediaTypeExt.APPLICATION_VND_GEO_JSON.toString()), is(true));
    }


    @Test
    void filterBodyWithFixedColumns() throws IOException {
        GeoJsonGfiFilter filter = new GeoJsonGfiFilter(mockPropertyFieldService());

        Set<String> allowedColumns = Set.of("name", "osm_id", "shape");
        QualifiedLayerName someQLayerName = QualifiedLayerName.fromString("myws:gis_osm_pois_free_1_o2o");
        LayerNameResolver layerNameResolver = new LayerNameResolver();
        layerNameResolver.add(someQLayerName);
        String filteredBody = filter.filterBody(layerNameResolver,
                loadFileContent("geoservice/get-feature-info/gis_osm_pois_free_1_o2o.json"),
                referencedColumnValue -> {
                    if (allowedColumns.contains(referencedColumnValue.columnName())) {
                        return Mono.just(referencedColumnValue.columnValue());
                    } else {
                        return Mono.empty();
                    }
                }
        ).block();

        assertThat(filteredBody, not(containsString("fclass")));
    }

    @Test
    void shouldReturnFalseIfMediaTypeIsNull() {
        GeoJsonGfiFilter filter = new GeoJsonGfiFilter(mockPropertyFieldService());
        Assertions.assertThat(filter.supportsFormat(null)).isFalse();
    }
}
