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

class ParseWmtsXmlTest extends AbstractParseXmlTest {

    @Test
    void wmtsGetCapabilities() {
        Optional<GeoServiceXmlRequestParameters> params = parse(TestBodies.GET_CAPABILITIES.body, geoServiceXmlRequestDocumentParser.getWmtsGetCapabilitiesParser());
        assertThat(params.isPresent(), is(true));
        assertThat(params.get().getGeoServiceType(), is(ServiceType.WMTS));
        assertThat(params.get().getGeoServiceRequestType(), is(GeoServiceRequestType.GET_CAPABILITIES));
        assertThat(params.get().getRequestedLayers(), is(Collections.emptySet()));
    }

    @Test
    void wmtsGetTile() {
        Optional<GeoServiceXmlRequestParameters> params = parse(TestBodies.GET_TILE.body, geoServiceXmlRequestDocumentParser.getWmtsGetTileParser());
        assertThat(params.isPresent(), is(true));
        assertThat(params.get().getGeoServiceType(), is(ServiceType.WMTS));
        assertThat(params.get().getGeoServiceRequestType(), is(GeoServiceRequestType.GET_TILE));
        assertThat(params.get().getRequestedLayers(), is(Set.of("etopo2")));
    }

    @Test
    void wmtsGetFeatureInfo() {
        Optional<GeoServiceXmlRequestParameters> params = parse(TestBodies.GET_FEATURE_INFO.body, geoServiceXmlRequestDocumentParser.getWmtsGetFeatureInfoParser());
        assertThat(params.isPresent(), is(true));
        assertThat(params.get().getGeoServiceType(), is(ServiceType.WMTS));
        assertThat(params.get().getGeoServiceRequestType(), is(GeoServiceRequestType.GET_FEATURE_INFO));
        assertThat(params.get().getRequestedLayers(), is(Set.of("etopo2")));
    }

    @Test
    void allWmtsBodiesParsable() {
        assertBodiesParsable(
                Arrays.stream(TestBodies.values()).map(v -> v.body).toList(),
                ServiceType.WMTS
        );
    }

    enum TestBodies {
        GET_CAPABILITIES("""
                <?xml version="1.0" encoding="UTF-8"?>
                <GetCapabilities service="WMTS" version="1.0.0"></GetCapabilities>
                """),

        GET_TILE("""
                <?xml version="1.0" encoding="UTF-8"?>
                <GetTile service="WMTS" version="1.0.0"
                xmlns="http://www.opengis.net/wmts/1.0">
                <Layer>etopo2</Layer>
                <Style>default</Style>
                <Format>image/png</Format>
                <TileMatrixSet> WholeWorld_CRS_84</TileMatrixSet>
                <TileMatrix>10m</TileMatrix>
                <TileRow>1</TileRow>
                <TileCol>3</TileCol>
                </GetTile>
                """),

        GET_FEATURE_INFO("""
                <?xml version="1.0" encoding="UTF-8"?>
                <GetFeatureInfo service="WMTS" version="1.0.0"
                xmlns="http://www.opengis.net/wmts/1.0">
                <GetTile service="WMTS" version="1.0.0"
                xmlns="http://www.opengis.net/wmts/1.0">
                <Layer>etopo2</Layer>
                <Style>default</Style>
                <Format>image/png</Format>
                <TileMatrixSet> WholeWorld_CRS_84</TileMatrixSet>
                <TileMatrix>10m</TileMatrix>
                <TileRow>1</TileRow>
                <TileCol>3</TileCol>
                </GetTile>
                <J>86</J>
                <I>132</I>
                <InfoFormat>application/gml+xml; version=3.1</InfoFormat>
                </GetFeatureInfo>
                """);
        final String body;

        TestBodies(String body) {
            this.body = body;
        }
    }
}
