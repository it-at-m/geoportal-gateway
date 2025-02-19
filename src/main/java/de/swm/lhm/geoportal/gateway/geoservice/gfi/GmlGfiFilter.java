package de.swm.lhm.geoportal.gateway.geoservice.gfi;

import de.swm.lhm.geoportal.gateway.geoservice.GeoServiceProperties;
import de.swm.lhm.geoportal.gateway.geoservice.PropertyFieldService;
import de.swm.lhm.geoportal.gateway.geoservice.inspect.QualifiedLayerName;
import de.swm.lhm.geoportal.gateway.util.HttpHeaderUtils;
import de.swm.lhm.geoportal.gateway.util.MediaTypeExt;
import de.swm.lhm.geoportal.gateway.util.ReactiveUtils;
import de.swm.lhm.geoportal.gateway.util.XmlUtils;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.Text;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import java.time.Duration;

@Slf4j
@Component
public class GmlGfiFilter extends AbstractGfiFilter {

    private final GeoServiceProperties geoServiceProperties;

    final DocumentBuilderFactory documentBuilderFactory = XmlUtils.getSecuredDocumentBuilderFactory();
    private final ThreadLocal<XPathExpressions> xPathExpressions;

    public GmlGfiFilter(PropertyFieldService propertyFieldService, GeoServiceProperties geoServiceProperties) {
        super(propertyFieldService);
        this.geoServiceProperties = geoServiceProperties;
        this.xPathExpressions = ThreadLocal.withInitial(XPathExpressions::new);
    }

    @Override
    public boolean supportsMediaType(MediaType mediaType) {
        if (mediaType == null) {
            return false;
        }
        return MediaTypeExt.APPLICATION_VND_OGC_GML.isCompatibleWith(mediaType)
                || MediaType.TEXT_XML.isCompatibleWith(mediaType)
                || MediaType.APPLICATION_XML.isCompatibleWith(mediaType);
    }

    @Override
    Mono<String> filterBody(LayerNameResolver layerNameResolver, String body, ReferencedColumnValueTransformer referencedColumnValueTransformer) {
        return XmlUtils.parseXmlWithinTimeout(documentBuilderFactory, body, geoServiceProperties)
                .flatMap(document -> this.filterBodyDocument(layerNameResolver, document, referencedColumnValueTransformer))
                .flatMap(
                        document -> ReactiveUtils.runInBackgroundWithTimeout(() -> XmlUtils.serializeXml(document),
                                Duration.ofMillis(200)));
    }

    Mono<Document> filterBodyDocument(LayerNameResolver layerNameResolver, Document bodyDocument, ReferencedColumnValueTransformer referencedColumnValueTransformer) {
        return Flux.fromStream(XmlUtils.xPathToStream(xPathExpressions.get().xpFeatureMembers, bodyDocument))
                .flatMap(featureMemberNode -> XmlUtils.nodeListToFlux(featureMemberNode.getChildNodes())
                        .filter(Element.class::isInstance)
                        .flatMap(featureNode -> {
                                    String nodeName = featureNode.getNodeName();
                                    try {
                                        return Mono.just(new FeatureOfLayer(featureMemberNode, featureNode, QualifiedLayerName.fromString(nodeName)));
                                    } catch (QualifiedLayerName.IllegalLayerNameSyntaxException e) {
                                        log.info("Keeping XML node with illegal layer name: {}", nodeName);
                                        return Mono.empty();
                                    }
                                }
                        )
                )
                .filter(featureOfLayer -> layerNameResolver.contains(featureOfLayer.qualifiedLayerName()))
                .flatMap(featureOfLayer ->
                        XmlUtils.nodeListToFlux(featureOfLayer.featureNode().getChildNodes())
                                .filter(Element.class::isInstance)
                                .flatMap(propertyNode -> {

                                            ReferencedColumnValue rcv = new ReferencedColumnValue(
                                                    featureOfLayer.qualifiedLayerName(),

                                                    // remove workspace-prefix from property name
                                                    propertyNode.getNodeName().substring(featureOfLayer.qualifiedLayerName().workspaceName().length() + 1),
                                                    propertyNode.getTextContent().strip()
                                            );

                                            return referencedColumnValueTransformer.accept(rcv)
                                                    .map(newTextContent -> {
                                                        boolean containsOnlyText = XmlUtils.nodeListToStream(propertyNode.getChildNodes())
                                                                .allMatch(Text.class::isInstance);
                                                        if (containsOnlyText) {
                                                            return new ModifyPropertyTask(featureOfLayer, propertyNode, newTextContent);
                                                        } else {
                                                            return new NoOpPropertyTask();
                                                        }
                                                    })
                                                    .cast(PropertyTask.class)
                                                    .switchIfEmpty(Mono.fromCallable(() -> new RemovePropertyTask(featureOfLayer, propertyNode)));
                                        }
                                )
                )
                .collectList() // finish by collecting the tasks before attempting to modify the document as that would invalidate NodeList iterators
                .map(propertyTasks -> {
                    propertyTasks.forEach(propertyTask -> {
                        if (propertyTask instanceof ModifyPropertyTask mpt) {
                            mpt.propertyNode.setTextContent(mpt.getNewTextContent());
                        } else if (propertyTask instanceof RemovePropertyTask rpt) {
                            rpt.propertyNode.getParentNode().removeChild(rpt.propertyNode);
                        }
                    });
                    return bodyDocument;
                });

    }

    private sealed interface PropertyTask {
    }

    private record FeatureOfLayer(Node featureMemberNode, Node featureNode, QualifiedLayerName qualifiedLayerName) {
    }

    @AllArgsConstructor
    @Getter
    private static final class RemovePropertyTask implements PropertyTask {
        FeatureOfLayer featureOfLayer;
        Node propertyNode;
    }

    @AllArgsConstructor
    @Getter
    private static final class ModifyPropertyTask implements PropertyTask {
        FeatureOfLayer featureOfLayer;
        Node propertyNode;
        String newTextContent;
    }

    private static final class NoOpPropertyTask implements PropertyTask {
    }

    protected static class XPathExpressions {
        final XPath xPath;
        XPathExpression xpFeatureMembers;

        public XPathExpressions() {
            this.xPath = XmlUtils.getSecuredXPathFactory().newXPath();
            precompileXpathExpressions();
        }

        @SneakyThrows
        private void precompileXpathExpressions() {
            this.xpFeatureMembers = xPath.compile("//*[local-name() = 'featureMember']");
        }
    }
}
