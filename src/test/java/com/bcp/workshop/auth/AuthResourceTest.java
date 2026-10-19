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
    void rejectsUnknownUserWithUnauthorizedStatus() {
        given()
                .contentType("application/json")
                .body("{\"username\":\"unknown-user\",\"password\":\"any-password\"}")
                .when().post("/auth/login")
                .then()
                .statusCode(401)
                .body("message", is("Invalid user unknown-user or password"));
    }

    @Test
    void acceptsRequestWithoutOptionalClient() {
        given()
                .contentType("application/json")
                .body("{\"username\":\"student\",\"password\":\"Workshop123!\"}")
                .when().post("/auth/login")
                .then()
                .statusCode(200)
                .body("user.username", is("student"));
    }

    @Test
    void rejectsNullRequestBody() {
        given()
                .contentType("application/json")
                .body("null")
                .when().post("/auth/login")
                .then()
                .statusCode(400)
                .body("message", is("Request is null"));
    }

    @Test
    void rejectsMissingUsername() {
        given()
                .contentType("application/json")
                .body("{\"password\":\"Workshop123!\"}")
                .when().post("/auth/login")
                .then()
                .statusCode(400)
                .body("message", is("Username is required"));
    }

    @Test
    void rejectsBlankUsername() {
        given()
                .contentType("application/json")
                .body("{\"username\":\"  \",\"password\":\"Workshop123!\"}")
                .when().post("/auth/login")
                .then()
                .statusCode(400)
                .body("message", is("Username is required"));
    }

    @Test
    void rejectsMissingPassword() {
        given()
                .contentType("application/json")
                .body("{\"username\":\"student\"}")
                .when().post("/auth/login")
                .then()
                .statusCode(400)
                .body("message", is("Password is required"));
    }

    @Test
    void rejectsBlankPassword() {
        given()
                .contentType("application/json")
                .body("{\"username\":\"student\",\"password\":\"  \"}")
                .when().post("/auth/login")
                .then()
                .statusCode(400)
                .body("message", is("Password is required"));
    }

    @Test
    void rejectsMalformedJsonWithEmptyBody() {
        given()
                .contentType("application/json")
                .body("{bad json")
                .when().post("/auth/login")
                .then()
                .statusCode(400)
                .body(is(""));
    }

    @Test
    void rejectsUnsupportedHttpMethodWithEmptyBody() {
        given()
                .when().get("/auth/login")
                .then()
                .statusCode(405)
                .body(is(""));
    }

    @Test
    void rejectsUnacceptableResponseTypeWithEmptyBody() {
        given()
                .accept("text/plain")
                .contentType("application/json")
                .body("{\"username\":\"student\",\"password\":\"Workshop123!\"}")
                .when().post("/auth/login")
                .then()
                .statusCode(406)
                .body(is(""));
    }

    @Test
    void rejectsUnsupportedRequestTypeWithEmptyBody() {
        given()
                .contentType("text/plain")
                .body("{}")
                .when().post("/auth/login")
                .then()
                .statusCode(415)
                .body(is(""));
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
