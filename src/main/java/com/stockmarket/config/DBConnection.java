package com.stockmarket.config;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Database connection manager using Oracle JDBC Driver.
 * Reads configuration from db.properties or environment variables,
 * and provides robust diagnostic testing and connection pooling helpers.
 */
public class DBConnection {
    private static final Logger LOGGER = Logger.getLogger(DBConnection.class.getName());

    private static String jdbcUrl;
    private static String dbUser;
    private static String dbPassword;
    private static String driverClass;

    static {
        loadConfiguration();
    }

    private static void loadConfiguration() {
        Properties props = new Properties();

        // 1. Attempt to load db.properties from ClassLoader
        try (InputStream input = DBConnection.class.getClassLoader().getResourceAsStream("db.properties")) {
            if (input != null) {
                props.load(input);
                LOGGER.info("Loaded database configuration from classpath db.properties");
            } else {
                LOGGER.warning("db.properties not found in classpath. Checking environment variables.");
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error reading db.properties, using fallback settings: " + e.getMessage());
        }

        // 2. Read properties with fallbacks to Environment Variables, then safe defaults
        driverClass = getSetting(props, "db.driver", "DB_DRIVER", "oracle.jdbc.OracleDriver");
        
        // Oracle XE 11g/21c defaults to port 1521 and SID 'xe' or service 'orcl'
        jdbcUrl = getSetting(props, "db.url", "DB_URL", "jdbc:oracle:thin:@//localhost:1521/xe");
        dbUser = getSetting(props, "db.user", "DB_USER", "STOCK_USER");
        dbPassword = getSetting(props, "db.password", "DB_PASSWORD", "oracle");

        // 3. Register JDBC Driver class
        try {
            Class.forName(driverClass);
            LOGGER.info("Oracle JDBC Driver loaded successfully: " + driverClass);
        } catch (ClassNotFoundException e) {
            LOGGER.log(Level.SEVERE, "Oracle JDBC Driver NOT found in classpath! Ensure ojdbc jar is present.", e);
        }
    }

    private static String getSetting(Properties props, String propKey, String envKey, String defaultValue) {
        String envVal = System.getenv(envKey);
        if (envVal != null && !envVal.trim().isEmpty()) {
            return envVal.trim();
        }
        String propVal = props.getProperty(propKey);
        if (propVal != null && !propVal.trim().isEmpty()) {
            return propVal.trim();
        }
        return defaultValue;
    }

    /**
     * Obtains an open Connection to the Oracle Database.
     * Callers must close connection using try-with-resources.
     *
     * @return Connection instance
     * @throws SQLException if a database access error occurs
     */
    public static Connection getConnection() throws SQLException {
        try {
            return DriverManager.getConnection(jdbcUrl, dbUser, dbPassword);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to connect to Oracle Database at " + jdbcUrl + " with user " + dbUser + ": " + e.getMessage());
            throw e;
        }
    }

    /**
     * Diagnostic method to test connectivity and output database metadata.
     */
    public static boolean testConnection() {
        LOGGER.info("Testing Oracle Database connection to: " + jdbcUrl);
        try (Connection conn = getConnection()) {
            boolean valid = conn != null && !conn.isClosed();
            if (valid) {
                LOGGER.info("Connection successful! Oracle Database Product: " 
                    + conn.getMetaData().getDatabaseProductName() + " " 
                    + conn.getMetaData().getDatabaseProductVersion());
            }
            return valid;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Database connection test FAILED: " + e.getMessage(), e);
            return false;
        }
    }

    // Getters for status and diagnostics
    public static String getJdbcUrl() { return jdbcUrl; }
    public static String getDbUser() { return dbUser; }
    public static String getDriverClass() { return driverClass; }
}
