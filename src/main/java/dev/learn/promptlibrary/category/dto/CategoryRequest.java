package dev.learn.promptlibrary.category.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

/** Shared create/update payload for a globally named category. */
@Schema(name = "CategoryRequest", description = "Payload to create or update a category")
public record CategoryRequest(
        @Schema(description = "Unique category name", example = "Coding")
        @NotBlank @Size(max = 100) String name) {
}
