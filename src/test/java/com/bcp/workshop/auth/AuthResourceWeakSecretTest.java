package com.bcp.workshop.auth;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.quarkus.test.junit.QuarkusTestProfile;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

@QuarkusTest
@TestProfile(AuthResourceWeakSecretTest.WeakSecretProfile.class)
class AuthResourceWeakSecretTest {
    @Test
    void returnsInternalServerErrorWhenSigningSecretIsTooShort() {
        given()
                .contentType("application/json")
                .body("{\"username\":\"student\",\"password\":\"Workshop123!\"}")
                .when().post("/auth/login")
                .then()
                .statusCode(500)
                .body("message", is("Authentication token secret must contain at least 32 bytes"))
                .body("exception", is("java.lang.IllegalStateException"));
    }

    public static class WeakSecretProfile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of("auth.token.secret", "short");
        }
    }
}