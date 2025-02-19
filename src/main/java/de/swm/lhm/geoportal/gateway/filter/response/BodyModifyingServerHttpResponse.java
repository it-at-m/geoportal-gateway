package de.swm.lhm.geoportal.gateway.filter.response;

import de.swm.lhm.geoportal.gateway.util.DataBufferUtils;
import de.swm.lhm.geoportal.gateway.util.messagebody.MessageBodyEncodingService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.reactivestreams.Publisher;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.Optional;
import java.util.function.UnaryOperator;

import static java.nio.charset.StandardCharsets.UTF_8;

@Slf4j
public abstract class BodyModifyingServerHttpResponse extends ServerHttpResponseDecorator {
    private final MessageBodyEncodingService messageBodyEncodingService;

    public BodyModifyingServerHttpResponse(ServerHttpResponse delegate, MessageBodyEncodingService messageBodyEncodingService) {
        super(delegate);
        this.messageBodyEncodingService = messageBodyEncodingService;
    }

    /** process the uncompressed body of the response */
    protected abstract Mono<DataBuffer> processBody(Mono<DataBuffer> body);

    public ErrorDetails getDefaultErrorDetails() {
        return new ErrorDetails(HttpStatus.INTERNAL_SERVER_ERROR, "Error processing response from upstream server");
    }

    /**
     * intended to be overridden in subclasses
     */
    protected Optional<ErrorDetails> getErrorDetails(Throwable e) {
        return Optional.of(getDefaultErrorDetails());
    }

    protected MediaType getErrorContentType() {
        return MediaType.TEXT_PLAIN;
    }

    protected Mono<DataBuffer> decompressBody(Mono<DataBuffer> body) {
        return messageBodyEncodingService.getContentEncoding(getHeaders())
                .map(contentEncodingName -> body
                        .flatMap(dataBuffer ->
                                DataBufferUtils.withDataBufferRelease(
                                                messageBodyEncodingService.decodeDataBuffer(dataBuffer, contentEncodingName),
                                                Mono::just,
                                                log,
                                            () -> getClass().getName() + " release decompressed body data buffer"
                                        )
                                        // release the compressed data buffer
                                        .doFinally(s -> org.springframework.core.io.buffer.DataBufferUtils.release(dataBuffer))))
                .orElseGet(() -> body);
    }

    protected Mono<DataBuffer> compressBody(Mono<DataBuffer> body) {
        return messageBodyEncodingService.getContentEncoding(getHeaders())
                .map(contentEncodingName -> body
                        .flatMap(dataBuffer ->
                                DataBufferUtils.withDataBufferRelease(
                                        dataBuffer,
                                        decompressedDataBuffer -> Mono.fromCallable(() ->
                                                messageBodyEncodingService.encodeDataBuffer(decompressedDataBuffer, contentEncodingName)),
                                        log,
                                        () -> getClass().getName() + " release uncompressed body data buffer after compression"
                                        )))
                .orElseGet(() -> body);

    }

    private Mono<DataBuffer> processPotentiallyCompressedBody(Mono<DataBuffer> body) {
        return compressBody(processBody(decompressBody(body)));
    }

    public Mono<Boolean> supportsBodyRewrite() {
        return Mono.just(true);
    }

    private boolean isWebSocketUpgrade() {
        // http 2
        HttpStatusCode statusCode = getDelegate().getStatusCode();
        if (statusCode != null && statusCode.isSameCodeAs(HttpStatus.SWITCHING_PROTOCOLS)) {
            return true;
        }
        // http 1.1
        return Optional.ofNullable(getHeaders().get(HttpHeaders.UPGRADE))
                .orElse(Collections.emptyList())
                .stream()
                .anyMatch(v -> StringUtils.equalsIgnoreCase(v, "websocket"));
    }

    @Override
    public Mono<Void> writeWith(Publisher<? extends DataBuffer> body) {
        if (isWebSocketUpgrade()) {
            log.warn("WebSocket upgrade detected, skipping body modification");
            // reason: websocket connections can not be completely read into a data buffer
            return this.getDelegate().writeWith(body);
        }
        return supportsBodyRewrite()
                .flatMap(supportsBodyRewrite -> {
                    if (Boolean.TRUE.equals(supportsBodyRewrite)) {

                        Mono<DataBuffer> dataBufferMono = org.springframework.core.io.buffer.DataBufferUtils.join(body);

                        return processPotentiallyCompressedBody(dataBufferMono)
                                .flatMap(this::writeWithContentLength)
                                .onErrorResume(e -> {
                                            // Drain the input flux and release the databuffers. Mostly required for cases where
                                            // an error occured.
                                            // Not sure why this necessary, but this seems to resolve most of the errors
                                            // where nettys leak-detector found a non-released ByteBuf-object before garbage collection.
                                            return dataBufferMono
                                                    // release the input data buffer when there was one
                                                    .map(dataBuffer -> DataBufferUtils.releaseDataBuffer(
                                                            dataBuffer,
                                                            "drain response",
                                                            log,
                                                                    () -> getClass().getName() + " release databuffer on error"))
                                                    // regardless if there was an input databuffer or even an error, return the error
                                                    .onErrorResume(e3 -> Mono.just(false))
                                                    .then(writeError(e));
                                        }
                                );
                    } else {
                        return this.getDelegate().writeWith(body);
                    }
                });
    }

    protected Mono<Void> writeError(Throwable e) {
        log.error("Error during processing of response body", e);
        return writeErrorDetails(getErrorDetails(e).orElseGet(this::getDefaultErrorDetails));
    }

    protected Mono<Void> writeErrorDetails(ErrorDetails errorDetails) {
        getDelegate().setStatusCode(errorDetails.httpStatus());
        HttpHeaders headers = getDelegate().getHeaders();
        if (log.isDebugEnabled()) {
            log.debug("Error encountered. The original response headers were: {}", headers);
        }
        headers.clearContentHeaders();
        headers.set(HttpHeaders.CONTENT_TYPE, getErrorContentType().toString());
        return writeWithContentLength(getDelegate().bufferFactory().wrap(errorDetails.message().getBytes(UTF_8)));
    }

    private Mono<Void> writeWithContentLength(DataBuffer dataBuffer) {
        // IMPORTANT: the databuffer must not be release here as it may be accessed from multiple filters
        getDelegate().getHeaders().set(HttpHeaders.CONTENT_LENGTH, String.valueOf(dataBuffer.readableByteCount()));
        return getDelegate().writeWith(Mono.just(dataBuffer));
    }

    /**
     * mutate the serverwebexchange to wrap the response using this instance
     * <p>
     * This method should be preferred over mutating the exchange directly as it also sets content-encoding related headers.
     *
     * @return ServerWebExchange
     */
    public ServerWebExchange mutateServerWebExchange(ServerWebExchange exchange) {
        return exchange.mutate()
                // prevent the backend service from applying unsupported compression algorithms to the
                // response body by removing them from the accept-encoding header
                .request(requestBuilder ->
                        requestBuilder.headers(messageBodyEncodingService::removeUnsupportedAcceptEncodings)
                )
                .response(this)
                .build();
    }

    public record ErrorDetails(HttpStatus httpStatus, String message) {
        public ErrorDetails mapMessage(UnaryOperator<String> mapper) {
            return new ErrorDetails(this.httpStatus(), mapper.apply(this.message));
        }
    }
}
