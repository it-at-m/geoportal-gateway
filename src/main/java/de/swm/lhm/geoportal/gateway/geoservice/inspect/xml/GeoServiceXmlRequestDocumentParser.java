package de.swm.lhm.geoportal.gateway.geoservice.inspect.xml;

import de.swm.lhm.geoportal.gateway.geoservice.inspect.GeoServiceRequestType;
import de.swm.lhm.geoportal.gateway.shared.model.ServiceType;
import de.swm.lhm.geoportal.gateway.util.XmlUtils;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Slf4j
public class GeoServiceXmlRequestDocumentParser {

    final DocumentBuilderFactory documentBuilderFactory = XmlUtils.getSecuredDocumentBuilderFactory();
    final ThreadLocal<XPathExpressions> xpathExpressions;

    public GeoServiceXmlRequestDocumentParser() {
        // This is intentionally disabled to make the parser more tolerant in regard
        // to further evolving xml schemata as well as unclear namespacing like
        // http://www.opengis.net/ows vs http://www.opengis.net/wfs
        documentBuilderFactory.setNamespaceAware(false);

        // xpath expressions can not be safely used from multiple threads
        this.xpathExpressions = ThreadLocal.withInitial(XPathExpressions::new);
    }

    protected XPathParser getAllParsers() {
        return new FirstMatchingXPathParserCombinator(
                getWmsParser(),
                getWmtsParser(),
                getWfsParser(),
                getWfs20Parser()
        );
    }

    public Optional<GeoServiceXmlRequestParameters> parseXmlString(String xmlString) {
        return parseXmlStringToDocument(xmlString, getAllParsers());
    }

    protected Optional<GeoServiceXmlRequestParameters> parseXmlStringToDocument(String xmlString, XPathParser parser) {
        try {
            DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
            Document document = documentBuilder.parse(IOUtils.toInputStream(xmlString, StandardCharsets.UTF_8));
            return parser.accept(document);
        } catch (ParserConfigurationException | IOException | SAXException | XPathExpressionException e) {
            log.info("unable to parse xml request", e);
            return Optional.empty();
        }
    }

    protected XPathParser getWmtsGetCapabilitiesParser() {
        return makeParser(xpathExpressions.get().xpWmtsGetCapabilities, document -> {
            GeoServiceXmlRequestParameters parameters = new GeoServiceXmlRequestParameters();
            parameters.setGeoServiceType(ServiceType.WMTS);
            parameters.setGeoServiceRequestType(GeoServiceRequestType.GET_CAPABILITIES);
            return parameters;
        });
    }

    protected XPathParser getWmtsGetTileParser() {
        XPathExpressions localXpathExpressions = xpathExpressions.get();
        return makeParser(localXpathExpressions.xpWmtsGetTile, document -> {
            GeoServiceXmlRequestParameters parameters = new GeoServiceXmlRequestParameters();
            parameters.setGeoServiceType(ServiceType.WMTS);
            parameters.setGeoServiceRequestType(GeoServiceRequestType.GET_TILE);

            XmlUtils.xPathToStream(localXpathExpressions.xpWmtsGetTileLayersWholeTree, document)
                    .filter(tag -> tag.getNodeType() == Node.ELEMENT_NODE)
                    .forEach(tag -> addRequestedLayer(parameters, tag.getTextContent()));

            return parameters;
        });
    }

    protected XPathParser getWmtsGetFeatureInfoParser() {
        XPathExpressions localXpathExpressions = xpathExpressions.get();
        return makeParser(localXpathExpressions.xpWmtsGetFeatureInfo, document -> {
            GeoServiceXmlRequestParameters parameters = new GeoServiceXmlRequestParameters();
            parameters.setGeoServiceType(ServiceType.WMTS);
            parameters.setGeoServiceRequestType(GeoServiceRequestType.GET_FEATURE_INFO);

            // request contains an inner GetTile request
            XmlUtils.xPathToStream(localXpathExpressions.xpWmtsGetTileLayersWholeTree, document)
                    .filter(tag -> tag.getNodeType() == Node.ELEMENT_NODE)
                    .forEach(tag -> addRequestedLayer(parameters, tag.getTextContent()));

            return parameters;
        });
    }

    private XPathParser getWmtsParser() {
        return new FirstMatchingXPathParserCombinator(
                getWmtsGetCapabilitiesParser(),
                getWmtsGetTileParser(),
                getWmtsGetFeatureInfoParser()
        );
    }

    protected XPathParser getWmsGetCapabilitiesParser() {
        return makeParser(xpathExpressions.get().xpWmsGetCapabilities, document -> {
            GeoServiceXmlRequestParameters parameters = new GeoServiceXmlRequestParameters();
            parameters.setGeoServiceType(ServiceType.WMS);
            parameters.setGeoServiceRequestType(GeoServiceRequestType.GET_CAPABILITIES);
            return parameters;
        });
    }

    protected XPathParser getWmsGetMapParser() {
        return makeParser(xpathExpressions.get().xpWmsGetMap, document -> {
            GeoServiceXmlRequestParameters parameters = new GeoServiceXmlRequestParameters();
            parameters.setGeoServiceType(ServiceType.WMS);
            parameters.setGeoServiceRequestType(GeoServiceRequestType.GET_MAP);

            XmlUtils.xPathToStream(xpathExpressions.get().xpNamedLayers, document)
                    .filter(tag -> tag.getNodeType() == Node.ELEMENT_NODE)
                    .forEach(tag -> addRequestedLayer(parameters, tag.getTextContent()));

            return parameters;
        });
    }

    private XPathParser getWmsParser() {
        return new FirstMatchingXPathParserCombinator(
                getWmsGetCapabilitiesParser(),
                getWmsGetMapParser()

                // GetLegendGraphic does not appear to have a XML representation.
                // GetFeatureInfo does not appear to have a XML representation.
        );
    }

    protected XPathParser getWfsGetCapabilitiesParser() {
        return makeParser(xpathExpressions.get().xpWfsGetCapabilities, document -> {
            GeoServiceXmlRequestParameters parameters = new GeoServiceXmlRequestParameters();
            parameters.setGeoServiceType(ServiceType.WFS);
            parameters.setGeoServiceRequestType(GeoServiceRequestType.GET_CAPABILITIES);
            return parameters;
        });
    }

    protected XPathParser getWfsGetFeatureParser() {
        XPathExpressions localXpathExpressions = xpathExpressions.get();
        return makeParser(localXpathExpressions.xpWfsGetFeature, document -> {
            GeoServiceXmlRequestParameters parameters = new GeoServiceXmlRequestParameters();
            parameters.setGeoServiceType(ServiceType.WFS);
            parameters.setGeoServiceRequestType(GeoServiceRequestType.GET_FEATURE);
            XmlUtils.xPathToStream(localXpathExpressions.xpWfsTypeNameAttributes, document)
                    .forEach(typeName -> addRequestedLayer(parameters, typeName.getTextContent()));
            return parameters;
        });
    }

    protected XPathParser getWfsDescribeFeatureTypeParser() {
        XPathExpressions localXpathExpressions = xpathExpressions.get();
        return makeParser(localXpathExpressions.xpWfsDescribeFeatureType, document -> {
            GeoServiceXmlRequestParameters parameters = new GeoServiceXmlRequestParameters();
            parameters.setGeoServiceType(ServiceType.WFS);
            parameters.setGeoServiceRequestType(GeoServiceRequestType.DESCRIBE_FEATURE_TYPE);
            XmlUtils.xPathToStream(localXpathExpressions.xpWfsTypeNameTags, document)
                    .forEach(typeName -> addRequestedLayer(parameters, typeName.getTextContent()));
            return parameters;
        });
    }

    private XPathParser getWfsParser() {
        return new FirstMatchingXPathParserCombinator(
                getWfsGetCapabilitiesParser(),
                getWfsGetFeatureParser(),
                getWfsDescribeFeatureTypeParser()
        );
    }

    protected XPathParser getWfs20TransactionParser() {
        XPathExpressions localXpathExpressions = xpathExpressions.get();
        return makeParser(localXpathExpressions.xpWfs20Transaction, document -> {
            GeoServiceXmlRequestParameters parameters = new GeoServiceXmlRequestParameters();
            parameters.setGeoServiceRequestType(GeoServiceRequestType.TRANSACTION);
            parameters.setGeoServiceType(ServiceType.WFS);
            XmlUtils.xPathToStream(localXpathExpressions.xpWfsTypeNameAttributes, document)
                    .forEach(typeName ->
                            addRequestedLayer(parameters, typeName.getTextContent()));

            for (XPathExpression expression : List.of(localXpathExpressions.xpWfs20TransactionReplace, localXpathExpressions.xpWfs20TransactionInsert)) {
                XmlUtils.xPathToStream(expression, document)
                        .filter(tag -> tag.getNodeType() == Node.ELEMENT_NODE)
                        .forEach(tag -> addRequestedLayer(parameters, tag.getNodeName()));
            }
            return parameters;
        });
    }

    protected XPathParser getWfs20GetPropertyValueParser() {
        XPathExpressions localXpathExpressions = xpathExpressions.get();
        return makeParser(localXpathExpressions.xpWfs20GetPropertyValue, document -> {
            GeoServiceXmlRequestParameters parameters = new GeoServiceXmlRequestParameters();
            parameters.setGeoServiceType(ServiceType.WFS);
            parameters.setGeoServiceRequestType(GeoServiceRequestType.GET_PROPERTY_VALUE);
            XmlUtils.xPathToStream(localXpathExpressions.xpWfs20TypeNamesPluralAttributes, document)
                    .forEach(typeNames ->
                            addRequestedLayerCommaSeperated(parameters, typeNames.getTextContent()));
            return parameters;
        });
    }

    protected XPathParser getWfs20GetFeatureWithLockParser() {
        XPathExpressions localXpathExpressions = xpathExpressions.get();
        return makeParser(localXpathExpressions.xpWfs20GetFeatureWithLock, document -> {
            GeoServiceXmlRequestParameters parameters = new GeoServiceXmlRequestParameters();
            parameters.setGeoServiceType(ServiceType.WFS);
            parameters.setGeoServiceRequestType(GeoServiceRequestType.GET_FEATURE_WITH_LOCK);
            XmlUtils.xPathToStream(localXpathExpressions.xpWfs20TypeNamesPluralAttributes, document)
                    .forEach(typeNames ->
                            addRequestedLayerCommaSeperated(parameters, typeNames.getTextContent()));
            return parameters;
        });
    }

    protected XPathParser getWfs20CreateStoredQueryParser() {
        XPathExpressions localXpathExpressions = xpathExpressions.get();

        return makeParser(localXpathExpressions.xpWfs20CreateStoredQuery, document -> {
            GeoServiceXmlRequestParameters parameters = new GeoServiceXmlRequestParameters();
            parameters.setGeoServiceType(ServiceType.WFS);
            parameters.setGeoServiceRequestType(GeoServiceRequestType.CREATE_STORED_QUERY);
            XmlUtils.xPathToStream(localXpathExpressions.xpWfs20TypeNamesPluralAttributes, document)
                    .forEach(typeNames ->
                            addRequestedLayerCommaSeperated(parameters, typeNames.getTextContent()));
            XmlUtils.xPathToStream(localXpathExpressions.xpWfs20QueryExpressionReturnFeatureTypes, document)
                    .forEach(typeNames ->
                            addRequestedLayerCommaSeperated(parameters, typeNames.getTextContent()));

            return parameters;
        });
    }

    protected XPathParser getWfs20DropStoredQueryParser() {
        return makeParser(xpathExpressions.get().xpWfs20DropStoredQuery, document -> {
            GeoServiceXmlRequestParameters parameters = new GeoServiceXmlRequestParameters();
            parameters.setGeoServiceType(ServiceType.WFS);
            parameters.setGeoServiceRequestType(GeoServiceRequestType.DROP_STORED_QUERY);
            return parameters;
        });
    }

    protected XPathParser getWfs20ListStoredQueriesParser() {
        return makeParser(xpathExpressions.get().xpWfs20ListStoredQueries, document -> {
            GeoServiceXmlRequestParameters parameters = new GeoServiceXmlRequestParameters();
            parameters.setGeoServiceType(ServiceType.WFS);
            parameters.setGeoServiceRequestType(GeoServiceRequestType.LIST_STORED_QUERIES);
            return parameters;
        });
    }

    protected XPathParser getWfs20DescribeStoredQueriesParser() {
        return makeParser(xpathExpressions.get().xpWfs20DescribeStoredQueries, document -> {
            GeoServiceXmlRequestParameters parameters = new GeoServiceXmlRequestParameters();
            parameters.setGeoServiceType(ServiceType.WFS);
            parameters.setGeoServiceRequestType(GeoServiceRequestType.DESCRIBE_STORED_QUERIES);
            return parameters;
        });
    }

    private XPathParser getWfs20Parser() {
        return new FirstMatchingXPathParserCombinator(
                getWfs20TransactionParser(),
                getWfs20GetPropertyValueParser(),
                getWfs20GetFeatureWithLockParser(),
                getWfs20CreateStoredQueryParser(),
                getWfs20DropStoredQueryParser(),
                getWfs20ListStoredQueriesParser(),
                getWfs20DescribeStoredQueriesParser()
        );
    }

    private XPathParser makeParser(XPathExpression rootElementMatcher, NonOptionalXPathParser innerParser) {
        return document -> {
            NodeList nodeList = (NodeList) rootElementMatcher.evaluate(document, XPathConstants.NODESET);
            if (nodeList == null || nodeList.getLength() == 0) {
                return Optional.empty();
            }
            return Optional.of(innerParser.accept(document));
        };
    }

    private void addRequestedLayer(GeoServiceXmlRequestParameters parameters, String layerName) {
        if (StringUtils.isNotBlank(layerName)) {
            parameters.getRequestedLayers().add(layerName.trim());
        }
    }

    private void addRequestedLayerCommaSeperated(GeoServiceXmlRequestParameters parameters, String layerNames) {
        if (StringUtils.isBlank(layerNames)) {
            return;
        }
        Arrays.stream(layerNames.split(","))
                .forEach(layerName -> addRequestedLayer(parameters, layerName));
    }

    protected static class XPathExpressions {
        final XPath xPath;

        XPathExpression xpWfsTypeNameAttributes;
        XPathExpression xpWfs20TransactionInsert;
        XPathExpression xpWfs20TransactionReplace;
        XPathExpression xpWfs20Transaction;
        XPathExpression xpWfsGetCapabilities;
        XPathExpression xpWmsGetCapabilities;
        XPathExpression xpWmsGetMap;
        XPathExpression xpNamedLayers;
        XPathExpression xpWfs20GetPropertyValue;
        XPathExpression xpWfs20TypeNamesPluralAttributes;
        XPathExpression xpWfs20GetFeatureWithLock;
        XPathExpression xpWfs20CreateStoredQuery;
        XPathExpression xpWfs20QueryExpressionReturnFeatureTypes;
        XPathExpression xpWfs20DropStoredQuery;
        XPathExpression xpWfs20ListStoredQueries;
        XPathExpression xpWfs20DescribeStoredQueries;
        XPathExpression xpWmtsGetCapabilities;
        XPathExpression xpWmtsGetTile;
        XPathExpression xpWmtsGetTileLayersWholeTree;
        XPathExpression xpWmtsGetFeatureInfo;
        XPathExpression xpWfsGetFeature;
        XPathExpression xpWfsTypeNameTags;
        XPathExpression xpWfsDescribeFeatureType;

        public XPathExpressions() {
            this.xPath = XmlUtils.getSecuredXPathFactory().newXPath();
            precompileXpathExpressions();
        }

        @SneakyThrows
        private void precompileXpathExpressions() {
            this.xpWfs20Transaction = xPath.compile("/Transaction");
            this.xpWfs20CreateStoredQuery = xPath.compile("/CreateStoredQuery");
            this.xpWfs20DropStoredQuery = xPath.compile("/DropStoredQuery");
            this.xpWfs20ListStoredQueries = xPath.compile("/ListStoredQueries");
            this.xpWfs20DescribeStoredQueries = xPath.compile("/DescribeStoredQueries");
            this.xpWfs20GetPropertyValue = xPath.compile("/GetPropertyValue");
            this.xpWfs20GetFeatureWithLock = xPath.compile("/GetFeatureWithLock");
            this.xpWfsGetFeature = xPath.compile("/GetFeature");
            this.xpWfsDescribeFeatureType = xPath.compile("/DescribeFeatureType");
            this.xpWfsTypeNameAttributes = xPath.compile("//@typeName");
            this.xpWfsTypeNameTags = xPath.compile("//TypeName");
            this.xpWfs20TypeNamesPluralAttributes = xPath.compile("//@typeNames");
            this.xpWfs20QueryExpressionReturnFeatureTypes = xPath.compile("//QueryExpressionText/@returnFeatureTypes");
            this.xpWfs20TransactionInsert = xPath.compile("/Transaction/Insert/*");
            this.xpWfs20TransactionReplace = xPath.compile("/Transaction/Replace/*[local-name(.) != 'Filter']");
            this.xpWfsGetCapabilities = xPath.compile("/GetCapabilities[@service = 'WFS']");
            this.xpWmsGetCapabilities = xPath.compile("/GetCapabilities[@service = 'WMS']");
            this.xpWmtsGetCapabilities = xPath.compile("/GetCapabilities[@service = 'WMTS']");
            this.xpWmtsGetTile = xPath.compile("/GetTile");
            this.xpWmtsGetFeatureInfo = xPath.compile("/GetFeatureInfo[@service = 'WMTS']");
            this.xpWmtsGetTileLayersWholeTree = xPath.compile("//GetTile/Layer");
            this.xpWmsGetMap = xPath.compile("/GetMap[@service = 'WMS']");
            this.xpNamedLayers = xPath.compile("//NamedLayer/Name");
        }
    }
}
