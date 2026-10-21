package com.bcp.workshop.auth;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LegacyDatabaseCharacterizationTest {
    @Test
    void capturesInitializationFailureWithoutDatabaseContents() throws Exception {
        String databaseUrl = System.getenv("AUTH_DB_URL");
        assertNotNull(databaseUrl, "Surefire must provide an isolated AUTH_DB_URL");
        assertTrue(databaseUrl.contains("target"), "AUTH_DB_URL must point inside target/");

        Path databasePath = Path.of(databaseUrl.substring("jdbc:sqlite:".length()));
        Files.deleteIfExists(databasePath);
        Files.createDirectory(databasePath);

        List<String> logMessages;
        try {
            logMessages = captureDatabaseLogs(() ->
                    new LegacyDatabase().initialize());
        } finally {
            Files.delete(databasePath);
            new LegacyDatabase().initialize();
        }

        assertEquals(List.of("Authentication database initialization failed"), logMessages);
    }

    @Test
    void returnsNoUserAndDoesNotPrintSyntheticCredentialsWhenTableIsUnavailable() throws Exception {
        String databaseUrl = System.getenv("AUTH_DB_URL");
        assertNotNull(databaseUrl, "Set AUTH_DB_URL to an isolated SQLite file under target/");
        assertTrue(databaseUrl.contains("target"), "AUTH_DB_URL must point inside target/");

        LegacyDatabase database = new LegacyDatabase();
        database.initialize();
        try (Connection connection = DriverManager.getConnection(databaseUrl);
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("DROP TABLE legacy_users");
        }

        UserRecord[] result = new UserRecord[1];
        List<String> logMessages;
        try {
            logMessages = captureDatabaseLogs(() ->
                    result[0] = database.findByCredentials("synthetic-user", "synthetic-password"));
        } finally {
            database.initialize();
        }

        assertNull(result[0]);
        assertEquals(List.of("Authentication credential lookup failed"), logMessages);
        assertFalse(logMessages.toString().contains("synthetic-user"));
        assertFalse(logMessages.toString().contains("synthetic-password"));
    }

    private List<String> captureDatabaseLogs(Runnable action) {
        Logger logger = Logger.getLogger(LegacyDatabase.class.getName());
        List<String> messages = new ArrayList<>();
        Handler handler = new Handler() {
            @Override
            public void publish(LogRecord record) {
                if (isLoggable(record)) {
                    messages.add(record.getMessage());
                }
            }

            @Override
            public void flush() {}

            @Override
            public void close() {}
        };
        Level previousLevel = logger.getLevel();
        boolean previousParentHandlers = logger.getUseParentHandlers();
        logger.setLevel(Level.ALL);
        logger.setUseParentHandlers(false);
        logger.addHandler(handler);
        try {
            action.run();
        } finally {
            logger.removeHandler(handler);
            logger.setLevel(previousLevel);
            logger.setUseParentHandlers(previousParentHandlers);
        }
        return messages;
    }
}
