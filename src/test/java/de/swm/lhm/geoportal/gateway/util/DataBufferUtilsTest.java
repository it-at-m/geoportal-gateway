package de.swm.lhm.geoportal.gateway.util;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.http.server.reactive.MockServerHttpResponse;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.adapter.DefaultServerWebExchange;
import org.springframework.web.server.i18n.AcceptHeaderLocaleContextResolver;
import org.springframework.web.server.session.DefaultWebSessionManager;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;


@ExtendWith(OutputCaptureExtension.class)
@Slf4j
class DataBufferUtilsTest {

    @Test
    void copyAsStringTest() {
        ServerWebExchange exchange = createExchange();

        StepVerifier.create(DataBufferUtils.copyAsString(exchange.getRequest().getBody()))
                .expectNext(testBodyAsString())
                .verifyComplete();

        String result = DataBufferUtils.copyAsString(exchange.getRequest().getBody()).block();
        assertThat(result, is(testBodyAsString()));

        String result2 = DataBufferUtils.copyAsString(exchange.getRequest().getBody()).block();
        assertThat(result, is(result2));
    }

    @Test
    void copyAsByteArrayTest() {

        ServerWebExchange exchange = createExchange();

        StepVerifier.create(DataBufferUtils.copyAsByteArray(exchange.getRequest().getBody()).map(String::new))
                .expectNext(testBodyAsString())
                .verifyComplete();

        byte[] result = DataBufferUtils.copyAsByteArray(exchange.getRequest().getBody()).block();
        assertThat(result, is(testBodyAsBytes()));

        byte[] result2 = DataBufferUtils.copyAsByteArray(exchange.getRequest().getBody()).block();
        assertThat(result, is(result2));
    }

    @Test
    void copyAsByteArrayShouldThrowIfMaxByteCountIsNegative() {
        Assertions.assertThrows(RuntimeException.class, () -> DataBufferUtils.copyAsByteArray(Flux.empty(), -1), "maxByteCount must be positive");
    }

    private String testBodyAsString() {
        return """
                {
                    "test": true
                }
                """;
    }

    private byte[] testBodyAsBytes() {
        return testBodyAsString().getBytes(StandardCharsets.UTF_8);
    }

    private DefaultServerWebExchange createExchange() {

        MockServerHttpRequest request = MockServerHttpRequest
                .post("https://example.com")
                .body(testBodyAsString());

        return new DefaultServerWebExchange(
                request,
                new MockServerHttpResponse(),
                new DefaultWebSessionManager(),
                ServerCodecConfigurer.create(),
                new AcceptHeaderLocaleContextResolver()
        );
    }

    @Test
    void copyAsStringWithMaxByteCountTest() {
        DefaultDataBufferFactory dataBufferFactory = new DefaultDataBufferFactory();

        Flux<DataBuffer> dataBufferFlux = Flux.fromStream(
                Stream.of("12", "34", "56", "78", "910")
                        .map(str -> dataBufferFactory.wrap(str.getBytes(StandardCharsets.UTF_8))));

        DataBufferUtils.DataBufferContents<String> read = DataBufferUtils.copyAsString(dataBufferFlux, 7).block();

        assertThat(read, instanceOf(DataBufferUtils.DataBufferContents.Partial.class));
        assertThat(read.get(), is("1234567"));
    }


    @Test
    void releaseDataBufferShouldReturnFalse() {
        DefaultDataBufferFactory dataBufferFactory = new DefaultDataBufferFactory();
        DataBuffer dataBuffer = dataBufferFactory.wrap("beep".getBytes(StandardCharsets.UTF_8));
        DataBufferUtils.withDataBufferRelease(dataBuffer, dataBuffer1 -> Mono.just("dfd"), log, () -> this.getClass().getName())
                .block();
        assertThat(DataBufferUtils.releaseDataBuffer(dataBuffer, log, () -> this.getClass().getName()), is(false));
    }
}