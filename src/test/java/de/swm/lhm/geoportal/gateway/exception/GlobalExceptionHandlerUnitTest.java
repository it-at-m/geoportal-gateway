package de.swm.lhm.geoportal.gateway.exception;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import static com.toomuchcoding.jsonassert.JsonAssertion.assertThat;
import static de.swm.lhm.geoportal.gateway.exception.GlobalExceptionHandler.getCapturingExceptions;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.util.ReflectionTestUtils.setField;

@ExtendWith(MockitoExtension.class)
@ExtendWith({OutputCaptureExtension.class})
@MockitoSettings(strictness = Strictness.LENIENT)
class GlobalExceptionHandlerUnitTest {

    private GlobalExceptionHandler handler;

    @Captor
    ArgumentCaptor<byte[]> dataBufferCaptor;

    @Mock
    ServerWebExchange serverWebExchange;

    @Mock
    ServerHttpResponse response;

    @Mock
    HttpHeaders headers;

    @Mock
    DataBufferFactory dataBufferFactory;

    @Mock
    ServerHttpRequest request;

    @BeforeEach
    public void setup() {
        handler = new GlobalExceptionHandler();

        // Mock ServerHttpRequest und HttpHeaders
        when(serverWebExchange.getRequest()).thenReturn(request);
        when(request.getHeaders()).thenReturn(headers);
        when(headers.getAccept()).thenReturn(List.of(MediaType.TEXT_HTML));

        // Mock ServerHttpResponse und DataBufferFactory
        when(serverWebExchange.getResponse()).thenReturn(response);
        when(response.getHeaders()).thenReturn(headers);
        when(response.bufferFactory()).thenReturn(dataBufferFactory);
        when(dataBufferFactory.wrap(dataBufferCaptor.capture())).thenReturn(mock(DataBuffer.class));
        when(response.writeWith(any())).thenReturn(Mono.empty());
        when(response.setComplete()).thenReturn(Mono.empty());

    }

    @Test
    void shouldHandle401Error() {
        setField(handler, "unauthorizedErrorPageUrl", "public/error/401.html");
        when(headers.getAccept()).thenReturn(List.of(MediaType.TEXT_HTML));

        Mono<Void> mono = handler.handle(serverWebExchange, new ResponseStatusException(HttpStatus.UNAUTHORIZED));

        StepVerifier.create(mono).verifyComplete();
        verify(response).setStatusCode(HttpStatusCode.valueOf(401));
        verify(headers).setContentType(MediaType.TEXT_HTML);
        String actualResponse = new String(dataBufferCaptor.getValue(), StandardCharsets.UTF_8);
        assertThat(actualResponse).contains("401");
    }

    @Test
    void shouldHandle403Error() {
        setField(handler, "forbiddenErrorPageUrl", "public/error/403.html");
        when(headers.getAccept()).thenReturn(List.of(MediaType.TEXT_HTML));

        Mono<Void> mono = handler.handle(serverWebExchange, new ResponseStatusException(HttpStatus.FORBIDDEN));

        StepVerifier.create(mono).verifyComplete();
        verify(response).setStatusCode(HttpStatusCode.valueOf(403));
        verify(headers).setContentType(MediaType.TEXT_HTML);
        String actualResponse = new String(dataBufferCaptor.getValue(), StandardCharsets.UTF_8);
        assertThat(actualResponse).contains("403");
    }

    @Test
    void shouldHandle404Error() {
        setField(handler, "notFoundErrorPageUrl", "public/error/404.html");
        when(headers.getAccept()).thenReturn(List.of(MediaType.TEXT_HTML));

        Mono<Void> mono = handler.handle(serverWebExchange, new ResponseStatusException(HttpStatus.NOT_FOUND));

        StepVerifier.create(mono).verifyComplete();
        verify(response).setStatusCode(HttpStatusCode.valueOf(404));
        verify(headers).setContentType(MediaType.TEXT_HTML);
        String actualResponse = new String(dataBufferCaptor.getValue(), StandardCharsets.UTF_8);
        assertThat(actualResponse).contains("404");
    }

    @Test
    void shouldHandle5xxError() {
        setField(handler, "internalServerErrorPageUrl", "public/error/500.html");
        when(headers.getAccept()).thenReturn(List.of(MediaType.TEXT_HTML));

        Mono<Void> mono = handler.handle(serverWebExchange, new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR));

        StepVerifier.create(mono).verifyComplete();
        verify(response).setStatusCode(HttpStatusCode.valueOf(500));
        verify(headers).setContentType(MediaType.TEXT_HTML);
        String actualResponse = new String(dataBufferCaptor.getValue(), StandardCharsets.UTF_8);
        assertThat(actualResponse).contains("500");
    }

    @Test
    void shouldRespondWithErrorIfErrorPageNotFound() {
        setField(handler, "internalServerErrorPageUrl", "idontexist");
        when(headers.getAccept()).thenReturn(List.of(MediaType.TEXT_HTML));

        Mono<Void> mono = handler.handle(serverWebExchange, new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR));

        StepVerifier.create(mono).verifyError();
    }

    @Test
    void shouldRespondWithInternalServerErrorPageForAnyOtherException() {
        setField(handler, "internalServerErrorPageUrl", "public/error/500.html");
        when(headers.getAccept()).thenReturn(List.of(MediaType.TEXT_HTML));

        Mono<Void> mono = handler.handle(serverWebExchange, new RuntimeException("Something went wrong!"));

        StepVerifier.create(mono).verifyComplete();
        verify(response).setStatusCode(HttpStatusCode.valueOf(500));
        String actualResponse = new String(dataBufferCaptor.getValue(), StandardCharsets.UTF_8);
        assertThat(actualResponse).contains("Something unexpected happened");
    }

    @Test
    void shouldHandleJsonResponse() {
        when(headers.getAccept()).thenReturn(List.of(MediaType.APPLICATION_JSON));

        Mono<Void> mono = handler.handle(serverWebExchange, new ResponseStatusException(HttpStatus.UNAUTHORIZED));

        StepVerifier.create(mono).verifyComplete();
        verify(response).setStatusCode(HttpStatus.UNAUTHORIZED);
        verify(headers).setContentType(MediaType.APPLICATION_JSON);
        String actualResponse = new String(dataBufferCaptor.getValue(), StandardCharsets.UTF_8);
        assertThat(actualResponse).contains("{\"errorCode\": 401, \"errorMessage\": \"401 UNAUTHORIZED\"}");
    }

    @Test
    void shouldHandleJsonResponseForInternalError() {
        when(headers.getAccept()).thenReturn(List.of(MediaType.APPLICATION_JSON));

        Mono<Void> mono = handler.handle(serverWebExchange, new RuntimeException("Something unexpected happened"));

        StepVerifier.create(mono).verifyComplete();
        verify(response).setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
        verify(headers).setContentType(MediaType.APPLICATION_JSON);
        String actualResponse = new String(dataBufferCaptor.getValue(), StandardCharsets.UTF_8);
        assertThat(actualResponse).contains("{\"errorCode\": 500, \"errorMessage\": \"Internal Server Error\"}");
    }


    @Test
    void shouldHandleExceptionWhenExchangeIsNull() {
        Mono<Void> mono = Mono.defer(() -> handler.handle(null, new RuntimeException("Test Exception")));

        StepVerifier.create(mono)
                .expectErrorMatches(throwable -> throwable instanceof ResponseStatusException &&
                        ((ResponseStatusException) throwable).getStatusCode() == HttpStatus.INTERNAL_SERVER_ERROR &&
                        "exchange is null".equals(((ResponseStatusException) throwable).getReason()))
                .verify();
    }

    @Test
    void shouldReturnFalseIfAcceptsHtmlThrowsException() {
        when(request.getHeaders()).thenThrow(new RuntimeException("Header exception"));

        Mono<Void> mono = handler.handle(serverWebExchange, new ResponseStatusException(HttpStatus.NOT_FOUND));

        StepVerifier.create(mono)
                .verifyComplete();
        verify(response).setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
        verify(response).setComplete();
    }

    @Test
    void shouldHandleExceptionInHandleMethod() {

        setField(handler, "unauthorizedErrorPageUrl", "public/error/401.html");
        setField(handler, "forbiddenErrorPageUrl", "public/error/403.html");
        setField(handler, "notFoundErrorPageUrl", "public/error/404.html");
        setField(handler, "internalServerErrorPageUrl", "public/error/500.html");

        Mono<Void> mono = Mono.defer(() -> handler.handle(null, new RuntimeException("Another Exception")));

        StepVerifier.create(mono)
                .verifyErrorMatches(throwable -> ((ResponseStatusException) throwable).getStatusCode().isSameCodeAs(HttpStatus.INTERNAL_SERVER_ERROR));

    }

    @Test
    void shouldReturnFallbackJsonResponseOnJsonProcessingException() throws JsonProcessingException {
        ObjectMapper objectMapperMock = mock(ObjectMapper.class);
        // Use reflection to set private final field
        setField(handler, "objectMapper", objectMapperMock);

        when(objectMapperMock.writeValueAsString(any(GlobalExceptionHandler.JsonResponse.class))).thenThrow(new JsonProcessingException("Test Exception") {});

        // When headers.getAccept() is called, it should not throw an exception
        when(request.getHeaders()).thenReturn(headers);
        when(headers.getAccept()).thenReturn(List.of(MediaType.APPLICATION_JSON));

        Mono<Void> mono = handler.handle(serverWebExchange, new RuntimeException("Test Exception"));

        StepVerifier.create(mono)
                .verifyComplete();
        verify(response).setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
        verify(headers).setContentType(MediaType.APPLICATION_JSON);
        String actualResponse = new String(dataBufferCaptor.getValue(), StandardCharsets.UTF_8);
        assertThat(actualResponse).contains("{\"errorCode\": 500, \"errorMessage\": \"Internal Server Error\"}");
    }

    @Test
    void shouldLogExchangeDetails(CapturedOutput output) {
        // Arrange
        when(request.getMethod()).thenReturn(HttpMethod.GET);
        when(request.getURI()).thenReturn(URI.create("http://localhost/test"));
        HttpHeaders requestHeaders = new HttpHeaders();
        requestHeaders.add("User-Agent", "JUnit");
        when(request.getHeaders()).thenReturn(requestHeaders);

        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.add("Content-Type", "application/json");
        when(response.getHeaders()).thenReturn(responseHeaders);

        when(response.getStatusCode()).thenReturn(HttpStatus.OK);

        Throwable exception = new RuntimeException("Test Exception");

        // Act
        handler.logExchangeDetails(serverWebExchange, exception);

        // Assert
        assertTrue(output.getOut().contains("Request: [method: GET, URI: http://localhost/test, headers: [User-Agent:\"JUnit\"]]"));
        assertTrue(output.getOut().contains("Response: [statusCode: 200 OK, headers: [Content-Type:\"application/json\"]]"));
        assertTrue(output.getOut().contains("Test Exception"));
    }

    @Test
    void shouldReturnValueWhenSupplierSucceeds() {
        // Arrange
        String expectedValue = "testValue";

        // Act
        Optional<String> result = getCapturingExceptions(() -> expectedValue, "return testValue");

        // Assert
        assertTrue(result.isPresent());
        assertEquals(expectedValue, result.get());
    }

    @Test
    void shouldReturnEmptyWhenSupplierThrowsException() {
        // Act
        Optional<String> result = getCapturingExceptions(() -> {
            throw new Exception("Test Exception");
        }, "throw test exception");

        // Assert
        assertTrue(result.isEmpty());
    }

    @Test
    void shouldReturnEmptyWhenSupplierReturnsNull() {
        // Act
        Optional<String> result = getCapturingExceptions(() -> null, "return null");

        // Assert
        assertTrue(result.isEmpty());
    }

    @Test
    void shouldLogExceptionWhenSupplierThrowsException(CapturedOutput capturedOutput) {
        getCapturingExceptions(() -> {
            throw new Exception("Test Exception");
        }, "throw test exception");

        assertTrue(capturedOutput.getOut().contains("Exception occurred while executing supplier [throw test exception]"));
        assertTrue(capturedOutput.getOut().contains("Test Exception"));
    }

    @Test
    void shouldHandleNullResponseStatusCode(CapturedOutput capturedOutput) {
        when(serverWebExchange.getResponse().getStatusCode()).thenThrow(new RuntimeException("Test Exception"));

        HttpStatusCode responseStatus = getCapturingExceptions(
                () -> serverWebExchange.getResponse().getStatusCode(),
                "exchange.getResponse().getStatusCode()"
        ).orElse(null);

        assertNull(responseStatus);
        assertTrue(capturedOutput.getOut().contains("RuntimeException: Test Exception"));
    }

    @Test
    void shouldHandleNullResponseHttpMethod(CapturedOutput capturedOutput) {
        when(serverWebExchange.getRequest().getMethod()).thenThrow(new RuntimeException("Test Exception"));

        HttpMethod nullMethod = HttpMethod.valueOf("null");

        HttpMethod requestMethod = getCapturingExceptions(
                () -> serverWebExchange.getRequest().getMethod(),
                "exchange.getRequest().getMethod()"
        ).orElse(nullMethod);

        assertEquals(requestMethod, nullMethod);
        assertTrue(capturedOutput.getOut().contains("RuntimeException: Test Exception"));
    }

}