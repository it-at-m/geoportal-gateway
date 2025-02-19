package de.swm.lhm.geoportal.gateway.geoservice.inspect.xml;

import org.w3c.dom.Document;

import javax.xml.xpath.XPathExpressionException;

interface NonOptionalXPathParser {
    GeoServiceXmlRequestParameters accept(Document document) throws XPathExpressionException;
}
