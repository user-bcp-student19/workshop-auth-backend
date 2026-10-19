package com.bcp.workshop.auth;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class LegacyDatabase {
    private static final String DB_URL = System.getenv().getOrDefault("AUTH_DB_URL", "jdbc:sqlite:./data/workshop.db");
    private static final String HASH_PREFIX = "pbkdf2-sha256$";

    private final String dbUrl;
    private final PasswordHasher passwordHasher;

    @Inject
    LegacyDatabase(PasswordHasher passwordHasher) {
        this(DB_URL, passwordHasher);
    }

    LegacyDatabase(String dbUrl, PasswordHasher passwordHasher) {
        this.dbUrl = dbUrl;
        this.passwordHasher = passwordHasher;
    }

    @PostConstruct
    void initialize() {
        try {
            Files.createDirectories(Path.of("data"));
            try (Connection connection = DriverManager.getConnection(dbUrl)) {
                connection.setAutoCommit(false);
                try (Statement statement = connection.createStatement()) {
                statement.executeUpdate("CREATE TABLE IF NOT EXISTS legacy_users (id INTEGER PRIMARY KEY AUTOINCREMENT, username TEXT UNIQUE, password TEXT NOT NULL, display_name TEXT NOT NULL, role TEXT NOT NULL)");
                }
                migratePlaintextPasswords(connection);
                insertSeedUser(connection, "student", "Workshop123!", "Workshop Student", "STUDENT");
                insertSeedUser(connection, "trainer", "Trainer123!", "Workshop Trainer", "TRAINER");
                connection.commit();
            } catch (Exception error) {
                error.printStackTrace();
            }
        } catch (Exception error) {
            error.printStackTrace();
        }
    }

    public UserRecord findByCredentials(String username, String password) {
        String sql = "SELECT id, username, password, display_name, role FROM legacy_users WHERE username = ?";
        try (Connection connection = DriverManager.getConnection(dbUrl);
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, username);
            try (ResultSet result = statement.executeQuery()) {
            if (result.next()) {
                    String storedPassword = result.getString("password");
                    if (passwordHasher.verify(password, storedPassword)) {
                        return new UserRecord(result.getLong("id"), result.getString("username"), result.getString("display_name"), result.getString("role"));
                    }
                }
            }
        } catch (Exception error) {
            error.printStackTrace();
        }
        return null;
    }

    private void migratePlaintextPasswords(Connection connection) throws Exception {
        List<LegacyPassword> legacyPasswords = new ArrayList<>();
        try (Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery("SELECT id, password FROM legacy_users")) {
            while (result.next()) {
                String storedPassword = result.getString("password");
                if (!storedPassword.startsWith(HASH_PREFIX)) {
                    legacyPasswords.add(new LegacyPassword(result.getLong("id"), storedPassword));
                }
            }
        }

        try (PreparedStatement statement = connection.prepareStatement("UPDATE legacy_users SET password = ? WHERE id = ?")) {
            for (LegacyPassword legacyPassword : legacyPasswords) {
                statement.setString(1, passwordHasher.hash(legacyPassword.value()));
                statement.setLong(2, legacyPassword.userId());
                statement.executeUpdate();
            }
        }
    }

    private void insertSeedUser(Connection connection, String username, String password, String displayName, String role) throws Exception {
        String sql = "INSERT OR IGNORE INTO legacy_users(username,password,display_name,role) VALUES (?,?,?,?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, username);
            statement.setString(2, passwordHasher.hash(password));
            statement.setString(3, displayName);
            statement.setString(4, role);
            statement.executeUpdate();
        }
    }

    private record LegacyPassword(long userId, String value) {}
}
