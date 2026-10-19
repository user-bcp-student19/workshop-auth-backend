package com.bcp.workshop.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LegacyTokenUtilTest {
    private static final String TEST_SECRET = "test-only-auth-token-secret-2026-9d4d8f5b2a6c7e1f";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Test
    void createsSignedTokenWithExplicitClaimsAndNoEmbeddedSecret() throws Exception {
        String token = LegacyTokenUtil.create("student", "STUDENT", TEST_SECRET);
        String[] parts = token.split("\\.");

        assertEquals(3, parts.length);
        assertFalse(token.contains(TEST_SECRET));
        assertEquals("HS256", decode(parts[0]).get("alg").asText());
        assertEquals("JWT", decode(parts[0]).get("typ").asText());

        JsonNode claims = decode(parts[1]);
        assertEquals("student", claims.get("sub").asText());
        assertEquals("STUDENT", claims.get("role").asText());
        assertEquals(LegacyTokenUtil.EXPIRATION_SECONDS,
                claims.get("exp").asLong() - claims.get("iat").asLong());
        assertEquals(expectedSignature(parts[0] + "." + parts[1], TEST_SECRET), parts[2]);
    }

    @Test
    void signingKeyChangesTokenSignature() {
        String firstToken = LegacyTokenUtil.create("student", "STUDENT", TEST_SECRET);
        String secondToken = LegacyTokenUtil.create("student", "STUDENT", TEST_SECRET + "-rotated");

        assertNotEquals(firstToken.split("\\.")[2], secondToken.split("\\.")[2]);
    }

    @Test
    void rejectsMissingOrWeakSigningKeys() {
        assertThrows(IllegalStateException.class,
                () -> LegacyTokenUtil.create("student", "STUDENT", "short"));
    }

    private JsonNode decode(String value) throws Exception {
        byte[] json = Base64.getUrlDecoder().decode(value);
        return OBJECT_MAPPER.readTree(json);
    }

    private String expectedSignature(String signingInput, String secret) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] signature = mac.doFinal(signingInput.getBytes(StandardCharsets.US_ASCII));
        return Base64.getUrlEncoder().withoutPadding().encodeToString(signature);
    }
}