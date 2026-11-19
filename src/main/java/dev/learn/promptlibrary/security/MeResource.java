package dev.learn.promptlibrary.security;

import io.quarkus.security.Authenticated;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.eclipse.microprofile.openapi.annotations.Operation;

@Path("/api/me")
@Produces(MediaType.APPLICATION_JSON)
@Authenticated
@org.eclipse.microprofile.openapi.annotations.tags.Tag(name = "Auth", description = "Registration, login and identity")
public class MeResource {

    private final CurrentUser currentUser;

    public MeResource(CurrentUser currentUser) {
        this.currentUser = currentUser;
    }

    @GET
    @Operation(summary = "Current user", description = "Data comes straight from the access token; no DB lookup.")
    public MeResponse me() {
        return new MeResponse(
                currentUser.id(),
                currentUser.username(),
                currentUser.email().orElse(null),
                currentUser.roles());
    }
}
