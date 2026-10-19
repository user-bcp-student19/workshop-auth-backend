package com.bcp.workshop.auth;

import jakarta.enterprise.context.ApplicationScoped;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

@ApplicationScoped
public class PasswordHasher {
    private static final String FORMAT = "pbkdf2-sha256";
    private static final int ITERATIONS = 600_000;
    private static final int SALT_LENGTH_BYTES = 16;
    private static final int HASH_LENGTH_BITS = 256;
    private static final int MAX_ACCEPTED_ITERATIONS = 2_000_000;

    private final SecureRandom secureRandom = new SecureRandom();

    public String hash(String password) {
        if (password == null) {
            throw new IllegalArgumentException("Password is required");
        }

        byte[] salt = new byte[SALT_LENGTH_BYTES];
        secureRandom.nextBytes(salt);
        byte[] derivedKey = deriveKey(password, salt, ITERATIONS);
        return FORMAT + "$" + ITERATIONS + "$"
                + Base64.getEncoder().encodeToString(salt) + "$"
                + Base64.getEncoder().encodeToString(derivedKey);
    }

    public boolean verify(String password, String encodedHash) {
        if (password == null || encodedHash == null) {
            return false;
        }

        String[] components = encodedHash.split("\\$", -1);
        if (components.length != 4 || !FORMAT.equals(components[0])) {
            return false;
        }

        try {
            int iterations = Integer.parseInt(components[1]);
            if (iterations < 1 || iterations > MAX_ACCEPTED_ITERATIONS) {
                return false;
            }
            byte[] salt = Base64.getDecoder().decode(components[2]);
            byte[] expectedHash = Base64.getDecoder().decode(components[3]);
            if (salt.length != SALT_LENGTH_BYTES || expectedHash.length != HASH_LENGTH_BITS / Byte.SIZE) {
                return false;
            }

            byte[] actualHash = deriveKey(password, salt, iterations);
            return MessageDigest.isEqual(expectedHash, actualHash);
        } catch (IllegalArgumentException error) {
            return false;
        }
    }

    private byte[] deriveKey(String password, byte[] salt, int iterations) {
        PBEKeySpec keySpec = new PBEKeySpec(password.toCharArray(), salt, iterations, HASH_LENGTH_BITS);
        try {
            SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            return keyFactory.generateSecret(keySpec).getEncoded();
        } catch (GeneralSecurityException error) {
            throw new IllegalStateException("Password hashing is unavailable", error);
        } finally {
            keySpec.clearPassword();
        }
    }
}