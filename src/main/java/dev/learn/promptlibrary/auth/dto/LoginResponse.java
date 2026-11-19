package dev.learn.promptlibrary.auth.dto;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(name = "LoginResponse", description = "Use accessToken as: Authorization: Bearer <accessToken>")
public record LoginResponse(
        String accessToken,
        String refreshToken,
        @Schema(description = "Lifetime of the access token in seconds", example = "1800") long expiresIn,
        @Schema(example = "Bearer") String tokenType) {
}
