package de.swm.lhm.geoportal.gateway.shared.exceptions;

public class DeserializationException extends RuntimeException {
    public DeserializationException(String message, Throwable cause) {
        super(message, cause);
    }
}
