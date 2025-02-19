package de.swm.lhm.geoportal.gateway.util;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import de.swm.lhm.geoportal.gateway.shared.exceptions.DeserializationException;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import reactor.test.StepVerifier;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.TimeoutException;

import static de.swm.lhm.geoportal.gateway.util.XmlUtils.parseXmlWithinTimeout;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@ExtendWith(OutputCaptureExtension.class)
class XmlUtilsTest {

    @BeforeAll
    public static void setLogLanguage() {
        Locale.setDefault(Locale.forLanguageTag("en"));
    }

    @Test
    void whitespaceIsStripped() {
        assertThat(XmlUtils.stripWhitespace("<tag attr=\" text3 \">  <tag2> text  text2 </tag2> </tag>"), is("<tag attr=\" text3 \"><tag2> text  text2 </tag2></tag>"));
    }

    @Test
    void securedXPathFactoryCanBeConstructed() {
        assertDoesNotThrow(XmlUtils::getSecuredXPathFactory);
    }

    @Test
    void securedTransformerFactoryCanBeConstructed() {
        assertDoesNotThrow(XmlUtils::getSecuredTransformerFactory);
    }

    @Test
    void securedDocumentBuilderFactoryCanBeConstructed() {
        assertDoesNotThrow(XmlUtils::getSecuredDocumentBuilderFactory);
    }

    @Test
    void removeNodes() {
        String xmlString = """
                <?xml version="1.0" encoding="UTF-8"?>
                <root>
                    <mammals>
                        <elephant>a</elephant>
                        <lion>b</lion>
                    </mammals>
                    <birds>
                        <blackbird>b</blackbird>
                        <atlantic-puffin>c</atlantic-puffin>
                        <eagle>d</eagle>
                    </birds>
                    <reptiles>
                        <crocodile>e</crocodile>
                        <mamba>f</mamba>
                    </reptiles>
                </root>
                """;
        DocumentBuilderFactory dbf = XmlUtils.getSecuredDocumentBuilderFactory();
        Document document = XmlUtils.parseXml(dbf, xmlString);
        assertThat(document, notNullValue());

        // remove all nodes containing an "e"
        ArrayList<Node> nodesToRemove = new ArrayList<>();
        collectNodesWithE(nodesToRemove, document.getDocumentElement());
        XmlUtils.removeNodes(nodesToRemove);

        String xmlStringAfter = XmlUtils.serializeXml(document);
        assertThat(xmlStringAfter, containsString("mammals"));
        assertThat(xmlStringAfter, not(containsString("elephant")));
        assertThat(xmlStringAfter, containsString("lion"));
        assertThat(xmlStringAfter, containsString("birds"));
        assertThat(xmlStringAfter, containsString("blackbird"));
        assertThat(xmlStringAfter, containsString("atlantic-puffin"));
        assertThat(xmlStringAfter, not(containsString("eagle")));
        assertThat(xmlStringAfter, not(containsString("reptiles")));
        assertThat(xmlStringAfter, not(containsString("crocodile")));
        assertThat(xmlStringAfter, not(containsString("mamba")));
    }

    @Test
    void testSerializeXml() throws Exception {
        String xmlContent = "<root><child>value</child></root>";
        Document document = createXmlDocument(xmlContent);

        String serializedXml = XmlUtils.serializeXml(document);

        Assertions.assertThat(serializedXml)
                .doesNotContain("\n")
                .doesNotContain("  ")
                .contains("<root><child>value</child></root>");
    }

    @Test
    void parseXmlWithinTimeoutShouldParseSuccessfully() {
        String xmlString = "<root><child>value</child></root>";
        int timeoutMillis = 1000;
        StepVerifier.create(parseXmlWithinTimeout(DocumentBuilderFactory.newInstance(), xmlString, timeoutMillis)).assertNext(document -> Assertions.assertThat(document).isNotNull()).expectComplete().verify();
    }

    @Test
    void parseXmlWithinTimeoutShouldThrowsForInvalidXml(CapturedOutput capturedOutput) {
        String xmlString = "(dada)";
        int timeoutMillis = 1000;
        setLogLevel(Level.INFO);
        StepVerifier.create(parseXmlWithinTimeout(DocumentBuilderFactory.newInstance(), xmlString, timeoutMillis)).expectError(DeserializationException.class).verify();
        Assertions.assertThat(capturedOutput.getErr()).contains("Content is not allowed in prolog.");
        Assertions.assertThat(capturedOutput.getOut()).contains("Parsing XML failed - document will be logged when debug logging is enabled.");
    }

    @Test
    void shouldLogBodyInDebugModeIfParsingFailed(CapturedOutput capturedOutput) {
        String xmlString = "(invalid)";
        int timeoutMillis = 1000;
        setLogLevel(Level.DEBUG);
        StepVerifier.create(parseXmlWithinTimeout(DocumentBuilderFactory.newInstance(), xmlString, timeoutMillis)).expectError(DeserializationException.class).verify();
        Assertions.assertThat(capturedOutput.getOut()).contains("Parsing XML failed. Received XML body was: (invalid)");
        Assertions.assertThat(capturedOutput.getErr()).contains("Content is not allowed in prolog.");
        setLogLevel(Level.INFO);
    }

    @Test
    @Disabled
    void parseXmlWithinTimeoutShouldFailOnTimeout(CapturedOutput capturedOutput) {
        String xmlString = """
                <?xml version="1.0" encoding="UTF-8"?>
                <root>
                    <mammals>
                        <elephant>a</elephant>
                        <lion>b</lion>
                    </mammals>
                    <birds>
                        <blackbird>b</blackbird>
                        <atlantic-puffin>c</atlantic-puffin>
                        <eagle>d</eagle>
                    </birds>
                    <reptiles>
                        <crocodile>e</crocodile>
                        <mamba>f</mamba>
                    </reptiles>
                </root>
                """;
        int timeoutMillis = -1;
        StepVerifier.create(parseXmlWithinTimeout(DocumentBuilderFactory.newInstance(), xmlString, timeoutMillis))
                .expectError(TimeoutException.class)
                .verify();
        Assertions.assertThat(capturedOutput.getOut()).contains("Could not parse XML document within -1 ms. -> discarded");
    }

    private static Document createXmlDocument(String xmlContent) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        return builder.parse(new InputSource(new ByteArrayInputStream(xmlContent.getBytes(StandardCharsets.UTF_8))));
    }

    private static void collectNodesWithE(List<Node> nodeList, Node node) {
        XmlUtils.nodeListToStream(node.getChildNodes()).forEach(child -> {
            if (child.getNodeType() == Node.ELEMENT_NODE && child.getNodeName().contains("e")) {
                nodeList.add(child);
            } else {
                collectNodesWithE(nodeList, child);
            }
        });
    }

    @Test
    void getAttributeValue() {
        String xmlString = """
                <?xml version="1.0" encoding="UTF-8"?>
                <root><tag attr="beep"></tag></root>
                """;
        DocumentBuilderFactory dbf = XmlUtils.getSecuredDocumentBuilderFactory();
        Document document = XmlUtils.parseXml(dbf, xmlString);
        assertThat(document, notNullValue());

        Node tag = document.getDocumentElement().getFirstChild();
        assertThat(tag.getNodeName(), is("tag"));
        assertThat(XmlUtils.getAttributeValue(tag, "attr"), is(Optional.of("beep")));
    }


    @Test
    void getAttributeValueNoAttribute() {
        String xmlString = """
                <?xml version="1.0" encoding="UTF-8"?>
                <root><tag attr="beep"></tag></root>
                """;
        DocumentBuilderFactory dbf = XmlUtils.getSecuredDocumentBuilderFactory();
        Document document = XmlUtils.parseXml(dbf, xmlString);
        assertThat(document, notNullValue());

        Node tag = document.getDocumentElement().getFirstChild();
        assertThat(tag.getNodeName(), is("tag"));
        assertThat(XmlUtils.getAttributeValue(tag, "attr2").isPresent(), is(false));
    }


    private static void setLogLevel(Level level) {
        Logger logger = (Logger) LoggerFactory.getLogger(XmlUtils.class);
        logger.setLevel(level);
    }

}
