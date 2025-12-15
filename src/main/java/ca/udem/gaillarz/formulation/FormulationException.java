package ca.udem.gaillarz.formulation;

/**
 * Exception thrown when there's an error with formulation operations.
 */
public class FormulationException extends RuntimeException {

    /**
     * Creates a new FormulationException with a message.
     *
     * @param message Description of the error
     */
    public FormulationException(String message) {
        super(message);
    }

    /**
     * Creates a new FormulationException with a message and cause.
     *
     * @param message Description of the error
     * @param cause   The underlying cause
     */
    public FormulationException(String message, Throwable cause) {
        super(message, cause);
    }
}
