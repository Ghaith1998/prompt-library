package dev.learn.promptlibrary.category.dto;

import dev.learn.promptlibrary.category.Category;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(name = "CategoryResponse")
public record CategoryResponse(
        @Schema(example = "1") Long id,
        @Schema(example = "Coding") String name) {

    public static CategoryResponse from(Category c) {
        return new CategoryResponse(c.getId(), c.getName());
    }
}
