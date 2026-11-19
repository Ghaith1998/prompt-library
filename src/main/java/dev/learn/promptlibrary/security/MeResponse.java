package dev.learn.promptlibrary.security;

import java.util.Set;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(name = "MeResponse", description = "Who am I, according to my token")
public record MeResponse(
        @Schema(description = "Keycloak user id (sub)") String id,
        @Schema(example = "alice") String username,
        @Schema(example = "alice@example.com") String email,
        @Schema(example = "[\"user\", \"admin\"]") Set<String> roles) {
}
