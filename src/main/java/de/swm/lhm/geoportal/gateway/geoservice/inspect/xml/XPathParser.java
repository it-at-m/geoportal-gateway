package de.swm.lhm.geoportal.gateway.geoservice.inspect.xml;

import org.w3c.dom.Document;

import javax.xml.xpath.XPathExpressionException;
import java.util.Optional;

public interface XPathParser {
    Optional<GeoServiceXmlRequestParameters> accept(Document document) throws XPathExpressionException;
}
