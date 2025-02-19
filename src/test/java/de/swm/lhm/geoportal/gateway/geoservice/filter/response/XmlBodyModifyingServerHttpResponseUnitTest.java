package de.swm.lhm.geoportal.gateway.geoservice.filter.response;

import de.swm.lhm.geoportal.gateway.filter.response.BodyModifyingServerHttpResponse;
import de.swm.lhm.geoportal.gateway.geoservice.GeoServiceProperties;
import de.swm.lhm.geoportal.gateway.util.messagebody.MessageBodyEncodingService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.w3c.dom.Document;
import reactor.core.publisher.Mono;

import javax.xml.parsers.DocumentBuilderFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class XmlBodyModifyingServerHttpResponseUnitTest {

    private final XmlBodyModifyingServerHttpResponse responseWrapper = new XmlBodyModifyingServerHttpResponse(
            mock(ServerHttpResponse.class),
            mock(DocumentBuilderFactory.class),
            mock(GeoServiceProperties.class),
            mock(MessageBodyEncodingService.class)) {
        @Override
        protected Mono<Document> processBodyDocument(Document document) {
            return Mono.empty();
        }
    };

    @Test
    void shouldReturnDefaultErrorMessageForXmlRewriteException() {
        BodyModifyingServerHttpResponse.ErrorDetails defaultErrorMessage = responseWrapper.getDefaultErrorDetails();
        assertThat(defaultErrorMessage.message()).contains("Error processing response from upstream server");
        assertThat(defaultErrorMessage.httpStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

}