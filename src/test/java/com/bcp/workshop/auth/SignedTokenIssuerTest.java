package com.bcp.workshop.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jose4j.jca.ProviderContext;
import org.jose4j.jwt.NumericDate;
import org.jose4j.jwt.consumer.InvalidJwtException;
import org.jose4j.jwt.consumer.JwtConsumer;
import org.jose4j.jwt.consumer.JwtConsumerBuilder;
import org.jose4j.jwa.AlgorithmConstraints;
import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Base64;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SignedTokenIssuerTest {
    private static final String TEST_SECRET = "synthetic-test-secret-with-at-least-32-bytes";
    private static final ObjectMapper JSON = new ObjectMapper();

    @Test
    void issuesHs256TokenFromOneClockReadingWithExplicitClaims() throws Exception {
        Instant now = Instant.parse("2026-10-21T12:00:00Z");
        AtomicInteger clockReads = new AtomicInteger();
        Clock clock = new Clock() {
            @Override
            public ZoneId getZone() {
                return ZoneId.of("UTC");
            }

            @Override
            public Clock withZone(ZoneId zone) {
                return this;
            }

            @Override
            public Instant instant() {
                clockReads.incrementAndGet();
                return now;
            }
        };

        SignedTokenIssuer.TokenEmission emission = new SignedTokenIssuer(TEST_SECRET, clock)
                .issue("student", "angular-workshop", "STUDENT");
        String[] segments = emission.token().split("\\.", -1);
        JsonNode header = decodeJson(segments[0]);
        JsonNode claims = decodeJson(segments[1]);

        assertEquals(1, clockReads.get());
        assertEquals(3, segments.length);
        assertEquals("HS256", header.path("alg").asText());
        assertEquals("student", claims.path("sub").asText());
        assertEquals("angular-workshop", claims.path("client").asText());
        assertEquals("STUDENT", claims.path("role").asText());
        assertEquals(now.getEpochSecond(), claims.path("iat").asLong());
        assertEquals(now.getEpochSecond() + SignedTokenIssuer.TOKEN_TTL_SECONDS, claims.path("exp").asLong());
        assertEquals(3600L, emission.expiresAt() - emission.issuedAt());
        assertEquals(3600, emission.expiresIn());
        assertFalse(emission.token().contains(TEST_SECRET));
        assertTrue(hasValidSignature(segments));
    }

    @Test
    void distinctClientsProduceDistinctTokensForTheSameIdentity() throws Exception {
        Clock clock = Clock.fixed(Instant.parse("2026-10-21T12:00:00Z"), ZoneId.of("UTC"));
        SignedTokenIssuer issuer = new SignedTokenIssuer(TEST_SECRET, clock);

        SignedTokenIssuer.TokenEmission first = issuer.issue("student", "client-one", "STUDENT");
        SignedTokenIssuer.TokenEmission second = issuer.issue("student", "client-two", "STUDENT");
        JsonNode firstClaims = decodeJson(first.token().split("\\.", -1)[1]);
        JsonNode secondClaims = decodeJson(second.token().split("\\.", -1)[1]);

        assertEquals(firstClaims.path("sub").asText(), secondClaims.path("sub").asText());
        assertEquals("client-one", firstClaims.path("client").asText());
        assertEquals("client-two", secondClaims.path("client").asText());
        assertNotEquals(first.token(), second.token());
        assertTrue(hasValidSignature(first.token().split("\\.", -1)));
        assertTrue(hasValidSignature(second.token().split("\\.", -1)));
    }

    @Test
    void alteredJwtPayloadDoesNotMatchTheHmacSignature() throws Exception {
        Clock clock = Clock.fixed(Instant.parse("2026-10-21T12:00:00Z"), ZoneId.of("UTC"));
        String[] segments = new SignedTokenIssuer(TEST_SECRET, clock)
                .issue("student", "angular-workshop", "STUDENT")
                .token().split("\\.", -1);
        byte[] alteredPayload = Base64.getUrlDecoder().decode(segments[1]);
        alteredPayload[0] ^= 1;
        String[] alteredSegments = {
                segments[0],
                Base64.getUrlEncoder().withoutPadding().encodeToString(alteredPayload),
                segments[2]
        };

        assertTrue(hasValidSignature(segments));
        assertFalse(hasValidSignature(alteredSegments));
    }

    @Test
    void tokenExpiresAtTheExactExpBoundaryWithoutClockSkew() throws Exception {
        Clock clock = Clock.fixed(Instant.parse("2026-10-21T12:00:00Z"), ZoneId.of("UTC"));
        String token = new SignedTokenIssuer(TEST_SECRET, clock)
                .issue("student", "angular-workshop", "STUDENT")
                .token();
        String[] segments = token.split("\\.", -1);
        long expiration = decodeJson(segments[1]).path("exp").asLong();

        JwtConsumer validBeforeExpiration = jwtConsumerAt(expiration - 1);
        JwtConsumer expiredAtBoundary = jwtConsumerAt(expiration);
        JwtConsumer expiredAfterBoundary = jwtConsumerAt(expiration + 1);

        assertDoesNotThrow(() -> validBeforeExpiration.processToClaims(token));
        assertThrows(InvalidJwtException.class, () -> expiredAtBoundary.processToClaims(token));
        assertThrows(InvalidJwtException.class, () -> expiredAfterBoundary.processToClaims(token));
    }

    @Test
    void rejectsMissingOrShortSigningSecretWithoutEchoingIt() {
        Clock clock = Clock.fixed(Instant.parse("2026-10-21T12:00:00Z"), ZoneId.of("UTC"));
        IllegalStateException missingSecret = assertThrows(
                IllegalStateException.class,
                () -> new SignedTokenIssuer(Optional.empty(), clock).issue("student", "client", "STUDENT"));
        String shortSecret = "synthetic-short-key";
        IllegalStateException shortKey = assertThrows(
                IllegalStateException.class,
                () -> new SignedTokenIssuer(shortSecret, clock).issue("student", "client", "STUDENT"));

        assertEquals("Authentication token secret is required", missingSecret.getMessage());
        assertEquals("Authentication token secret must contain at least 32 bytes", shortKey.getMessage());
        assertFalse(missingSecret.getMessage().contains(shortSecret));
        assertFalse(shortKey.getMessage().contains(shortSecret));
    }

    private static JsonNode decodeJson(String segment) throws Exception {
        byte[] json = Base64.getUrlDecoder().decode(segment);
        return JSON.readTree(json);
    }

    private static boolean hasValidSignature(String[] segments) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(TEST_SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] signingInput = (segments[0] + "." + segments[1]).getBytes(StandardCharsets.US_ASCII);
        byte[] expectedSignature = mac.doFinal(signingInput);
        byte[] actualSignature = Base64.getUrlDecoder().decode(segments[2]);
        return MessageDigest.isEqual(expectedSignature, actualSignature);
    }

    private static JwtConsumer jwtConsumerAt(long now) {
        return new JwtConsumerBuilder()
                .setRequireExpirationTime()
                .setAllowedClockSkewInSeconds(0)
                .setEvaluationTime(NumericDate.fromSeconds(now))
                .setVerificationKey(new SecretKeySpec(TEST_SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"))
                .setJwsAlgorithmConstraints(AlgorithmConstraints.ConstraintType.PERMIT, "HS256")
                .build();
    }
}