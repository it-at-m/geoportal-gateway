package de.swm.lhm.geoportal.gateway.filter.webfilter;

import de.swm.lhm.geoportal.gateway.util.messagebody.MessageBodyEncodingService;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.cloud.gateway.filter.factory.rewrite.GzipMessageBodyResolver;
import org.springframework.cloud.gateway.filter.factory.rewrite.MessageBodyDecoder;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.CACHED_REQUEST_BODY_ATTR;

@ExtendWith(OutputCaptureExtension.class)
class BodyCachingFilterTest {

    static final String PAYLOAD = "some payload, and some more payload content";

    private BodyCachingFilter filter;
    private MockServerWebExchange exchange;
    private WebFilterChain filterChain;

    private MessageBodyDecoder messageBodyDecoderMock;

    @BeforeEach
    void setUp() {

        // Initialize and configure the MessageBodyDecoder mock
        messageBodyDecoderMock = Mockito.mock(MessageBodyDecoder.class);
        when(messageBodyDecoderMock.encodingType()).thenReturn("gzip");
        when(messageBodyDecoderMock.decode(any())).thenReturn("decoded body".getBytes(StandardCharsets.UTF_8));

        MessageBodyEncodingService messageBodyEncodingService = new MessageBodyEncodingService(
                List.of(messageBodyDecoderMock),
                List.of()
        );
        filter = new BodyCachingFilter(messageBodyEncodingService);

        exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/"));
        filterChain = Mockito.mock(WebFilterChain.class);
        when(filterChain.filter(any())).thenReturn(Mono.empty());
    }


    @Test
    void testBodyDecompression() {
        GzipMessageBodyResolver gzipMessageBodyResolver = new GzipMessageBodyResolver();

        MessageBodyEncodingService messageBodyEncodingService = new MessageBodyEncodingService(
                List.of(gzipMessageBodyResolver),
                List.of(gzipMessageBodyResolver)
        );

        BodyCachingFilter bodyCachingFilter = new BodyCachingFilter(messageBodyEncodingService);

        DefaultDataBufferFactory dbf = new DefaultDataBufferFactory();
        DataBuffer payload = dbf.wrap(gzipMessageBodyResolver.encode(dbf.wrap(PAYLOAD.getBytes(StandardCharsets.UTF_8))));
        Flux<DataBuffer> hotBodyStream = Flux.fromStream(Stream.of(payload)).doFinally(s ->
            DataBufferUtils.release(payload)
        );

        MockServerWebExchange mockServerWebExchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("http://somehost/somewhere")
                        .header(HttpHeaders.CONTENT_ENCODING, gzipMessageBodyResolver.encodingType())
                        .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(payload.readableByteCount()))
                        .body(hotBodyStream)
        );

        WebFilterChain testFilterChain = filterExchange -> {
            ServerHttpRequest request = filterExchange.getRequest();
            HttpHeaders headers = request.getHeaders();

            assertThat(
                    Optional.ofNullable(headers.get(HttpHeaders.CONTENT_ENCODING))
                            .orElse(Collections.emptyList()),
                    is(Collections.emptyList()));
            assertThat(headers.get(HttpHeaders.CONTENT_LENGTH).getFirst(),
                    is(String.valueOf(PAYLOAD.getBytes(StandardCharsets.UTF_8).length)));

            // fetch body a few times to take advantage of the caching
            for (int i = 0; i < 5; i++) {
                String body = de.swm.lhm.geoportal.gateway.util.DataBufferUtils.copyAsString(request.getBody()).block();
                assertThat(body, is(PAYLOAD));
            }

            return Mono.empty();
        };

        bodyCachingFilter.filter(mockServerWebExchange, testFilterChain).block();
    }

    @Test
    void testWithLimitBodyTooLarge(CapturedOutput capturedOutput) {
        MessageBodyEncodingService messageBodyEncodingService = new MessageBodyEncodingService(
                Collections.emptyList(),
                Collections.emptyList()
        );

        BodyCachingFilter bodyCachingFilter = new BodyCachingFilter(messageBodyEncodingService);
        bodyCachingFilter.setMaxByteCountForBody(500);
        String body = "body".repeat(300);

        MockServerWebExchange mockServerWebExchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("http://somehost/somewhere")
                        .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(body.getBytes(StandardCharsets.UTF_8).length))
                        .body(body)
        );

        AtomicBoolean wasChainCalled = new AtomicBoolean(false);

        bodyCachingFilter.filter(mockServerWebExchange, filterExchange -> {
            wasChainCalled.set(true);
            return Mono.empty();
        }).block();

        assertThat(mockServerWebExchange.getResponse().getStatusCode(), is(HttpStatus.PAYLOAD_TOO_LARGE));
        assertThat(wasChainCalled.get(), is(false));
        assertThat(capturedOutput.getOut(), containsString("exceeded the allowed size"));
    }

    @Test
    void testWithLimitBodyWithinLimit(CapturedOutput capturedOutput) {
        MessageBodyEncodingService messageBodyEncodingService = new MessageBodyEncodingService(
                Collections.emptyList(),
                Collections.emptyList()
        );

        BodyCachingFilter bodyCachingFilter = new BodyCachingFilter(messageBodyEncodingService);
        bodyCachingFilter.setMaxByteCountForBody(500);
        String body = "body";

        MockServerWebExchange mockServerWebExchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("http://somehost/somewhere")
                        .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(body.getBytes(StandardCharsets.UTF_8).length))
                        .body(body)
        );

        AtomicBoolean wasChainCalled = new AtomicBoolean(false);

        bodyCachingFilter.filter(mockServerWebExchange, filterExchange -> {
            wasChainCalled.set(true);
            filterExchange.getResponse().setStatusCode(HttpStatus.OK);
            return Mono.empty();
        }).block();

        assertThat(mockServerWebExchange.getResponse().getStatusCode(), is(HttpStatus.OK));
        assertThat(wasChainCalled.get(), is(true));
        assertThat(capturedOutput.getOut(), not(containsString("exceeded the allowed size")));
    }

    @Test
    void whenSchemeIsNotHttpOrHttps_thenShouldSkipFilter() {
        MockServerHttpRequest request = MockServerHttpRequest.get("ftp://example.com").build();
        exchange = MockServerWebExchange.from(request);

        StepVerifier.create(filter.filter(exchange, filterChain))
                .expectSubscription()
                .verifyComplete();

        Assertions.assertThat((Predicate<String>) exchange.getAttribute(CACHED_REQUEST_BODY_ATTR)).isNull();
    }


    @Test
    void whenDecompressingRequestBody_thenShouldDecodeCorrectly() {
        MockServerHttpRequest gzipRequest = MockServerHttpRequest.post("/")
                .header(HttpHeaders.CONTENT_ENCODING, "gzip")
                .body("compressed body");
        exchange = MockServerWebExchange.from(gzipRequest);

        when(messageBodyDecoderMock.decode("compressed body".getBytes(StandardCharsets.UTF_8))).thenReturn("decompressed body".getBytes(StandardCharsets.UTF_8));

        StepVerifier.create(filter.filter(exchange, filterChain))
                .expectSubscription()
                .verifyComplete();

    }
}
