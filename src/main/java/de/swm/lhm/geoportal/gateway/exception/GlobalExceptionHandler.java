package de.swm.lhm.geoportal.gateway.exception;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.util.function.ThrowingSupplier;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import static de.swm.lhm.geoportal.gateway.util.HtmlServeUtils.createHtmlResponse;

@RequiredArgsConstructor
@Component
@Slf4j
@Order(-2)
public class GlobalExceptionHandler implements ErrorWebExceptionHandler {

    private static final String INTERNAL_SERVER_ERROR_MESSAGE = "Internal Server Error";
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${geoportal.gateway.error-urls.401}")
    private String unauthorizedErrorPageUrl;

    @Value("${geoportal.gateway.error-urls.403}")
    private String forbiddenErrorPageUrl;

    @Value("${geoportal.gateway.error-urls.404}")
    private String notFoundErrorPageUrl;

    @Value("${geoportal.gateway.error-urls.500}")
    private String internalServerErrorPageUrl;

    @Override
    @NonNull
    public Mono<Void> handle(@Nullable ServerWebExchange exchange, @Nullable Throwable ex) {

        if (exchange == null) {
            log.error("ServerWebExchange is null");
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "exchange is null");
        }

        logExchangeDetails(exchange, ex);

        try {
            if (acceptsOnlyJson(exchange)) {
                return handleJsonResponse(exchange, ex);
            } else {
                return handleHtmlResponse(exchange, ex);
            }
        } catch (Exception e) {
            log.error("Error occurred while handling exception", e);
            exchange.getResponse().setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
            return exchange.getResponse().setComplete();
        }
    }

    void logExchangeDetails(ServerWebExchange exchange, Throwable ex) {

        HttpMethod requestMethod = getCapturingExceptions(
                () -> exchange.getRequest().getMethod(),
                "exchange.getRequest().getMethod()"
        ).orElse(HttpMethod.valueOf("null"));

        URI requestUri = getCapturingExceptions(
                () -> exchange.getRequest().getURI(),
                "exchange.getRequest().getURI()"
        ).orElse(URI.create(""));

        HttpHeaders requestHeaders = getCapturingExceptions(
                () -> exchange.getRequest().getHeaders(),
                "exchange.getRequest().getHeaders()"
        ).orElse(HttpHeaders.EMPTY);

        String requestDetails = String.format(
                "Request: [method: %s, URI: %s, headers: %s]",
                requestMethod,
                requestUri,
                requestHeaders
        );

        HttpStatusCode responseStatus = getCapturingExceptions(
                () -> exchange.getResponse().getStatusCode(),
                "exchange.getResponse().getStatusCode()"
        ).orElse(null);

        HttpHeaders responseHeaders = getCapturingExceptions(
                () -> exchange.getResponse().getHeaders(),
                "exchange.getResponse().getHeaders()"
        ).orElse(HttpHeaders.EMPTY);

        String responseDetails = String.format(
                "Response: [statusCode: %s, headers: %s]",
                responseStatus,
                responseHeaders
        );

        log.atError()
           .setMessage(() -> String.format("Exchange Details:\n%s\n%s\nException: ", requestDetails, responseDetails))
           .setCause(ex)
           .log();
    }

    static <U> Optional<U> getCapturingExceptions(ThrowingSupplier<U> supplier, String description) {
        try {
            return Optional.ofNullable(supplier.get());
        } catch (Exception e) {
            log.atError()
               .setMessage(() -> String.format("Exception occurred while executing supplier [%s]: %s", description, e.getMessage()))
               .setCause(e)
               .log();
            return Optional.empty();
        }
    }

    private boolean acceptsOnlyJson(ServerWebExchange exchange) {
        List<MediaType> acceptHeaders = getCapturingExceptions(
                () -> exchange.getRequest().getHeaders().getAccept(),
                "exchange.getRequest().getHeaders().getAccept()"
        ).orElse(List.of());
        return acceptHeaders.contains(MediaType.APPLICATION_JSON)
                && !acceptHeaders.contains(MediaType.TEXT_HTML);
    }

    private Mono<Void> handleJsonResponse(ServerWebExchange exchange, Throwable ex) {
        JsonResponse jsonResponse;
        if (ex instanceof ResponseStatusException responseStatusException) {
            HttpStatusCode status = responseStatusException.getStatusCode();
            jsonResponse = JsonResponse.builder()
                    .errorCode(status.value())
                    .errorMessage(responseStatusException.getReason())
                    .build();
            exchange.getResponse().setStatusCode(status);
        } else {
            jsonResponse = JsonResponse.builder()
                    .errorCode(500)
                    .errorMessage(INTERNAL_SERVER_ERROR_MESSAGE)
                    .build();
            exchange.getResponse().setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
        }

        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        try {
            String json = objectMapper.writeValueAsString(jsonResponse);
            byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
            return exchange.getResponse().writeWith(Mono.just(exchange.getResponse().bufferFactory().wrap(bytes)));
        } catch (JsonProcessingException e) {
            log.error("Error serializing json response", e);
            byte[] bytes = "{\"errorCode\": 500, \"errorMessage\": \"%s\"}".formatted(INTERNAL_SERVER_ERROR_MESSAGE).getBytes(StandardCharsets.UTF_8);
            return exchange.getResponse().writeWith(Mono.just(exchange.getResponse().bufferFactory().wrap(bytes)));
        }
    }

    private Mono<Void> handleHtmlResponse(ServerWebExchange exchange, Throwable ex) {
        if (ex instanceof ResponseStatusException responseStatusException) {
            HttpStatusCode status = responseStatusException.getStatusCode();
            return switch (status.value()) {
                case 401 -> createHtmlResponse(exchange, unauthorizedErrorPageUrl, status);
                case 403 -> createHtmlResponse(exchange, forbiddenErrorPageUrl, status);
                case 404 -> createHtmlResponse(exchange, notFoundErrorPageUrl, status);
                default -> {
                    log.info("Global exception", ex);
                    yield createHtmlResponse(exchange, internalServerErrorPageUrl, status);
                }
            };
        } else {
            log.error("Global exception without explicit response status", ex);
        }
        return createHtmlResponse(exchange, internalServerErrorPageUrl, HttpStatusCode.valueOf(500));
    }

    @Data
    @Builder
    public static final class JsonResponse {
        private int errorCode;
        private String errorMessage;
    }
}