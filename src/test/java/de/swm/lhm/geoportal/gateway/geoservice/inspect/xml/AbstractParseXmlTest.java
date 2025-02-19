package de.swm.lhm.geoportal.gateway.geoservice.inspect.xml;


import de.swm.lhm.geoportal.gateway.shared.model.ServiceType;

import java.util.List;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

abstract class AbstractParseXmlTest {


    protected GeoServiceXmlRequestDocumentParser geoServiceXmlRequestDocumentParser = new GeoServiceXmlRequestDocumentParser();

    protected Optional<GeoServiceXmlRequestParameters> parse(String xmlString, XPathParser parser) {
        return geoServiceXmlRequestDocumentParser.parseXmlStringToDocument(xmlString, parser);
    }

    void assertBodiesParsable(List<String> bodies, ServiceType expectedGeoServiceType) {
        for (String body : bodies) {
            Optional<GeoServiceXmlRequestParameters> params = geoServiceXmlRequestDocumentParser.parseXmlStringToDocument(body, geoServiceXmlRequestDocumentParser.getAllParsers());
            assertThat(params.isPresent(), is(true));
        }
    }

}
