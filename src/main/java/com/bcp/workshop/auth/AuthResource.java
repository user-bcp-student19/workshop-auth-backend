package com.bcp.workshop.auth;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.commons.text.StringEscapeUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Path("/auth")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class AuthResource {
    @Inject
    AuthenticationService authenticationService;

    @POST
    @Path("/login")
    public Response login(LoginRequest request) {
        try {
            if (request == null) {
                return Response.status(400).entity(Map.of("message", "Request is null")).build();
            } else {
                if (request.username == null || request.username.trim().isEmpty()) {
                    return Response.status(400).entity(Map.of("message", "Username is required")).build();
                } else {
                    if (request.password == null || request.password.trim().isEmpty()) {
                        return Response.status(400).entity(Map.of("message", "Password is required")).build();
                    } else {
                        if (request.client == null || request.client.trim().isEmpty()) {
                            return Response.status(400).entity(Map.of("message", "Client is required")).build();
                        }
                        Optional<AuthenticationService.LoginResult> result = authenticationService.authenticate(
                            request.username, request.password, request.client);
                        if (result.isEmpty()) {
                            return Response.status(401).entity(Map.of("message", "Invalid user " + request.username + " or password")).build();
                        } else {
                            AuthenticationService.LoginResult login = result.get();
                            Map<String, Object> response = new HashMap<>();
                            response.put("token", login.token());
                            response.put("expiresIn", login.expiresIn());
                            response.put("user", Map.of(
                                "username", login.username(),
                                "displayName", StringEscapeUtils.escapeHtml4(login.displayName()),
                                "role", login.role()
                            ));
                            response.put("debug", "authenticated-with-legacy-flow");
                            return Response.ok(response).build();
                        }
                    }
                }
            }
        } catch (Exception error) {
            return Response.status(500).entity(Map.of(
                    "message", error.getMessage() == null ? "Unknown error" : error.getMessage(),
                    "exception", error.getClass().getName()
            )).build();
        }
    }
}
