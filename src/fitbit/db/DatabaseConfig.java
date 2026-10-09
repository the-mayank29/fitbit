package fitbit.db;

/**
 * Configuration settings for Database Connectivity.
 * Demonstrates database integration configuration for Review 1.
 */
public class DatabaseConfig {
    public static final String DB_DRIVER = "org.sqlite.JDBC";
    public static final String DB_URL = "jdbc:sqlite:data/fitbit.db";
    public static final String DB_SCHEMA_FILE = "data/schema.sql";
    public static final int CONNECTION_TIMEOUT_SECONDS = 5;

    private DatabaseConfig() {
        // Utility configuration class
    }
}
