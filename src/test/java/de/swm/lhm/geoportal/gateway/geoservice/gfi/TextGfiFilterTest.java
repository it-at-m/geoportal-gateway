package de.swm.lhm.geoportal.gateway.geoservice.gfi;

import de.swm.lhm.geoportal.gateway.geoservice.inspect.QualifiedLayerName;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

class TextGfiFilterTest extends AbstractGfiFilterTest {

    @Test
    void supportsGeoServerAnnouncedFormats() {
        TextGfiFilter filter = new TextGfiFilter(mockPropertyFieldService());
        assertThat(filter.supportsFormat(MediaType.TEXT_PLAIN_VALUE), is(true));
    }

    @Test
    void filterBodyWithFixedColumns() throws IOException {
        TextGfiFilter filter = new TextGfiFilter(mockPropertyFieldService());

        Set<String> allowedColumns = Set.of("name", "osm_id", "shape");
        QualifiedLayerName qLayerName = QualifiedLayerName.fromString("play:gis_osm_pois_free_1_o2o");
        LayerNameResolver layerNameResolver = new LayerNameResolver();
        layerNameResolver.add(qLayerName, qLayerName);
        String filteredBody = filter.filterBody(
                layerNameResolver,
                loadFileContent("geoservice/get-feature-info/gis_osm_pois_free_1_o2o.txt"),
                referencedColumnValue -> {
                    if (referencedColumnValue.qualifiedLayerName().equals(qLayerName) && allowedColumns.contains(referencedColumnValue.columnName())) {
                        return Mono.just("new " + referencedColumnValue.columnValue());
                    } else {
                        return Mono.empty();
                    }
                }
        ).block();

        assertThat(filteredBody, is("""
                Results for FeatureType 'playgound:gis_osm_pois_free_1_o2o':
                --------------------------------------------
                osm_id = new 4873640911
                name = new null
                shape = new [GEOMETRY (Point) with 1 points]
                --------------------------------------------"""));
    }

    @Test
    void filterBodyWithFixedColumnsNonResolvedLayer() throws IOException {
        TextGfiFilter filter = new TextGfiFilter(mockPropertyFieldService());

        Set<String> allowedColumns = Set.of("name", "osm_id", "shape");
        LayerNameResolver layerNameResolver = new LayerNameResolver();
        String filteredBody = filter.filterBody(
                layerNameResolver,
                loadFileContent("geoservice/get-feature-info/gis_osm_pois_free_1_o2o.txt"),
                referencedColumnValue -> {
                    if (referencedColumnValue.qualifiedLayerName()
                            .equals(QualifiedLayerName.fromString("play:gis_osm_pois_free_1_o2o")) && allowedColumns.contains(referencedColumnValue.columnName())) {
                        return Mono.just("new " + referencedColumnValue.columnValue());
                    } else {
                        return Mono.empty();
                    }
                }
        ).block();

        assertThat(filteredBody, is("""
                Results for FeatureType 'playgound:gis_osm_pois_free_1_o2o':
                --------------------------------------------
                --------------------------------------------"""));
    }

    @Test
    void shouldReturnFalseIfMediaTypeIsNull() {
        TextGfiFilter filter = new TextGfiFilter(mockPropertyFieldService());
        Assertions.assertThat(filter.supportsFormat(null)).isFalse();
    }
}
