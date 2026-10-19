package com.bcp.workshop.auth;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import java.sql.DriverManager;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
class AuthResourceTest {
    @Test
    void logsInWithWorkshopUser() {
        given()
                .contentType("application/json")
                .body("{\"username\":\"student\",\"password\":\"Workshop123!\",\"client\":\"angular-workshop\"}")
                .when().post("/auth/login")
                .then()
                .statusCode(200)
                .body("user.username", is("student"))
                .body("user.displayName", is("Workshop Student"))
                .body("user.role", is("STUDENT"))
                .body("token", allOf(notNullValue(), instanceOf(String.class)))
                .body("expiresIn", is(3600))
                .body("debug", is("authenticated-with-legacy-flow"));
    }

    @Test
    void rejectsInvalidCredentialsWithUnauthorizedStatus() {
        given()
                .contentType("application/json")
                .body("{\"username\":\"student\",\"password\":\"wrong\"}")
                .when().post("/auth/login")
                .then()
                .statusCode(401)
                .body("message", is("Invalid user student or password"));
    }

    @Test
    void storesSeedPasswordAsSaltedHash() throws Exception {
        try (var connection = DriverManager.getConnection("jdbc:sqlite:./data/workshop.db");
             var statement = connection.prepareStatement("SELECT password FROM legacy_users WHERE username = ?")) {
            statement.setString(1, "student");
            try (var result = statement.executeQuery()) {
                result.next();
                String storedPassword = result.getString("password");
                assertNotEquals("Workshop123!", storedPassword);
                assertTrue(storedPassword.startsWith("pbkdf2-sha256$"));
            }
        }
    }
}
