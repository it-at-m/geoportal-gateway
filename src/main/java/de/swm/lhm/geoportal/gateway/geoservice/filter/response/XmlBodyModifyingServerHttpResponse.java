package de.swm.lhm.geoportal.gateway.geoservice.filter.response;

import de.swm.lhm.geoportal.gateway.filter.response.BodyModifyingServerHttpResponse;
import de.swm.lhm.geoportal.gateway.geoservice.GeoServiceProperties;
import de.swm.lhm.geoportal.gateway.shared.exceptions.DeserializationException;
import de.swm.lhm.geoportal.gateway.shared.exceptions.SerializationException;
import de.swm.lhm.geoportal.gateway.util.DataBufferUtils;
import de.swm.lhm.geoportal.gateway.util.HttpHeaderUtils;
import de.swm.lhm.geoportal.gateway.util.XmlUtils;
import de.swm.lhm.geoportal.gateway.util.messagebody.MessageBodyEncodingService;
import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Publisher;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.w3c.dom.Document;
import reactor.core.publisher.Mono;

import javax.xml.parsers.DocumentBuilderFactory;
import java.util.Optional;

import static java.nio.charset.StandardCharsets.UTF_8;

@Slf4j
public abstract class XmlBodyModifyingServerHttpResponse extends BodyModifyingServerHttpResponse {
    final DocumentBuilderFactory documentBuilderFactory;
    final GeoServiceProperties geoServiceProperties;

    public XmlBodyModifyingServerHttpResponse(ServerHttpResponse delegate, DocumentBuilderFactory documentBuilderFactory,
                                              GeoServiceProperties geoServiceProperties, MessageBodyEncodingService messageBodyEncodingService) {
        super(delegate, messageBodyEncodingService);
        this.documentBuilderFactory = documentBuilderFactory;
        this.geoServiceProperties = geoServiceProperties;
    }

    @Override
    protected Mono<DataBuffer> processBody(Mono<DataBuffer> body) {
        return DataBufferUtils.copyAsString(body)
                .flatMap(xmlString -> XmlUtils.parseXmlWithinTimeout(documentBuilderFactory, xmlString, geoServiceProperties))
                .flatMap(this::processBodyDocument)
                .map(XmlUtils::serializeXml)
                .map(xmlString -> getDelegate().bufferFactory().wrap(xmlString.getBytes(UTF_8)));

    }

    protected abstract Mono<Document> processBodyDocument(Document document);

    @Override
    protected Mono<Void> writeErrorDetails(ErrorDetails errorDetails) {
        return super.writeErrorDetails(errorDetails
                .mapMessage(message -> String.format("<error>%s</error>", message)));
    }

    @Override
    protected Optional<ErrorDetails> getErrorDetails(Throwable e) {
        return switch (e) {
            case XmlRewriteException xre ->
                    Optional.of(new ErrorDetails(HttpStatus.INTERNAL_SERVER_ERROR, xre.getMessage()));
            case SerializationException se ->
                    Optional.of(new ErrorDetails(HttpStatus.INTERNAL_SERVER_ERROR, se.getMessage()));
            case DeserializationException de ->
                    Optional.of(new ErrorDetails(HttpStatus.INTERNAL_SERVER_ERROR, de.getMessage()));
            default -> super.getErrorDetails(e);
        };
    }

    @Override
    protected MediaType getErrorContentType() {
        return MediaType.TEXT_XML;
    }

    @Override
    public Mono<Boolean> supportsBodyRewrite() {
        return Mono.just(HttpHeaderUtils.isContentTypeXmlOrGml(getHeaders()));
    }

    @Override
    public Mono<Void> writeWith(Publisher<? extends DataBuffer> body) {
        HttpHeaders headers = getDelegate().getHeaders();
        if (!HttpHeaderUtils.isContentTypeXmlOrGml(headers)) {
            String message = "Received non-xml content-type from upstream service (http status: %s)"
                    .formatted(getDelegate().getStatusCode());

            Mono<String> bodyStringMono = Mono.empty();
            if (HttpHeaderUtils.isTextFormatContentType(headers)) {
                bodyStringMono = decompressBody(org.springframework.core.io.buffer.DataBufferUtils.join(body))
                        .map(DataBufferUtils::readDataBufferAsString);
            }

            return bodyStringMono
                    .map(bodyString -> {
                        if (log.isDebugEnabled()) {
                            log.warn("{} [disable debug logging to hide body]: \n{}", message, bodyString);
                        } else {
                            log.warn("{}: [enable debug logging for received body]", message);
                        }
                        return true;
                    })
                    .switchIfEmpty(Mono.fromCallable(() -> {
                        log.warn("{}: [non-textual body]", message);
                        return true;
                    }))
                    .then(this.writeErrorDetails(new ErrorDetails(HttpStatus.INTERNAL_SERVER_ERROR, message)));

        }
        return super.writeWith(body);
    }

    protected static class XmlRewriteException extends RuntimeException {
        public XmlRewriteException(String userMessage, Throwable e) {
            super(userMessage, e);
        }

        public XmlRewriteException(String userMessage) {
            super(userMessage);
        }
    }
}
