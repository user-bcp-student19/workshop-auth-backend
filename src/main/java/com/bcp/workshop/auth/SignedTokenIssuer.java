package com.bcp.workshop.auth;

import io.smallrye.jwt.algorithm.SignatureAlgorithm;
import io.smallrye.jwt.build.Jwt;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.Objects;
import java.util.Optional;

@ApplicationScoped
public class SignedTokenIssuer {
    public static final long TOKEN_TTL_SECONDS = 3600L;

    private final Optional<String> secret;
    private final Clock clock;

    @Inject
    public SignedTokenIssuer(@ConfigProperty(name = "AUTH_TOKEN_SECRET") Optional<String> secret) {
        this(secret, Clock.systemUTC());
    }

    public SignedTokenIssuer(String secret, Clock clock) {
        this(Optional.of(Objects.requireNonNull(secret)), clock);
    }

    SignedTokenIssuer(Optional<String> secret, Clock clock) {
        this.secret = Objects.requireNonNull(secret);
        this.clock = Objects.requireNonNull(clock);
    }

    public TokenEmission issue(String username, String client, String role) {
        String signingSecret = requireSecret();
        long issuedAt = clock.instant().getEpochSecond();
        long expiresAt = Math.addExact(issuedAt, TOKEN_TTL_SECONDS);
        String token = Jwt.claims()
                .subject(username)
                .claim("client", client)
                .claim("role", role)
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .jws()
                .algorithm(SignatureAlgorithm.HS256)
                .signWithSecret(signingSecret);

        return new TokenEmission(token, issuedAt, expiresAt, Math.toIntExact(expiresAt - issuedAt));
    }

    private String requireSecret() {
        String signingSecret = secret.orElseThrow(
                () -> new IllegalStateException("Authentication token secret is required"));
        if (signingSecret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("Authentication token secret must contain at least 32 bytes");
        }
        return signingSecret;
    }

    public record TokenEmission(String token, long issuedAt, long expiresAt, int expiresIn) {}
}