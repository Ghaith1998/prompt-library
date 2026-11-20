package dev.learn.promptlibrary.tag;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

/** Create/rename payload; name uniqueness is scoped to the caller's owner. */
@Schema(name = "TagRequest", description = "Payload to create or update a tag")
public record TagRequest(
        @Schema(description = "Unique tag name for the current owner", example = "java")
        @NotBlank @Size(max = 50) String name) {
}
