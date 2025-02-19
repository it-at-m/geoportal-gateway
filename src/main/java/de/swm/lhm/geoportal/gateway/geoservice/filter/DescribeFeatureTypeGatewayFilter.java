package de.swm.lhm.geoportal.gateway.geoservice.filter;

import de.swm.lhm.geoportal.gateway.geoservice.authorization.GeoServiceAuthorizationService;
import de.swm.lhm.geoportal.gateway.geoservice.inspect.GeoServiceInspectorService;
import de.swm.lhm.geoportal.gateway.geoservice.inspect.GeoServiceRequest;
import de.swm.lhm.geoportal.gateway.geoservice.inspect.GeoServiceRequestType;
import de.swm.lhm.geoportal.gateway.geoservice.inspect.QualifiedLayerName;
import de.swm.lhm.geoportal.gateway.util.ExtendedURIBuilder;
import de.swm.lhm.geoportal.gateway.util.XmlUtils;
import de.swm.lhm.geoportal.gateway.util.messagebody.MessageBodyEncodingService;
import jakarta.annotation.PostConstruct;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.NameValuePair;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import reactor.core.publisher.Mono;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Component
public class DescribeFeatureTypeGatewayFilter extends AbstractGeoServiceResponseXmlGatewayFilter {
    private static final Pattern RE_COMPLEX_TYPE_NAME = Pattern.compile(":(?<complexType>[^:]+)$");
    private static final String XSD_ATTR_SCHEMA_LOCATION = "schemaLocation";
    private final GeoServiceAuthorizationService geoServiceAuthorizationService;
    private ThreadLocal<XPathExpressions> xPathExpressions;

    DescribeFeatureTypeGatewayFilter(GeoServiceInspectorService geoServiceInspectorService, GeoServiceAuthorizationService geoServiceAuthorizationService, MessageBodyEncodingService messageBodyEncodingService) {
        super(geoServiceInspectorService, messageBodyEncodingService);
        this.geoServiceAuthorizationService = geoServiceAuthorizationService;
    }

    @PostConstruct
    private void precompileXpathExpressions() {
        this.xPathExpressions = ThreadLocal.withInitial(XPathExpressions::new);
    }

    @Override
    protected Boolean isApplicable(GeoServiceRequest geoServiceRequest) {
        return geoServiceRequest.is(GeoServiceRequestType.DESCRIBE_FEATURE_TYPE);
    }

    @Override
    protected Mono<Document> rewriteDocument(Document document, GeoServiceRequest geoServiceRequest) {
        return geoServiceAuthorizationService.getNonHiddenGeoServiceLayersLowercased()
                .map(allowedLayerNames -> {
                    Stream<DocumentModification> modificationStream = findXsdImportModifications(document, allowedLayerNames);

                    if (geoServiceRequest.getWorkspaceName().isPresent()) {
                        // geoserver only returns types on workspace level. On global DescribeFeatureType requests
                        // links to the individual workspaces are returned
                        modificationStream = Stream.concat(
                                modificationStream,
                                findElementsToRemoveInWorkspacedDocument(document, geoServiceRequest.getWorkspaceName().get(), allowedLayerNames)
                        );
                    }

                    modificationStream.forEach(modification -> {
                        switch (modification) {
                            case DeleteNode deleteNode -> XmlUtils.removeNode(deleteNode.node());
                            case SetSchemaLocationAttribute setSchemaLocationAttribute ->
                                    setSchemaLocationAttribute.node().getAttributes()
                                            .getNamedItem(XSD_ATTR_SCHEMA_LOCATION)
                                            .setTextContent(setSchemaLocationAttribute.newSchemaLocation());
                        }
                    });
                    return document;
                });
    }

    private Stream<DocumentModification> findXsdImportModifications(Document document, Set<QualifiedLayerName> allowedLayerNames) {
        return XmlUtils.xPathToStream(xPathExpressions.get().xpXsdTopLevelImport, document)
                .flatMap(importElement -> findXsdImportElementModifications(importElement, allowedLayerNames));
    }

    private Stream<DocumentModification> findXsdImportElementModifications(Node importElement, Set<QualifiedLayerName> allowedLayerNames) {
        Optional<String> schemaLocation = XmlUtils.getAttributeValue(importElement, XSD_ATTR_SCHEMA_LOCATION);
        if (schemaLocation.isEmpty() || StringUtils.isBlank(schemaLocation.get())) {
            return Stream.empty();
        }

        Optional<GeoServiceRequest> schemaLocationGeoServiceRequest = geoServiceInspectorService.inspectGetRequestWithQueryParams(schemaLocation.get());
        if (schemaLocationGeoServiceRequest.isEmpty() || schemaLocationGeoServiceRequest.get().getLayers().isEmpty()) {
            return Stream.empty();
        }

        // filter layers
        String layersToBePreserved = schemaLocationGeoServiceRequest.get().getLayers()
                .stream()
                .filter(allowedLayerNames::contains)
                .map(QualifiedLayerName::toString)
                .collect(Collectors.joining(","));

        // replace url
        if (StringUtils.isBlank(layersToBePreserved)) {
            return Stream.of(new DeleteNode(importElement));
        } else {
            return Stream.of(new SetSchemaLocationAttribute(
                    importElement,
                    replaceLayerNamesInSchemaLocation(schemaLocation.get(), layersToBePreserved)
            ));
        }
    }

    @SneakyThrows
    private String replaceLayerNamesInSchemaLocation(String schemaLocation, String layerNamesCommaSeperatedList) {
        try {
            // replace the typename-parameter in the schemaLocation url
            ExtendedURIBuilder uriBuilder = new ExtendedURIBuilder(schemaLocation);
            List<NameValuePair> paramsToBePreserved = uriBuilder.getQueryParams()
                    .stream()
                    .filter(nameValuePair -> !StringUtils.equalsIgnoreCase(nameValuePair.getName(), "typename"))
                    .toList();

            uriBuilder.clearParameters();
            uriBuilder.addParameters(paramsToBePreserved);
            uriBuilder.addParameter("typeName", layerNamesCommaSeperatedList);

            return uriBuilder.toString();
        } catch (URISyntaxException e) {
            log.error("Parsing Url from DescribeFeatureType XSD failed", e);
            throw e;
        }
    }

    private Stream<DocumentModification> findElementsToRemoveInWorkspacedDocument(Document document, String workspaceName, Set<QualifiedLayerName> allowedLayerNames) {

        Set<String> complexTypesToRemove = new HashSet<>();
        Stream.Builder<DocumentModification> streamBuilder = Stream.builder();

        XmlUtils.xPathToStream(xPathExpressions.get().xpXsdTopLevelElements, document).forEach(element -> {
            Optional<String> nonQualifiedLayerName = XmlUtils.getAttributeValue(element, "name");
            if (nonQualifiedLayerName.isPresent()) {
                QualifiedLayerName qualifiedLayerName = new QualifiedLayerName(workspaceName, nonQualifiedLayerName.get());
                if (!allowedLayerNames.contains(qualifiedLayerName)) {
                    streamBuilder.add(new DeleteNode(element));

                    Optional<String> qualifiedComplexTypeName = XmlUtils.getAttributeValue(element, "type");
                    if (qualifiedComplexTypeName.isPresent()) {
                        Matcher m = RE_COMPLEX_TYPE_NAME.matcher(qualifiedComplexTypeName.get());
                        if (m.find()) {
                            complexTypesToRemove.add(m.group("complexType"));
                        }
                    }
                }
            }
        });

        // remove the complex-types for layers which are about to be removed
        XmlUtils.xPathToStream(xPathExpressions.get().xpXsdTopLevelComplexTypes, document)
                .forEach(complexTypeNode -> {
                    Optional<String> complexTypeName = XmlUtils.getAttributeValue(complexTypeNode, "name");
                    if (complexTypeName.isPresent() && complexTypesToRemove.contains(complexTypeName.get())) {
                        streamBuilder.add(new DeleteNode(complexTypeNode));
                    }
                });

        return streamBuilder.build();
    }

    private sealed interface DocumentModification {
    }

    private record DeleteNode(Node node) implements DocumentModification {
    }

    private record SetSchemaLocationAttribute(Node node, String newSchemaLocation) implements DocumentModification {
    }


    protected static class XPathExpressions {
        final XPath xPath;

        XPathExpression xpXsdTopLevelElements;
        XPathExpression xpXsdTopLevelComplexTypes;
        XPathExpression xpXsdTopLevelImport;

        public XPathExpressions() {
            this.xPath = XmlUtils.getSecuredXPathFactory().newXPath();
            precompileXpathExpressions();
        }


        @SneakyThrows
        private void precompileXpathExpressions() {
            this.xpXsdTopLevelElements = xPath.compile("//*[local-name() = 'schema']/*[local-name() = 'element']");
            this.xpXsdTopLevelComplexTypes = xPath.compile("//*[local-name() = 'schema']/*[local-name() = 'complexType']");
            this.xpXsdTopLevelImport = xPath.compile("//*[local-name() = 'schema']/*[local-name() = 'import']");
        }
    }
}
