package dev.learn.promptlibrary.auth.dto;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

/** Minimal response returned after Keycloak creates the account. */
@Schema(name = "RegisterResponse")
public record RegisterResponse(
        @Schema(description = "Keycloak user id") String userId,
        @Schema(example = "carol") String username) {
}
