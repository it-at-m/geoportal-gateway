package de.swm.lhm.geoportal.gateway.geoservice.inspect.xml;


import de.swm.lhm.geoportal.gateway.geoservice.inspect.GeoServiceRequestType;
import de.swm.lhm.geoportal.gateway.shared.model.ServiceType;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

class ParseWmsXmlTest extends AbstractParseXmlTest {

    @Test
    void wmsGetCapabilities() {
        Optional<GeoServiceXmlRequestParameters> params = parse(TestBodies.GET_CAPABILITIES.body, geoServiceXmlRequestDocumentParser.getWmsGetCapabilitiesParser());
        assertThat(params.isPresent(), is(true));
        assertThat(params.get().getGeoServiceType(), is(ServiceType.WMS));
        assertThat(params.get().getGeoServiceRequestType(), is(GeoServiceRequestType.GET_CAPABILITIES));
        assertThat(params.get().getRequestedLayers(), is(Collections.emptySet()));
    }

    @Test
    void wmsGetMap() {
        Optional<GeoServiceXmlRequestParameters> params = parse(TestBodies.GET_MAP.body, geoServiceXmlRequestDocumentParser.getWmsGetMapParser());
        assertThat(params.isPresent(), is(true));
        assertThat(params.get().getGeoServiceType(), is(ServiceType.WMS));
        assertThat(params.get().getGeoServiceRequestType(), is(GeoServiceRequestType.GET_MAP));
        assertThat(params.get().getRequestedLayers(), is(Set.of("topp:states")));
    }

    @Test
    void allWmsBodiesParsable() {
        assertBodiesParsable(
                Arrays.stream(TestBodies.values()).map(v -> v.body).toList(),
                ServiceType.WMS
        );
    }

    enum TestBodies {
        GET_CAPABILITIES("""
                <?xml version="1.0" encoding="UTF-8"?>
                <GetCapabilities service="WMS" version="1.3.0" xmlns="http://www.opengis.net/wms"></GetCapabilities>
                """),

        GET_MAP("""
                        <ogc:GetMap xmlns:ogc="http://www.opengis.net/ows"
                                    xmlns:gml="http://www.opengis.net/gml"
                           version="1.1.1" service="WMS">
                           <StyledLayerDescriptor version="1.0.0">
                              <NamedLayer>
                                <Name>topp:states</Name>
                                <NamedStyle><Name>population</Name></NamedStyle>
                              </NamedLayer>
                           </StyledLayerDescriptor>
                           <BoundingBox srsName="http://www.opengis.net/gml/srs/epsg.xml#4326">
                              <gml:coord><gml:X>-130</gml:X><gml:Y>24</gml:Y></gml:coord>
                              <gml:coord><gml:X>-55</gml:X><gml:Y>50</gml:Y></gml:coord>
                           </BoundingBox>
                           <Output>
                              <Format>image/png</Format>
                              <Size><Width>550</Width><Height>250</Height></Size>
                           </Output>
                        </ogc:GetMap>
                """);
        final String body;

        TestBodies(String body) {
            this.body = body;
        }
    }

}
