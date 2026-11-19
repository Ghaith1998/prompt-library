package dev.learn.promptlibrary.common;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(name = "ApiError", description = "Uniform error body")
public record ApiError(
        @Schema(description = "Machine readable code", example = "NOT_FOUND") String code,
        @Schema(description = "Human readable message", example = "Prompt 42 not found") String message) {
}
