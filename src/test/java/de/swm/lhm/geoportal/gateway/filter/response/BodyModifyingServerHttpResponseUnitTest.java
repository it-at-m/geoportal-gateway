package de.swm.lhm.geoportal.gateway.filter.response;

import de.swm.lhm.geoportal.gateway.util.messagebody.MessageBodyEncodingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.mock.http.server.reactive.MockServerHttpResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;


class BodyModifyingServerHttpResponseUnitTest {

    private TestBodyModifyingServerHttpResponse bodyModifyingResponse;
    private MockServerHttpResponse mockResponse;
    private MessageBodyEncodingService messageBodyEncodingService;

    private DefaultDataBufferFactory bufferFactory;

    @BeforeEach
    void setUp() {
        mockResponse = new MockServerHttpResponse();
        messageBodyEncodingService = new MessageBodyEncodingService(Collections.emptyList(), Collections.emptyList());
        bodyModifyingResponse = new TestBodyModifyingServerHttpResponse(mockResponse, messageBodyEncodingService);
        bufferFactory = new DefaultDataBufferFactory();
    }

    @Test
    void writeWithSuccess() {
        String originalBody = "Original Body";
        Flux<DataBuffer> bodyFlux = Flux.just(new DefaultDataBufferFactory().wrap(originalBody.getBytes()));

        StepVerifier.create(bodyModifyingResponse.writeWith(bodyFlux))
                .expectComplete()
                .verify();

        assertThat(mockResponse.getBodyAsString().block()).isEqualTo(originalBody);
        //assertThat(mockResponse.getHeaders().getContentType()).isEqualTo(MediaType.TEXT_PLAIN);
        //assertThat(mockResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void writeWithError() {
        Flux<DataBuffer> bodyFlux = Flux.error(new RuntimeException("Simulated error"));

        StepVerifier.create(bodyModifyingResponse.writeWith(bodyFlux))
                .verifyComplete();

        String expectedErrorMessage = "Error processing response from upstream server";
        assertThat(mockResponse.getBodyAsString().block()).contains(expectedErrorMessage);
        assertThat(mockResponse.getHeaders().getContentType()).isEqualTo(MediaType.TEXT_PLAIN);
        assertThat(mockResponse.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    void whenWriteWithSuccess_thenSuccessfullyWritesData() {
        String data = "Hello, World!";
        DataBuffer dataBuffer = bufferFactory.wrap(data.getBytes());
        Flux<DataBuffer> body = Flux.just(dataBuffer);

        StepVerifier.create(bodyModifyingResponse.writeWith(body))
                .expectSubscription()
                .expectComplete()
                .verify();

        assertThat(mockResponse.getBodyAsString().block()).isEqualTo(data);
    }

    // Test case with an error during body processing
    @Test
    void whenWriteWithError_whileProcessingBody_thenWritesDefaultError() {
        Flux<DataBuffer> body = Flux.error(new RuntimeException("Unknown error"));

        StepVerifier.create(bodyModifyingResponse.writeWith(body))
                .verifyComplete();

        String defaultErrorMessage = "Error processing response from upstream server";
        assertThat(mockResponse.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(mockResponse.getBodyAsString().block()).contains(defaultErrorMessage);
        assertThat(mockResponse.getHeaders().getContentType()).isEqualTo(bodyModifyingResponse.getErrorContentType());
    }

    // Test overriding getErrorMessageAndStatus() to return custom error message and status
    @Test
    void whenOverridingGetErrorMessageAndStatus_thenWritesCustomError() {
        // Creating a subclass which overrides the default error handling behavior
        TestBodyModifyingServerHttpResponse customErrorResponse = new TestBodyModifyingServerHttpResponse(mockResponse, messageBodyEncodingService) {
            @Override
            protected Optional<ErrorDetails> getErrorDetails(Throwable e) {
                return Optional.of(new ErrorDetails(HttpStatus.BAD_GATEWAY, "Custom error"));
            }
        };

        Flux<DataBuffer> body = Flux.error(new RuntimeException("Unknown error"));

        StepVerifier.create(customErrorResponse.writeWith(body))
                .verifyComplete();

        assertThat(mockResponse.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
        assertThat(mockResponse.getBodyAsString().block()).contains("Custom error");
        assertThat(mockResponse.getHeaders().getContentType()).isEqualTo(customErrorResponse.getErrorContentType());
    }


}

class TestBodyModifyingServerHttpResponse extends BodyModifyingServerHttpResponse {

    public TestBodyModifyingServerHttpResponse(ServerHttpResponse delegate, MessageBodyEncodingService messageBodyEncodingService) {
        super(delegate, messageBodyEncodingService);
    }

    @Override
    protected Mono<DataBuffer> processBody(Mono<DataBuffer> body) {
        // Return body as is, or provide custom processing logic
        return body;
    }
}