package de.swm.lhm.geoportal.gateway.geoservice.gfi;

import de.swm.lhm.geoportal.gateway.geoservice.GeoServiceProperties;
import de.swm.lhm.geoportal.gateway.geoservice.inspect.QualifiedLayerName;
import de.swm.lhm.geoportal.gateway.util.MediaTypeExt;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

class GmlGfiFilterTest extends AbstractGfiFilterTest {

    private GeoServiceProperties getGeoServiceProperties() {
        GeoServiceProperties geoServiceProperties = new GeoServiceProperties();
        geoServiceProperties.setMaxXmlParsingDurationMs(500);
        return geoServiceProperties;
    }

    @Test
    void supportsGeoServerAnnouncedFormats() {
        // formats announced in geoserver capabilities
        GmlGfiFilter filter = new GmlGfiFilter(mockPropertyFieldService(), getGeoServiceProperties());
        assertThat(filter.supportsFormat(MediaTypeExt.APPLICATION_VND_OGC_GML_VALUE), is(true));
        assertThat(filter.supportsFormat("application/vnd.ogc.gml/3.1.1"), is(true));
        assertThat(filter.supportsFormat("text/xml"), is(true));
        assertThat(filter.supportsFormat("text/xml; subtype=gml/3.1.1"), is(true));
        assertThat(filter.supportsFormat("application/xml"), is(true));
    }

    @Test
    void filterBodyWithFixedColumns() throws IOException {
        GmlGfiFilter filter = new GmlGfiFilter(mockPropertyFieldService(), getGeoServiceProperties());

        Set<String> allowedColumns = Set.of("name", "osm_id", "shape");
        QualifiedLayerName qLayerName = QualifiedLayerName.fromString("play:gis_osm_pois_free_1_o2o");
        LayerNameResolver layerNameResolver = new LayerNameResolver();
        layerNameResolver.add(qLayerName);

        String filteredBody = filter.filterBody(
                layerNameResolver,
                loadFileContent("geoservice/get-feature-info/gis_osm_pois_free_1_o2o.xml"),
                referencedColumnValue -> {
                    if (referencedColumnValue.qualifiedLayerName().equals(qLayerName) && allowedColumns.contains(referencedColumnValue.columnName())) {
                        if (referencedColumnValue.columnName().equals("osm_id")) {
                            return Mono.just("new " + referencedColumnValue.columnValue());
                        } else {
                            return Mono.just(referencedColumnValue.columnValue());
                        }
                    } else {
                        return Mono.empty();
                    }
                }
        ).block(Duration.ofSeconds(2));

        assertThat(filteredBody, not(containsString("fclass")));
        assertThat(filteredBody, not(containsString("vending_any")));
        assertThat(filteredBody, containsString("new 4873"));
        assertThat(filteredBody, containsString("play:osm_id"));
        assertThat(filteredBody, containsString("gml:coordinates"));
        assertThat(filteredBody, containsString("690319.5,5335033.8"));
    }
}
