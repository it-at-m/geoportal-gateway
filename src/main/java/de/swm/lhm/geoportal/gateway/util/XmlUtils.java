package de.swm.lhm.geoportal.gateway.util;

import de.swm.lhm.geoportal.gateway.geoservice.GeoServiceProperties;
import de.swm.lhm.geoportal.gateway.shared.exceptions.DeserializationException;
import de.swm.lhm.geoportal.gateway.shared.exceptions.SerializationException;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import javax.xml.xpath.XPathFactoryConfigurationException;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.TimeoutException;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static javax.xml.XMLConstants.ACCESS_EXTERNAL_DTD;
import static javax.xml.XMLConstants.ACCESS_EXTERNAL_STYLESHEET;
import static javax.xml.XMLConstants.FEATURE_SECURE_PROCESSING;

@Slf4j
@UtilityClass
public class XmlUtils {

    /**
     * get a preconfigured documentbuilderfactory suitable to parse untrusted input
     */
    @SneakyThrows
    public static DocumentBuilderFactory getSecuredDocumentBuilderFactory() {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        try {
            // Disable external entities: XML external entities should be disabled to prevent XXE attacks.
            dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            dbf.setFeature("http://xml.org/sax/features/external-general-entities", false);
            dbf.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            dbf.setXIncludeAware(false);
            dbf.setExpandEntityReferences(false);

            // Disable DTDs (Document Type Definitions): Disabling DTDs prevents an attacker from using
            // DTDs to launch DoS attacks or to detect files on the server.
            dbf.setFeature("http://apache.org/xml/features/nonvalidating/load-dtd-grammar", false);
            dbf.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);

            // Implement secure processing: Enabling secure processing can limit the Java XML processing
            // APIs' functionality, reducing the risk of an attacker exploiting the parser by limiting the
            // APIs available to the application.
            dbf.setFeature(FEATURE_SECURE_PROCESSING, true);
            dbf.setFeature("http://javax.xml.XMLConstants/feature/secure-processing", true);

        } catch (ParserConfigurationException e) {
            log.error("Could not configure XML documentfactory", e);
            throw e;
        }
        return dbf;
    }

    @SneakyThrows
    public static XPathFactory getSecuredXPathFactory() {
        XPathFactory xpf = XPathFactory.newInstance();
        try {
            xpf.setFeature(FEATURE_SECURE_PROCESSING, true);
        } catch (XPathFactoryConfigurationException e) {
            log.error("Could not configure XML XPathFactory", e);
            throw e;
        }
        return xpf;
    }

    @SneakyThrows
    public static TransformerFactory getSecuredTransformerFactory() {
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        try {
            transformerFactory.setFeature(FEATURE_SECURE_PROCESSING, true);
        } catch (TransformerConfigurationException e) {
            log.error("Could not configure XML TransformerFactory", e);
            throw e;
        }

        // Disable external DTDs and external entity processing to prevent XXE
        transformerFactory.setAttribute(ACCESS_EXTERNAL_DTD, "");
        transformerFactory.setAttribute(ACCESS_EXTERNAL_STYLESHEET, "");

        return transformerFactory;
    }

    public static String serializeXml(Document document) {
        TransformerFactory transformerFactory = getSecuredTransformerFactory();
        try (StringWriter stringWriter = new StringWriter()) {
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "no");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "0");

            transformer.transform(new DOMSource(document), new StreamResult(stringWriter));
            return stripWhitespace(stringWriter.toString());
        } catch (TransformerException | IOException e) {
            throw new SerializationException("Serializing XML failed", e);
        }
    }

    public static String stripWhitespace(String xmlString) {
        return xmlString.replaceAll(">\\s+<", "><");
    }

    public static Document parseXml(DocumentBuilderFactory documentBuilderFactory, String xmlString) {
        try (InputStream is = IOUtils.toInputStream(xmlString, StandardCharsets.UTF_8)) {
            DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
            return documentBuilder.parse(is);
        } catch (ParserConfigurationException | SAXException | IOException e) {
            if (log.isDebugEnabled()) {
                log.atWarn()
                        .setMessage(() -> String.format("Parsing XML failed. Received XML body was: %s", xmlString))
                        .setCause(e)
                        .log();
            } else {
                log.warn("Parsing XML failed - document will be logged when debug logging is enabled.", e);
            }
            throw new DeserializationException("Parsing input XML failed", e);
        }
    }

    public static Mono<Document> parseXmlWithinTimeout(DocumentBuilderFactory documentBuilderFactory, String xmlString, int timeoutMillis) {
        return ReactiveUtils.runInBackgroundWithTimeout(
                () -> parseXml(documentBuilderFactory, xmlString),
                Duration.ofMillis(timeoutMillis)
        ).onErrorMap(TimeoutException.class, e -> {
            log.atWarn()
                    .setMessage(() -> String.format(
                            "Could not parse XML document within %d ms. -> discarded",
                            timeoutMillis))
                    .log();
            return e;
        });
    }

    public static Mono<Document> parseXmlWithinTimeout(DocumentBuilderFactory documentBuilderFactory, String xmlString, GeoServiceProperties geoServiceProperties) {
        return parseXmlWithinTimeout(documentBuilderFactory, xmlString, geoServiceProperties.getSanitizedMaxXmlParsingDurationMs());
    }

    public static Stream<Node> xPathToStream(XPathExpression xPathExpression, Document document) {
        try {
            return nodeListToStream((NodeList) xPathExpression.evaluate(document, XPathConstants.NODESET));
        } catch (XPathExpressionException e) {
            log.error("Failed to evaluate xpath expression", e);
            throw new RuntimeException("Failed to evaluate xpath expression", e);
        }
    }

    public static Stream<Node> nodeListToStream(NodeList nodeList) {
        if (nodeList == null) {
            return Stream.empty();
        }
        return IntStream.range(0, nodeList.getLength())
                .mapToObj(nodeList::item);
    }

    public static Flux<Node> nodeListToFlux(NodeList nodeList) {
        return Flux.fromStream(nodeListToStream(nodeList));
    }

    public static void removeNodes(Collection<Node> nodesToRemove) {
        nodesToRemove.forEach(XmlUtils::removeNode);
    }

    public static void removeNode(Node node) {
        if (node == null) {
            return;
        }
        Node parent = node.getParentNode();
        if (parent != null) {
            try {
                parent.removeChild(node);
            } catch (DOMException e) {
                // ignore errors when the node has been removed before
                if (e.code == DOMException.NOT_FOUND_ERR) {
                    log.atDebug()
                            .setMessage(() -> String.format(
                                    "Node %s was marked for removal, but could not be removed",
                                    node.getNodeName()))
                            .log();
                } else {
                    throw e;
                }
            }
        }
    }

    public static Optional<String> getAttributeValue(Node node, String attributeName) {
        return Optional.ofNullable(node.getAttributes())
                .flatMap(attrs -> Optional.ofNullable(attrs.getNamedItem(attributeName)))
                .flatMap(attr -> Optional.ofNullable(attr.getTextContent()));
    }
}
