package dev.learn.promptlibrary.category.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(name = "CategoryRequest", description = "Payload to create a category")
public record CategoryRequest(
        @Schema(description = "Unique category name", example = "Coding")
        @NotBlank @Size(max = 100) String name) {
}
