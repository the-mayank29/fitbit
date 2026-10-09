package fitbit.exception;

/**
 * Base domain exception for the Fitbit Application.
 * Demonstrates clean OOP exception hierarchy.
 */
public class FitnessException extends Exception {
    private final String errorCode;

    public FitnessException(String message) {
        super(message);
        this.errorCode = "FITNESS_ERROR";
    }

    public FitnessException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public FitnessException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "FITNESS_INTERNAL_ERROR";
    }

    public String getErrorCode() {
        return errorCode;
    }
}
