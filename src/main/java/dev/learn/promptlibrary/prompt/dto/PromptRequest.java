package dev.learn.promptlibrary.prompt.dto;

import java.util.Set;

import dev.learn.promptlibrary.prompt.PromptStatus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

/*
 * The API contract lives here. SmallRye OpenAPI reads the Bean Validation annotations
 * (@NotBlank -> required, @Size -> maxLength ...) so you don't document the same rule twice.
 */
@Schema(name = "PromptRequest", description = "Payload to create or update a prompt")
public record PromptRequest(

        @Schema(description = "Short descriptive title", example = "Code review assistant")
        @NotBlank @Size(max = 200) String title,

        @Schema(description = "The prompt text", example = "You are a senior Java reviewer. Review the following code...")
        @NotBlank @Size(max = 20000) String content,

        @Schema(description = "Id of an existing category", example = "1")
        @NotNull @Positive Long categoryId,

        @Schema(description = "Lifecycle status, defaults to DRAFT on create", example = "DRAFT")
        PromptStatus status,

        @Schema(description = "Mark as favorite", example = "false")
        Boolean favorite,

        @Schema(description = "Tag names (created automatically, stored lowercase), max 20", example = "[\"java\", \"review\"]")
        @Size(max = 20) Set<@NotBlank @Size(max = 50) String> tags) {
}
