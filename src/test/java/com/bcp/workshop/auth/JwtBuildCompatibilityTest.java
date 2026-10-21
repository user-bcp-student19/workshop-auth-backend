package com.bcp.workshop.auth;

import io.smallrye.jwt.algorithm.SignatureAlgorithm;
import io.smallrye.jwt.build.Jwt;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtBuildCompatibilityTest {
    @Test
    void buildsCompactHs256TokenWithExplicitSyntheticSecret() {
        String secret = "synthetic-test-secret-with-at-least-32-bytes";
        String token = Jwt.claims()
                .subject("synthetic-subject")
                .claim("client", "synthetic-client")
                .claim("role", "STUDENT")
                .issuedAt(1_000L)
                .expiresAt(4_600L)
                .jws()
                .algorithm(SignatureAlgorithm.HS256)
                .signWithSecret(secret);

        String[] segments = token.split("\\.", -1);
        String header = new String(Base64.getUrlDecoder().decode(segments[0]), StandardCharsets.UTF_8);

        assertEquals(3, segments.length);
        assertTrue(header.contains("\"alg\":\"HS256\""));
    }
}