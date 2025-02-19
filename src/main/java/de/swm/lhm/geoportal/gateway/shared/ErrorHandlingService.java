package de.swm.lhm.geoportal.gateway.shared;

import de.swm.lhm.geoportal.gateway.shared.exceptions.LoggedResponseStatusException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

@Slf4j
public abstract class ErrorHandlingService {

    protected static final String ERROR_TEMPLATE_STATUS_MESSAGE_BODY = "%s - %s - %s - %s";
    protected static final String ERROR_TEMPLATE_STATUS_MESSAGE_BODY_CONTEXT = "%s - %s - %s [Context: %s] - %s";

    public ResponseStatusException handleException(String message, Throwable throwable) {
        switch (throwable) {
            case LoggedResponseStatusException loggedResponseStatusException -> {
                if (!loggedResponseStatusException.isLogged()) {
                    log.atError()
                            .setMessage(() -> String.format(
                                    ERROR_TEMPLATE_STATUS_MESSAGE_BODY,
                                    loggedResponseStatusException.getStatusCode().value(),
                                    getStatusText(loggedResponseStatusException.getStatusCode()),
                                    message,
                                    loggedResponseStatusException.getReason()))
                            .setCause(loggedResponseStatusException)
                            .log();
                }
                return loggedResponseStatusException;
            }
            case ResponseStatusException responseStatusException -> {
                log.atError()
                        .setMessage(() -> String.format(
                                ERROR_TEMPLATE_STATUS_MESSAGE_BODY,
                                responseStatusException.getStatusCode().value(),
                                getStatusText(responseStatusException.getStatusCode()),
                                message,
                                responseStatusException.getReason()))
                        .setCause(responseStatusException)
                        .log();
                return LoggedResponseStatusException.from(responseStatusException, true);
            }
            case WebClientResponseException webClientResponseException -> {
                HttpStatusCode statusCode = webClientResponseException.getStatusCode();
                String statusText = webClientResponseException.getStatusText();
                String responseBody = webClientResponseException.getResponseBodyAsString();

                log.atError()
                        .setMessage(() -> String.format(
                                ERROR_TEMPLATE_STATUS_MESSAGE_BODY,
                                statusCode.value(),
                                statusText,
                                message,
                                responseBody))
                        .setCause(webClientResponseException)
                        .log();
                return LoggedResponseStatusException.from(webClientResponseException, message, true);
            }
            case null, default -> {
                log.atError()
                        .setMessage(() -> String.format(
                                ERROR_TEMPLATE_STATUS_MESSAGE_BODY,
                                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                                getStatusText(HttpStatus.INTERNAL_SERVER_ERROR),
                                message,
                                throwable != null ? throwable.getMessage() : "No Message provided"))
                        .setCause(throwable)
                        .log();
                return LoggedResponseStatusException.from(throwable, HttpStatus.INTERNAL_SERVER_ERROR, message, true);
            }
        }
    }

    public <T extends Throwable> Mono<T> handleErrorResponse(HttpStatusCode status, String message, ClientResponse clientResponse, String context) {
        return clientResponse.bodyToMono(String.class).flatMap(body -> {
            log.atError()
                    .setMessage(() -> String.format(
                            ERROR_TEMPLATE_STATUS_MESSAGE_BODY_CONTEXT,
                            status.value(),
                            getStatusText(status),
                            message,
                            context,
                            body))
                    .log();
            LoggedResponseStatusException exception = new LoggedResponseStatusException(status, String.format("%s: %s", message, body), true);
            return Mono.error(exception);
        });
    }

    private String getStatusText(HttpStatusCode statusCode) {
        HttpStatus status = HttpStatus.resolve(statusCode.value());
        return status != null ? status.getReasonPhrase() : "Unknown Status";
    }
}
