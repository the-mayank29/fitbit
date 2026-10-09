package fitbit.db;

import fitbit.exception.DatabaseException;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Singleton Connection Factory for Database Connectivity.
 * Demonstrates JDBC driver loading, connection pooling design, and exception handling.
 */
public class DbConnectionFactory {
    private static volatile DbConnectionFactory instance;
    private boolean driverLoaded = false;

    private DbConnectionFactory() {
        try {
            Class.forName(DatabaseConfig.DB_DRIVER);
            driverLoaded = true;
        } catch (ClassNotFoundException e) {
            // Driver will be present when running with Maven dependency
            driverLoaded = false;
        }
    }

    public static DbConnectionFactory getInstance() {
        if (instance == null) {
            synchronized (DbConnectionFactory.class) {
                if (instance == null) {
                    instance = new DbConnectionFactory();
                }
            }
        }
        return instance;
    }

    /**
     * Obtains an active JDBC Connection to the SQLite database.
     *
     * @return java.sql.Connection
     * @throws DatabaseException on connection failure
     */
    public Connection getConnection() throws DatabaseException {
        try {
            return DriverManager.getConnection(DatabaseConfig.DB_URL);
        } catch (SQLException e) {
            throw new DatabaseException("Failed to establish database connection to: " + DatabaseConfig.DB_URL, e);
        }
    }

    public boolean isDriverAvailable() {
        return driverLoaded;
    }

    public static void closeQuietly(AutoCloseable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (Exception ignored) {
            }
        }
    }
}
