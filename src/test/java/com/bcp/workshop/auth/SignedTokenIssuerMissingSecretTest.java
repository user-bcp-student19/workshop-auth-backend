package com.bcp.workshop.auth;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.hamcrest.CoreMatchers.is;

@QuarkusTest
@TestProfile(SignedTokenIssuerMissingSecretTest.MissingSecretProfile.class)
class SignedTokenIssuerMissingSecretTest {
    @Inject
    SignedTokenIssuer tokenIssuer;

    @Test
    void applicationStartsWithoutSecretButEmissionFailsClosed() {
        IllegalStateException error = assertThrows(
                IllegalStateException.class,
                () -> tokenIssuer.issue("student", "synthetic-client", "STUDENT"));

        assertEquals("Authentication token secret is required", error.getMessage());
    }

    @Test
    void loginWithoutSecretFailsWithoutTokenAndHealthRemainsPublic() {
        var loginResponse = given()
                .contentType("application/json")
                .body("{\"username\":\"student\",\"password\":\"Workshop123!\",\"client\":\"synthetic-client\"}")
                .when().post("/auth/login")
                .then()
                .statusCode(500)
                .body("message", is("Authentication token secret is required"))
                .extract().response();

        assertNull(loginResponse.jsonPath().get("token"));

        given()
                .accept("application/json")
                .when().get("/q/health")
                .then()
                .statusCode(200)
                .body("status", is("UP"));
    }

    public static class MissingSecretProfile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of();
        }
    }
}