package de.swm.lhm.geoportal.gateway.filter.webfilter;

import de.swm.lhm.geoportal.gateway.util.DataBufferUtils;
import de.swm.lhm.geoportal.gateway.util.messagebody.MessageBodyEncodingService;
import io.netty.buffer.Unpooled;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DefaultDataBuffer;
import org.springframework.core.io.buffer.NettyDataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.CACHED_REQUEST_BODY_ATTR;

/**
 * Modelled after Springs CacheRequestBodyGatewayFilterFactory in combination with ServerWebExchangeUtils
 * <p>
 * <p>
 * This class decodes incomming messages from gzip encoding. They will not get re-encoded before passing them on.
 */
@Setter
@Slf4j
@RequiredArgsConstructor
public class BodyCachingFilter implements WebFilter {

    private final MessageBodyEncodingService messageBodyEncodingService;

    /**
     * maximum number of bytes to accept in the body of a request. value 0 means no limit is enforced
     */
    private int maxByteCountForBody = 0;

    private boolean isBodyCachingDisabled(ServerWebExchange exchange) {
        ServerHttpRequest request = exchange.getRequest();
        URI requestUri = request.getURI();
        String scheme = requestUri.getScheme();

        // Record only http requests (including https)
        // this means Mock-http requests will also not be cached and this filter will be
        // skipped (as scheme == null).
        if ((!"http".equals(scheme) && !"https".equals(scheme))) {
            return true;
        }

        // WebFlux formdata extraction does not work with bodycaching as the mono extracting the formdata is instantiated
        // upon construction of the exchange object. The databuffer referenced within this mono is drained by this filter
        // before exchange.getFormData can be called.
        return Optional.ofNullable(exchange.getRequest().getHeaders().get(HttpHeaders.CONTENT_TYPE))
                .orElse(Collections.emptyList())
                .stream()
                .anyMatch(contentTypeString -> StringUtils.startsWith(contentTypeString, MediaType.APPLICATION_FORM_URLENCODED_VALUE));
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        if (isBodyCachingDisabled(exchange)) {
            return chain.filter(exchange);
        }

        Mono<Void> filterMono = switch (exchange.getAttribute(CACHED_REQUEST_BODY_ATTR)) {
            case null -> this.cacheRequestBody(exchange, serverHttpRequest ->
                    chain.filter(exchange.mutate().request(serverHttpRequest).build()));
            // Request body is already cached, there is a high chance this filter was accidentally
            // added multiple times. Regardless of that we are reusing the cached attribute
            case DataBuffer dataBuffer -> chain.filter(exchange.mutate().request(decorate(exchange, dataBuffer)).build());
            default -> Mono.error(new IllegalStateException("unexpected type of cached request body"));
        };

        Runnable releaseCachedRequestBody = () -> {
            // attempt to release manually when a cache exists to avoid leaking buffers
            if (exchange.getAttribute(CACHED_REQUEST_BODY_ATTR) instanceof DataBuffer cachedDataBuffer) {
                DataBufferUtils.releaseDataBuffer(
                        cachedDataBuffer,
                        "release cached request body",
                        log,
                        () -> getClass().getName() + " releaseCachedRequestBody"
                );
            }
        };

        return filterMono
                .doFinally(s -> releaseCachedRequestBody.run());
    }

    // adapted from ServerWebExchangeUtils
    <T> Mono<T> cacheRequestBody(ServerWebExchange exchange, Function<ServerHttpRequest, Mono<T>> function) {
        ServerHttpResponse response = exchange.getResponse();
        ServerHttpRequest request = exchange.getRequest();
        DataBufferFactory factory = response.bufferFactory();

        Flux<DataBuffer> incommingBodyFlux = request.getBody();

        if (maxByteCountForBody <= 0) {
            return decorateRaw(
                    exchange,
                    DataBufferUtils.copyAsByteArray(incommingBodyFlux)
                            .map(bytes -> factory.wrap(
                                    messageBodyEncodingService.decodeIncomingMessage(exchange.getRequest(), bytes))
                            ),
                    function
            );
        } else {
            return DataBufferUtils.copyAsByteArray(incommingBodyFlux, maxByteCountForBody)
                    .flatMap(result -> switch (result) {
                                case DataBufferUtils.DataBufferContents.Complete<byte[]> complete -> {
                                    byte[] decodedBytes = messageBodyEncodingService.decodeIncomingMessage(exchange.getRequest(), complete.get());
                                    yield decorateRaw(exchange, Mono.just(factory.wrap(decodedBytes)), function);
                                }
                                case DataBufferUtils.DataBufferContents.Partial<byte[]> partial -> {
                                    // allowed body size exceeded, request is not passed to the provided
                                    // `function` and instead is answered directly.

                                    log.atWarn()
                                            .setMessage(() -> String.format(
                                                "Received body in %s request to %s exceeded the allowed size of %d bytes. Size was %d",
                                                request.getMethod(),
                                                request.getPath(),
                                                maxByteCountForBody,
                                                partial.getNumBytesContained()))
                                            .log();

                                    response.setStatusCode(HttpStatus.PAYLOAD_TOO_LARGE);
                                    response.getHeaders().add(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_PLAIN_VALUE);
                                    yield DataBufferUtils.withDataBufferRelease(
                                                    factory.wrap(HttpStatus.PAYLOAD_TOO_LARGE.getReasonPhrase().getBytes(UTF_8)),
                                                    dataBuffer -> response.writeWith(Mono.just(dataBuffer)),
                                                    log,
                                                    () -> getClass().getName() + " TooLargeResponse body"
                                            )
                                            .then(Mono.empty());
                                }
                            }
                    );
        }
    }

    private <T> Mono<T> decorateRaw(ServerWebExchange exchange, Mono<DataBuffer> dataBufferMono, Function<ServerHttpRequest, Mono<T>> function) {
        return dataBufferMono
                .map(byteArray -> decorate(exchange, byteArray))
                .switchIfEmpty(Mono.just(exchange.getRequest()))
                .flatMap(function);
    }

    private ServerHttpRequest decorate(ServerWebExchange exchange, DataBuffer dataBuffer) {
        if (dataBuffer.readableByteCount() > 0) {
            if (log.isTraceEnabled()) {
                log.atTrace().setMessage(() -> String.format("retaining body with %d bytes in exchange attribute", dataBuffer.readableByteCount())).log();
            }

            Object cachedDataBuffer = exchange.getAttribute(CACHED_REQUEST_BODY_ATTR);
            // don't cache if body is already cached
            if (!(cachedDataBuffer instanceof DataBuffer)) {
                exchange.getAttributes().put(CACHED_REQUEST_BODY_ATTR, dataBuffer);
            }
        }

        return new ServerHttpRequestDecorator(exchange.getRequest()) {

            @Override
            public HttpHeaders getHeaders() {
                HttpHeaders headers = HttpHeaders.writableHttpHeaders(super.getHeaders());

                // remove content-encoding as the message has already been decoded
                Set<String> supportedContentEncodingsForDecoding = messageBodyEncodingService.getSupportedRequestContentEncodings();
                List<String> untouchedEncodings = messageBodyEncodingService.getContentEncoding(exchange.getRequest().getHeaders())
                        .stream()
                        .filter(encoding -> !supportedContentEncodingsForDecoding.contains(encoding))
                        .toList();
                headers.replace(HttpHeaders.CONTENT_ENCODING, untouchedEncodings);

                // set the correct content length of the decoded message body.
                // This is important when the request is proxied/loadbalanced using the api gateway
                // to ensure the whole body is sent.
                headers.set(HttpHeaders.CONTENT_LENGTH, String.valueOf(dataBuffer.readableByteCount()));

                return HttpHeaders.readOnlyHttpHeaders(headers);
            }

            @Override
            public Flux<DataBuffer> getBody() {
                return Mono.fromSupplier(() -> {
                            if (exchange.getAttribute(CACHED_REQUEST_BODY_ATTR) == null) {
                                // probably == downstream closed or no body
                                return null;
                            }
                            return switch (dataBuffer) {
                                case NettyDataBuffer pdb -> pdb.factory().wrap(pdb.getNativeBuffer().retainedSlice());
                                case DefaultDataBuffer ddf -> ddf.factory().wrap(Unpooled.wrappedBuffer(ddf.getNativeBuffer()).nioBuffer());
                                default -> throw new IllegalArgumentException(
                                        "Unable to handle DataBuffer of type " + dataBuffer.getClass());
                            };
                        })
                        .flux();
            }
        };
    }
}
