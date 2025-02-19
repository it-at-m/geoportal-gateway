package de.swm.lhm.geoportal.gateway.geoservice.inspect.xml;

import org.w3c.dom.Document;

import javax.xml.xpath.XPathExpressionException;
import java.util.Optional;

class FirstMatchingXPathParserCombinator implements XPathParser {

    private final XPathParser[] branches;

    public FirstMatchingXPathParserCombinator(XPathParser... branches) {
        this.branches = branches;
    }

    @Override
    public Optional<GeoServiceXmlRequestParameters> accept(Document document) throws XPathExpressionException {
        for (XPathParser parser : branches) {
            Optional<GeoServiceXmlRequestParameters> result = parser.accept(document);
            if (result.isPresent()) {
                return result;
            }
        }
        return Optional.empty();
    }
}
