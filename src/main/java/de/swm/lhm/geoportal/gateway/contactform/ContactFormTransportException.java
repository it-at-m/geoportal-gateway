package de.swm.lhm.geoportal.gateway.contactform;

/**
 * Thrown in case an error occurs during the transport of contact form data.
 */
public class ContactFormTransportException extends RuntimeException {

    /**
     * Constructor with message
     *
     * @param message the message to show/print
     */
    public ContactFormTransportException(String message) {
        super(message);
    }

    /**
     * Constructor with message and original Exception as {@link Throwable}
     *
     * @param message the message to show/print
     * @param cause   the cause of the exception
     */
    public ContactFormTransportException(String message, Throwable cause) {
        super(message, cause);
    }
}
