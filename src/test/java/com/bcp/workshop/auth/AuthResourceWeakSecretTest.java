package com.bcp.workshop.auth;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.nullValue;

@QuarkusTest
@TestProfile(AuthResourceWeakSecretTest.ShortSecretProfile.class)
class AuthResourceWeakSecretTest {
    @Test
    void loginWithShortSecretPreservesDocumented500WithoutIssuingToken() {
        given()
                .contentType("application/json")
                .body("{\"username\":\"student\",\"password\":\"Workshop123!\",\"client\":\"synthetic-client\"}")
                .when().post("/auth/login")
                .then()
                .statusCode(500)
                .body("exception", is(IllegalStateException.class.getName()))
                .body("message", is("Authentication token secret must contain at least 32 bytes"))
                .body("token", nullValue());
    }

    public static class ShortSecretProfile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of("AUTH_TOKEN_SECRET", "synthetic-short-key");
        }
    }
}