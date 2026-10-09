package fitbit.exception;

/**
 * Thrown when input validation fails for any fitness metric or entity.
 */
public class ValidationException extends FitnessException {
    private final String fieldName;

    public ValidationException(String message, String fieldName) {
        super(message, "VALIDATION_FAILED");
        this.fieldName = fieldName;
    }

    public String getFieldName() {
        return fieldName;
    }
}
