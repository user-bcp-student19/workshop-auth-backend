package com.bcp.workshop.auth;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordHasherTest {
    private final PasswordHasher passwordHasher = new PasswordHasher();

    @Test
    void createsDifferentSaltedHashesForSamePassword() {
        String firstHash = passwordHasher.hash("Correct horse battery staple");
        String secondHash = passwordHasher.hash("Correct horse battery staple");

        assertNotEquals(firstHash, secondHash);
        assertTrue(firstHash.startsWith("pbkdf2-sha256$"));
    }

    @Test
    void verifiesOnlyTheMatchingPassword() {
        String encodedHash = passwordHasher.hash("correct-password");

        assertTrue(passwordHasher.verify("correct-password", encodedHash));
        assertFalse(passwordHasher.verify("incorrect-password", encodedHash));
    }

    @Test
    void rejectsMalformedHashesAndNullPasswords() {
        assertFalse(passwordHasher.verify("password", "not-a-password-hash"));
        assertFalse(passwordHasher.verify("password", "pbkdf2-sha256$999999999$bad$bad"));
        assertFalse(passwordHasher.verify(null, "pbkdf2-sha256$600000$bad$bad"));
        assertThrows(IllegalArgumentException.class, () -> passwordHasher.hash(null));
    }
}