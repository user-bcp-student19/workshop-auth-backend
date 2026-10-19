package com.bcp.workshop.auth;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.microprofile.config.ConfigProvider;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public final class LegacyTokenUtil {
    public static final int EXPIRATION_SECONDS = 3600;

    private static final String SECRET_CONFIG_KEY = "auth.token.secret";
    private static final String SIGNATURE_ALGORITHM = "HmacSHA256";
    private static final int MINIMUM_SECRET_LENGTH_BYTES = 32;
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private LegacyTokenUtil() {}

    public static String create(String username, String role) {
        String secret = ConfigProvider.getConfig()
                .getOptionalValue(SECRET_CONFIG_KEY, String.class)
                .orElseThrow(() -> new IllegalStateException("Authentication token secret is not configured"));
        return create(username, role, secret);
    }

    static String create(String username, String role, String secret) {
        validateSecret(secret);
        long issuedAt = Instant.now().getEpochSecond();
        long expiresAt = issuedAt + EXPIRATION_SECONDS;
        try {
            String header = encodeJson(Map.of("alg", "HS256", "typ", "JWT"));
            String payload = encodeJson(Map.of(
                    "sub", username,
                    "role", role,
                    "iat", issuedAt,
                    "exp", expiresAt
            ));
            String signingInput = header + "." + payload;
            String signature = Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(sign(signingInput, secret));
            return signingInput + "." + signature;
        } catch (JsonProcessingException | GeneralSecurityException error) {
            throw new IllegalStateException("Unable to create authentication token", error);
        }
    }

    private static String encodeJson(Map<String, ?> values) throws JsonProcessingException {
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(OBJECT_MAPPER.writeValueAsBytes(values));
    }

    private static byte[] sign(String signingInput, String secret) throws GeneralSecurityException {
        Mac mac = Mac.getInstance(SIGNATURE_ALGORITHM);
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), SIGNATURE_ALGORITHM));
        return mac.doFinal(signingInput.getBytes(StandardCharsets.US_ASCII));
    }

    private static void validateSecret(String secret) {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < MINIMUM_SECRET_LENGTH_BYTES) {
            throw new IllegalStateException("Authentication token secret must contain at least 32 bytes");
        }
    }
}
