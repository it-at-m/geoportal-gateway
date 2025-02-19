package de.swm.lhm.geoportal.gateway.shared.exceptions;

import org.springframework.http.HttpStatusCode;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ResponseStatusException;

public class LoggedResponseStatusException extends ResponseStatusException {

    private final boolean isLogged;

    public LoggedResponseStatusException(HttpStatusCode status, String reason, boolean isLogged) {
        super(status, reason);
        this.isLogged = isLogged;
    }

    public LoggedResponseStatusException(HttpStatusCode status, String reason, Throwable cause, boolean isLogged) {
        super(status, reason, cause);
        this.isLogged = isLogged;
    }

    public boolean isLogged() {
        return isLogged;
    }

    // Method to convert ResponseStatusException to LoggedResponseStatusException
    public static LoggedResponseStatusException from(ResponseStatusException ex, boolean isLogged) {
        return new LoggedResponseStatusException(ex.getStatusCode(), ex.getReason(), ex.getCause(), isLogged);
    }

    // Method to convert WebClientResponseException to LoggedResponseStatusException
    public static LoggedResponseStatusException from(WebClientResponseException ex, String customMessage, boolean isLogged) {
        String responseBody = ex.getResponseBodyAsString();
        String reason = String.format("%s: %s: %s", customMessage, ex.getStatusText(), responseBody);
        return new LoggedResponseStatusException(ex.getStatusCode(), reason, ex, isLogged);
    }

    // Method to convert Throwable to LoggedResponseStatusException
    public static LoggedResponseStatusException from(Throwable ex, HttpStatusCode status, String customMessage, boolean isLogged) {
        return new LoggedResponseStatusException(status, customMessage, ex, isLogged);
    }
}