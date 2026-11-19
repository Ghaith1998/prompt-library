package dev.learn.promptlibrary.auth.dto;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(name = "RegisterResponse")
public record RegisterResponse(
        @Schema(description = "Keycloak user id") String userId,
        @Schema(example = "carol") String username) {
}
