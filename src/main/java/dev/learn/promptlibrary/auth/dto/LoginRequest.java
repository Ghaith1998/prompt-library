package dev.learn.promptlibrary.auth.dto;

import jakarta.validation.constraints.NotBlank;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(name = "LoginRequest")
public record LoginRequest(
        @Schema(example = "alice") @NotBlank String username,
        @Schema(example = "alice") @NotBlank String password) {
}
