package ca.udem.gaillarz.io;

/**
 * Exception thrown when an MKP instance file has an invalid format.
 */
public class InvalidInstanceException extends Exception {

    /**
     * Creates a new InvalidInstanceException with a message.
     *
     * @param message Description of the error
     */
    public InvalidInstanceException(String message) {
        super(message);
    }

    /**
     * Creates a new InvalidInstanceException with a message and cause.
     *
     * @param message Description of the error
     * @param cause   The underlying cause
     */
    public InvalidInstanceException(String message, Throwable cause) {
        super(message, cause);
    }
}

