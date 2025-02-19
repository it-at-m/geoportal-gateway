package de.swm.lhm.geoportal.gateway.geoservice.filter;

import de.swm.lhm.geoportal.gateway.geoservice.authorization.GeoServiceAuthorizationService;
import de.swm.lhm.geoportal.gateway.geoservice.gfi.GfiFilter;
import de.swm.lhm.geoportal.gateway.geoservice.inspect.GeoServiceInspectorService;
import de.swm.lhm.geoportal.gateway.geoservice.inspect.GeoServiceRequest;
import de.swm.lhm.geoportal.gateway.geoservice.inspect.GeoServiceRequestType;
import de.swm.lhm.geoportal.gateway.geoservice.inspect.QualifiedLayerName;
import de.swm.lhm.geoportal.gateway.util.XmlUtils;
import de.swm.lhm.geoportal.gateway.util.messagebody.MessageBodyEncodingService;
import jakarta.annotation.PostConstruct;
import lombok.SneakyThrows;
import org.springframework.stereotype.Component;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import reactor.core.publisher.Mono;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;

@Component
public class GetCapabilitiesGatewayFilter extends AbstractGeoServiceResponseXmlGatewayFilter {

    private final GeoServiceAuthorizationService geoServiceAuthorizationService;
    private final List<GfiFilter> gfiFilters;
    private ThreadLocal<XPathExpressions> xPathExpressions;

    public GetCapabilitiesGatewayFilter(GeoServiceInspectorService geoServiceInspectorService, GeoServiceAuthorizationService geoServiceAuthorizationService,
            List<GfiFilter> gfiFilters, MessageBodyEncodingService messageBodyEncodingService) {
        super(geoServiceInspectorService, messageBodyEncodingService);
        this.geoServiceAuthorizationService = geoServiceAuthorizationService;
        this.gfiFilters = gfiFilters;
    }

    @PostConstruct
    private void precompileXpathExpressions() {
        this.xPathExpressions = ThreadLocal.withInitial(XPathExpressions::new);
    }

    @Override
    protected Boolean isApplicable(GeoServiceRequest geoServiceRequest) {
        return geoServiceRequest.is(GeoServiceRequestType.GET_CAPABILITIES);
    }

    private Document removeBlockedRequests(Document document) {
        XPathExpressions localXPathExpressions = xPathExpressions.get();

        // nodes get removed after all xpath expressions have been applied to
        // avoid modifying a document while it is queried.
        List<Node> nodesToRemove = new ArrayList<>();

        // prune operations based on blocked-request-types
        XmlUtils.xPathToStream(localXPathExpressions.xpWfsWmtsOperationNames, document)
                .forEach(node -> {
                    if (geoServiceInspectorService.isBlockedRequestType(node.getTextContent()))
                        nodesToRemove.add(((Attr) node).getOwnerElement());
                });

        XmlUtils.xPathToStream(localXPathExpressions.xpWmsCapabilities, document)
                .forEach(node -> {
                    if (
                            geoServiceInspectorService.isBlockedRequestType(node.getLocalName())
                                    || geoServiceInspectorService.isBlockedRequestType(node.getNodeName())
                    )
                        nodesToRemove.add(node);
                });

        if (geoServiceInspectorService.isBlockedRequestType("getlegendgraphic")) {
            XmlUtils.xPathToStream(localXPathExpressions.xpLegendUrl, document)
                    .forEach(nodesToRemove::add);
        }

        XmlUtils.removeNodes(nodesToRemove);

        return document;
    }

    Mono<Document> removeNonVisibleLayers(Document document, GeoServiceRequest geoServiceRequest) {
        return geoServiceAuthorizationService.getNonHiddenGeoServiceLayersLowercased()
                .map(allowedLayerNames -> {

                    XPathExpressions localXPathExpressions = xPathExpressions.get();

                    Predicate<String> isLayerAllowed = layerName ->
                            QualifiedLayerName.fromStringWithWorkspaceFallback(
                                            geoServiceRequest.getWorkspaceName()
                                                    .map(String::toLowerCase),
                                            layerName.toLowerCase(Locale.ROOT))
                                    .map(allowedLayerNames::contains)
                                    .orElse(false);

                    // nodes get removed after all xpath expressions have been applied to
                    // avoid modifying a document while it is queried.
                    List<Node> nodesToRemove = new ArrayList<>();

                    // prune layers
                    for (XPathExpression xPathExpression : List.of(localXPathExpressions.xpWfsFeatureTypeNames, localXPathExpressions.xpWmsLayerNames,
                            localXPathExpressions.xpWmtsLayerIdentifiers)) {
                        XmlUtils.xPathToStream(xPathExpression, document)
                                .filter(node -> !isLayerAllowed.test(node.getTextContent()))
                                .forEach(node ->
                                        // remove the parent node, see used xpath expressions
                                        nodesToRemove.add(node.getParentNode())
                                );
                    }

                    // remove all marked for removal
                    XmlUtils.removeNodes(nodesToRemove);

                    return document;
                });
    }

    Document removeNonSupportedGetFeatureInfoFormats(Document document) {
        List<Node> nodesToRemove = new ArrayList<>();

        XmlUtils.xPathToStream(xPathExpressions.get().xpWmsGfiFormats, document)
                .filter(node -> {
                    String format = node.getTextContent().strip();
                    return gfiFilters.stream()
                            .noneMatch(gfiFilter -> gfiFilter.supportsFormat(format));
                })
                .forEach(node ->
                        // remove the parent node, see used xpath expressions
                        nodesToRemove.add(node.getParentNode())
                );
        XmlUtils.removeNodes(nodesToRemove);

        return document;
    }

    @Override
    protected Mono<Document> rewriteDocument(Document document, GeoServiceRequest geoServiceRequest) {
        return Mono.fromCallable(() -> removeBlockedRequests(document))
                .flatMap(document1 -> removeNonVisibleLayers(document1, geoServiceRequest))
                .map(this::removeNonSupportedGetFeatureInfoFormats)
                .then(Mono.just(document));
    }

    protected static class XPathExpressions {
        final XPath xPath;
        XPathExpression xpWfsFeatureTypeNames;
        XPathExpression xpWmsLayerNames;
        XPathExpression xpWmtsLayerIdentifiers;
        XPathExpression xpWfsWmtsOperationNames;
        XPathExpression xpWmsCapabilities;
        XPathExpression xpLegendUrl;
        XPathExpression xpWmsGfiFormats;

        public XPathExpressions() {
            this.xPath = XmlUtils.getSecuredXPathFactory().newXPath();
            precompileXpathExpressions();
        }

        @SneakyThrows
        private void precompileXpathExpressions() {
            this.xpWfsFeatureTypeNames = xPath.compile("//*[local-name() = 'FeatureType']/*[local-name() = 'Name']");
            this.xpWmsLayerNames = xPath.compile("//*[local-name() = 'Layer']/*[local-name() = 'Name']");
            this.xpWmtsLayerIdentifiers = xPath.compile("//*[local-name() = 'Layer']/*[local-name() = 'Identifier']");
            this.xpWfsWmtsOperationNames = xPath.compile("//*[local-name() = 'OperationsMetadata']/*[local-name() = 'Operation']/@name");
            this.xpWmsCapabilities = xPath.compile("//*[local-name() = 'Capability']/*[local-name() = 'Request']/*");
            this.xpLegendUrl = xPath.compile("//*[local-name() = 'LegendURL']");
            this.xpWmsGfiFormats = xPath.compile(
                    "//*[local-name() = 'Capability']/*[local-name() = 'Request']/*[local-name() = 'GetFeatureInfo']/*[local-name() = 'Format']");
        }
    }
}
