package com.bcp.workshop.auth;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@QuarkusTest
@TestProfile(SignedTokenIssuerConfigurationTest.SyntheticSecretProfile.class)
class SignedTokenIssuerConfigurationTest {
    @Inject
    SignedTokenIssuer tokenIssuer;

    @Test
    void signsWithExplicitTestConfigurationWithoutEmbeddingTheSecret() {
        SignedTokenIssuer.TokenEmission emission = tokenIssuer.issue("student", "synthetic-client", "STUDENT");

        assertNotNull(emission.token());
        assertFalse(emission.token().contains("synthetic-test-secret-with-at-least-32-bytes"));
    }

    public static class SyntheticSecretProfile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of("AUTH_TOKEN_SECRET", "synthetic-test-secret-with-at-least-32-bytes");
        }
    }
}