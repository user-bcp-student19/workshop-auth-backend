package com.bcp.workshop.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.notNullValue;

@QuarkusTest
@TestProfile(SignedTokenIssuerConfigurationTest.SyntheticSecretProfile.class)
class AuthResourceTest {
    private static final ObjectMapper JSON = new ObjectMapper();

    @Test
    void logsInWithWorkshopUser() {
        var response = given()
                .contentType("application/json")
                .body("{\"username\":\"student\",\"password\":\"Workshop123!\",\"client\":\"angular-workshop\"}")
                .when().post("/auth/login")
                .then()
                .statusCode(200)
            .contentType("application/json")
                .body("user.username", is("student"))
            .body("user.displayName", is("Workshop Student"))
            .body("user.role", is("STUDENT"))
                .body("token", notNullValue())
            .body("expiresIn", is(3600))
            .body("debug", is("authenticated-with-legacy-flow"))
            .extract().response();

        String token = response.jsonPath().getString("token");
        String[] segments = token.split("\\.", -1);
        JsonNode header = decodeJwtSegment(segments[0]);
        JsonNode claims = decodeJwtSegment(segments[1]);

        assertEquals(3, segments.length);
        assertEquals("HS256", header.path("alg").asText());
        assertEquals("student", claims.path("sub").asText());
        assertEquals("angular-workshop", claims.path("client").asText());
        assertEquals("STUDENT", claims.path("role").asText());
        assertEquals(3600L, claims.path("exp").asLong() - claims.path("iat").asLong());
        assertEquals(3600, response.jsonPath().getInt("expiresIn"));
        assertNull(response.jsonPath().get("client"));
    }

        @Test
        void rejectsNullAndIncompleteCredentialsWithDocumentedMessages() {
        given()
            .contentType("application/json")
            .body("null")
            .when().post("/auth/login")
            .then()
            .statusCode(400)
            .body("message", is("Request is null"));

        given()
            .contentType("application/json")
            .body("{}")
            .when().post("/auth/login")
            .then()
            .statusCode(400)
            .body("message", is("Username is required"));

        given()
            .contentType("application/json")
            .body("{\"username\":\"  \",\"password\":\"Workshop123!\"}")
            .when().post("/auth/login")
            .then()
            .statusCode(400)
            .body("message", is("Username is required"));

        given()
            .contentType("application/json")
            .body("{\"username\":\"student\"}")
            .when().post("/auth/login")
            .then()
            .statusCode(400)
            .body("message", is("Password is required"));

        given()
            .contentType("application/json")
            .body("{\"username\":\"student\",\"password\":\"  \"}")
            .when().post("/auth/login")
            .then()
            .statusCode(400)
            .body("message", is("Password is required"));
        }

        @Test
        void rejectsNullCredentialFieldsWithDocumentedMessages() {
        given()
            .contentType("application/json")
            .body("{\"username\":null,\"password\":\"Workshop123!\"}")
            .when().post("/auth/login")
            .then()
            .statusCode(400)
            .body("message", is("Username is required"));

        given()
            .contentType("application/json")
            .body("{\"username\":\"student\",\"password\":null}")
            .when().post("/auth/login")
            .then()
            .statusCode(400)
            .body("message", is("Password is required"));
        }

        @Test
        void rejectsInvalidCredentialsWithDocumentedMessage() {
        given()
            .contentType("application/json")
                .body("{\"username\":\"student\",\"password\":\"wrong\",\"client\":\"test-client\"}")
            .when().post("/auth/login")
            .then()
            .statusCode(401)
            .body("message", is("Invalid user student or password"));

        given()
            .contentType("application/json")
                .body("{\"username\":\"unknown-user\",\"password\":\"wrong\",\"client\":\"test-client\"}")
            .when().post("/auth/login")
            .then()
            .statusCode(401)
            .body("message", is("Invalid user unknown-user or password"));
        }

        @Test
        void usesOriginalCredentialValuesInsteadOfTrimmingThem() {
        given()
            .contentType("application/json")
                .body("{\"username\":\" student \",\"password\":\"Workshop123!\",\"client\":\"test-client\"}")
            .when().post("/auth/login")
            .then()
            .statusCode(401)
            .body("message", is("Invalid user  student  or password"));

        given()
            .contentType("application/json")
                .body("{\"username\":\"student\",\"password\":\" Workshop123! \",\"client\":\"test-client\"}")
            .when().post("/auth/login")
            .then()
            .statusCode(401)
            .body("message", is("Invalid user student or password"));
        }

        @Test
        void preservesDocumentedProtocolErrorStatusesAndEmptyBodies() {
        given()
            .contentType("application/json")
            .body("{")
            .when().post("/auth/login")
            .then()
            .statusCode(400)
            .body(is(""));

        given()
            .accept("application/json")
            .when().get("/auth/login")
            .then()
            .statusCode(405)
            .body(is(""));

        given()
            .accept("text/plain")
            .contentType("application/json")
            .body("{\"username\":\"student\",\"password\":\"Workshop123!\"}")
            .when().post("/auth/login")
            .then()
            .statusCode(406)
            .body(is(""));

        given()
            .contentType("text/plain")
            .body("{\"username\":\"student\",\"password\":\"Workshop123!\"}")
            .when().post("/auth/login")
            .then()
            .statusCode(415)
            .body(is(""));
        }

        @Test
        void reportsHealthWithoutAuthentication() {
        given()
            .accept("application/json")
            .when().get("/q/health")
            .then()
            .statusCode(200)
            .contentType("application/json")
            .body("status", is("UP"))
            .body("checks.size()", is(0));
        }

        @Test
        void rejectsMissingClientWithApprovedMessage() {
            given()
                    .contentType("application/json")
                    .body("{\"username\":\"student\",\"password\":\"Workshop123!\"}")
                    .when().post("/auth/login")
                    .then()
                    .statusCode(400)
                    .body("message", is("Client is required"));
        }

        @Test
        void rejectsNullEmptyAndBlankClient() {
            String[] requests = {
                    "{\"username\":\"student\",\"password\":\"Workshop123!\",\"client\":null}",
                    "{\"username\":\"student\",\"password\":\"Workshop123!\",\"client\":\"\"}",
                    "{\"username\":\"student\",\"password\":\"Workshop123!\",\"client\":\"  \"}"
            };

            for (String request : requests) {
                given()
                        .contentType("application/json")
                        .body(request)
                        .when().post("/auth/login")
                        .then()
                        .statusCode(400)
                        .body("message", is("Client is required"));
            }
        }

        @Test
        void preservesFrameworkDeserializationErrorForNonStringClient() {
            given()
                    .contentType("application/json")
                    .body("{\"username\":\"student\",\"password\":\"Workshop123!\",\"client\":{}}")
                    .when().post("/auth/login")
                    .then()
                    .statusCode(400);
        }

        @Test
        void validatesUsernameAndPasswordBeforeClient() {
            given()
                    .contentType("application/json")
                    .body("{\"username\":\"  \",\"password\":\"Workshop123!\",\"client\":\"  \"}")
                    .when().post("/auth/login")
                    .then()
                    .statusCode(400)
                    .body("message", is("Username is required"));

            given()
                    .contentType("application/json")
                    .body("{\"username\":\"student\",\"password\":\"  \",\"client\":\"  \"}")
                    .when().post("/auth/login")
                    .then()
                    .statusCode(400)
                    .body("message", is("Password is required"));
        }

        @Test
        void rejectsInvalidClientBeforeCallingAuthenticationService() {
            AuthResource resource = new AuthResource();
            resource.authenticationService = new AuthenticationService() {
                @Override
                public Optional<LoginResult> authenticate(String username, String password, String client) {
                    throw new AssertionError("Authentication must not run for an invalid client");
                }
            };
            LoginRequest request = new LoginRequest();
            request.username = "student";
            request.password = "Workshop123!";
            request.client = "  ";

            var response = resource.login(request);
            Map<?, ?> responseBody = (Map<?, ?>) response.getEntity();

            assertEquals(400, response.getStatus());
            assertEquals("Client is required", responseBody.get("message"));
        }

        @Test
        void preservesDistinctAcceptedClientsInSignedTokens() {
            JsonNode firstClaims = loginClaims(" first-client ");
            JsonNode secondClaims = loginClaims("second-client");

            assertEquals(" first-client ", firstClaims.path("client").asText());
            assertEquals("second-client", secondClaims.path("client").asText());
            assertEquals("student", firstClaims.path("sub").asText());
            assertEquals("student", secondClaims.path("sub").asText());
            assertNotEquals(firstClaims.path("client").asText(), secondClaims.path("client").asText());
        }

            @Test
            void observesExpiresInConsistentWithJwtExpirationWithinTwoSeconds() {
            long requestStarted = System.nanoTime();
            var response = given()
                .contentType("application/json")
                .body("{\"username\":\"student\",\"password\":\"Workshop123!\",\"client\":\"timing-client\"}")
                .when().post("/auth/login")
                .then()
                .statusCode(200)
                .extract().response();
            long observedAt = Instant.now().getEpochSecond();
            long observationNanos = System.nanoTime() - requestStarted;

            String[] segments = response.jsonPath().getString("token").split("\\.", -1);
            JsonNode claims = decodeJwtSegment(segments[1]);
            long issuedAt = claims.path("iat").asLong();
            long expiresAt = claims.path("exp").asLong();
            int expiresIn = response.jsonPath().getInt("expiresIn");
            long difference = Math.abs(expiresIn - (expiresAt - observedAt));

            System.out.printf("JWT time evidence: iat=%d exp=%d now=%d expiresIn=%d difference=%d budget=2s%n",
                issuedAt, expiresAt, observedAt, expiresIn, difference);
            assertEquals(3600L, expiresAt - issuedAt);
            assertEquals(3600, expiresIn);
            assertTrue(observationNanos <= 2_000_000_000L,
                "Observation exceeded the approved two-second budget");
            assertTrue(difference <= 2, "expiresIn differs from exp-now by more than two seconds");
            }

        private JsonNode loginClaims(String client) {
            String token = given()
                    .contentType("application/json")
                    .body("{\"username\":\"student\",\"password\":\"Workshop123!\",\"client\":\"" + client + "\"}")
                    .when().post("/auth/login")
                    .then()
                    .statusCode(200)
                    .extract().path("token");

            return decodeJwtSegment(token.split("\\.", -1)[1]);
        }

        private static JsonNode decodeJwtSegment(String segment) {
            try {
                return JSON.readTree(Base64.getUrlDecoder().decode(segment));
            } catch (Exception error) {
                throw new AssertionError("JWT segment must be valid JSON", error);
            }
        }

        @Test
        void escapesDisplayNameWhenBuildingSuccessfulResponse() {
            AuthResource resource = new AuthResource();
            resource.authenticationService = new AuthenticationService() {
                @Override
                public Optional<LoginResult> authenticate(String username, String password, String client) {
                    return Optional.of(new LoginResult(
                            username, "<b>Student</b>", "STUDENT", "synthetic-token", 3600));
                }
            };
            LoginRequest request = new LoginRequest();
            request.username = "test-user";
            request.password = "synthetic-password";
            request.client = "test-client";

            var response = resource.login(request);
            Map<?, ?> responseBody = (Map<?, ?>) response.getEntity();
            Map<?, ?> user = (Map<?, ?>) responseBody.get("user");

            assertEquals(200, response.getStatus());
            assertEquals("&lt;b&gt;Student&lt;/b&gt;", user.get("displayName"));
        }

        @Test
        void mapsControlledAuthenticationExceptionToDocumented500() {
            AuthResource resource = new AuthResource();
            resource.authenticationService = new AuthenticationService() {
                @Override
                public Optional<LoginResult> authenticate(String username, String password, String client) {
                    throw new IllegalStateException("synthetic failure");
                }
            };
            LoginRequest request = new LoginRequest();
            request.username = "test-user";
            request.password = "synthetic-password";
            request.client = "test-client";

            var response = resource.login(request);
            Map<?, ?> responseBody = (Map<?, ?>) response.getEntity();

            assertEquals(500, response.getStatus());
            assertEquals("synthetic failure", responseBody.get("message"));
            assertEquals(IllegalStateException.class.getName(), responseBody.get("exception"));
            assertFalse(responseBody.containsKey("token"));
        }

        @Test
        void mapsMissingDatabaseResultToUnauthorizedWithoutToken() {
            AuthResource resource = new AuthResource();
            resource.authenticationService = new AuthenticationService() {
                @Override
                public Optional<LoginResult> authenticate(String username, String password, String client) {
                    return Optional.empty();
                }
            };
            LoginRequest request = new LoginRequest();
            request.username = "unknown-user";
            request.password = "synthetic-password";
            request.client = "test-client";

            var response = resource.login(request);
            Map<?, ?> responseBody = (Map<?, ?>) response.getEntity();

            assertEquals(401, response.getStatus());
            assertEquals("Invalid user unknown-user or password", responseBody.get("message"));
            assertFalse(responseBody.containsKey("token"));
        }

        @Test
        void recordsPermissiveCorsAsKnownBaselineDeviation() {
        assertPreflightAccepted("/auth/login", "POST");
        assertPreflightAccepted("/q/health", "GET");
        }

        private void assertPreflightAccepted(String path, String requestedMethod) {
        given()
            .header("Origin", "https://unconfigured.example")
            .header("Access-Control-Request-Method", requestedMethod)
            .when().options(path)
            .then()
            .statusCode(200)
            .header("Access-Control-Allow-Origin", notNullValue());
        }
}
