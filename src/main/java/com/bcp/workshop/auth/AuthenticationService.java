package com.bcp.workshop.auth;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.Optional;

@ApplicationScoped
public class AuthenticationService {
    @Inject
    LegacyDatabase database;

    @Inject
    SignedTokenIssuer tokenIssuer;

    public Optional<LoginResult> authenticate(String username, String password, String client) {
        UserRecord user = database.findByCredentials(username, password);
        if (user == null) {
            return Optional.empty();
        }

        SignedTokenIssuer.TokenEmission token = tokenIssuer.issue(user.username(), client, user.role());
        return Optional.of(new LoginResult(
                user.username(),
                user.displayName(),
                user.role(),
            token.token(),
            token.expiresIn()
        ));
    }

    public record LoginResult(String username, String displayName, String role, String token, int expiresIn) {}
}