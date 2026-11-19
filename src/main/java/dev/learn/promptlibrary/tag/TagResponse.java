package dev.learn.promptlibrary.tag;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(name = "TagResponse")
public record TagResponse(
        @Schema(example = "3") Long id,
        @Schema(example = "java") String name) {

    public static TagResponse from(Tag t) {
        return new TagResponse(t.getId(), t.getName());
    }
}
