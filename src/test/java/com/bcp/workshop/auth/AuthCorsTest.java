package com.bcp.workshop.auth;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.nullValue;

@QuarkusTest
class AuthCorsTest {
    @Test
    void allowsConfiguredOriginForLoginPreflight() {
        given()
                .header("Origin", "http://localhost:4200")
                .header("Access-Control-Request-Method", "POST")
                .when().options("/auth/login")
                .then()
                .statusCode(200)
                .header("Access-Control-Allow-Origin", is("http://localhost:4200"));
    }

    @Test
    void rejectsUnconfiguredOriginForLoginPreflight() {
        given()
                .header("Origin", "https://untrusted.example")
                .header("Access-Control-Request-Method", "POST")
                .when().options("/auth/login")
                .then()
                .statusCode(403)
                .header("Access-Control-Allow-Origin", nullValue())
                .body(is(""));
    }

    @Test
    void rejectsUnconfiguredOriginForHealthPreflight() {
        given()
                .header("Origin", "https://untrusted.example")
                .header("Access-Control-Request-Method", "GET")
                .when().options("/q/health")
                .then()
                .statusCode(403)
                .header("Access-Control-Allow-Origin", nullValue())
                .body(is(""));
    }
}