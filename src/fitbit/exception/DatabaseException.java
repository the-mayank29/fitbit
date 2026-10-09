package fitbit.exception;

/**
 * Thrown when a database connectivity, SQL execution, or transaction error occurs.
 */
public class DatabaseException extends FitnessException {
    public DatabaseException(String message) {
        super(message, "DATABASE_ERROR");
    }

    public DatabaseException(String message, Throwable cause) {
        super(message, cause);
    }
}
