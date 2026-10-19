package com.bcp.workshop.auth;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.DriverManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LegacyDatabaseTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void migratesExistingPasswordsOnceAndVerifiesCredentials() throws Exception {
        String dbUrl = "jdbc:sqlite:" + temporaryDirectory.resolve("legacy.db");
        createLegacyDatabase(dbUrl);
        LegacyDatabase database = new LegacyDatabase(dbUrl, new PasswordHasher());

        database.initialize();

        assertNotNull(database.findByCredentials("existing-user", "ExistingPassword123!"));
        assertNull(database.findByCredentials("existing-user", "incorrect-password"));
        assertNull(database.findByCredentials("' OR 1=1 --", "ExistingPassword123!"));
        assertNotNull(database.findByCredentials("student", "Workshop123!"));

        String storedHash = readStoredPassword(dbUrl, "existing-user");
        assertNotEquals("ExistingPassword123!", storedHash);
        assertTrue(storedHash.startsWith("pbkdf2-sha256$"));
        assertTrue(new PasswordHasher().verify("ExistingPassword123!", storedHash));

        database.initialize();

        assertEquals(storedHash, readStoredPassword(dbUrl, "existing-user"));
    }

    private void createLegacyDatabase(String dbUrl) throws Exception {
        Class.forName("org.sqlite.JDBC");
        try (var connection = DriverManager.getConnection(dbUrl);
             var statement = connection.createStatement()) {
            statement.executeUpdate("CREATE TABLE legacy_users (id INTEGER PRIMARY KEY AUTOINCREMENT, username TEXT UNIQUE, password TEXT NOT NULL, display_name TEXT NOT NULL, role TEXT NOT NULL)");
            try (var insert = connection.prepareStatement("INSERT INTO legacy_users(username,password,display_name,role) VALUES (?,?,?,?)")) {
                insert.setString(1, "existing-user");
                insert.setString(2, "ExistingPassword123!");
                insert.setString(3, "Existing User");
                insert.setString(4, "USER");
                insert.executeUpdate();
            }
        }
    }

    private String readStoredPassword(String dbUrl, String username) throws Exception {
        Class.forName("org.sqlite.JDBC");
        try (var connection = DriverManager.getConnection(dbUrl);
             var statement = connection.prepareStatement("SELECT password FROM legacy_users WHERE username = ?")) {
            statement.setString(1, username);
            try (var result = statement.executeQuery()) {
                assertTrue(result.next());
                return result.getString("password");
            }
        }
    }
}