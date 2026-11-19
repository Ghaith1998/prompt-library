package dev.learn.promptlibrary.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

/** Validated self-registration data sent to the Keycloak Admin API. */
@Schema(name = "RegisterRequest", description = "Self-registration payload")
public record RegisterRequest(
        @Schema(example = "carol") @NotBlank @Size(min = 3, max = 50) String username,
        @Schema(example = "carol@example.com") @NotBlank @Email String email,
        @Schema(example = "S3cret-pass!") @NotBlank @Size(min = 8, max = 100) String password,
        // Keycloak 24+ treats accounts without first/last name as "not fully set up" and refuses password logins
        @Schema(example = "Carol") @NotBlank @Size(max = 100) String firstName,
        @Schema(example = "Coder") @NotBlank @Size(max = 100) String lastName) {
}
