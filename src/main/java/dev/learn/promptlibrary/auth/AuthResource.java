package dev.learn.promptlibrary.auth;

import dev.learn.promptlibrary.auth.dto.LoginRequest;
import dev.learn.promptlibrary.auth.dto.LoginResponse;
import dev.learn.promptlibrary.auth.dto.RegisterRequest;
import dev.learn.promptlibrary.auth.dto.RegisterResponse;

import jakarta.annotation.security.PermitAll;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.jboss.resteasy.reactive.RestResponse;

/**
 * Public endpoints (@PermitAll) that talk to Keycloak on behalf of the caller.
 * Fine for learning/scripts. For a real frontend prefer Authorization Code + PKCE straight against Keycloak.
 */
@Path("/api/auth")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@org.eclipse.microprofile.openapi.annotations.tags.Tag(name = "Auth", description = "Registration, login and identity")
public class AuthResource {

    private final KeycloakService keycloak;

    public AuthResource(KeycloakService keycloak) {
        this.keycloak = keycloak;
    }

    @POST
    @Path("/register")
    @PermitAll
    @Operation(summary = "Self-register", description = "Creates the user in Keycloak via the Admin API.")
    @APIResponse(responseCode = "201", description = "User created")
    @APIResponse(responseCode = "400", description = "Validation failed")
    @APIResponse(responseCode = "409", description = "Username or e-mail already registered")
    @APIResponse(responseCode = "502", description = "Keycloak unreachable or misconfigured")
    public RestResponse<RegisterResponse> register(@Valid RegisterRequest request) {
        String userId = keycloak.register(request);
        return RestResponse.status(jakarta.ws.rs.core.Response.Status.CREATED,
                new RegisterResponse(userId, request.username()));
    }

    @POST
    @Path("/login")
    @PermitAll
    @Operation(summary = "Log in", description = "Password grant against Keycloak; returns the tokens.")
    @APIResponse(responseCode = "200", description = "Tokens")
    @APIResponse(responseCode = "401", description = "Wrong credentials")
    public LoginResponse login(@Valid LoginRequest request) {
        return keycloak.login(request.username(), request.password());
    }
}
