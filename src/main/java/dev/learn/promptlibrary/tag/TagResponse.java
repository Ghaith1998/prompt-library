package dev.learn.promptlibrary.tag;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

/** Tag projection, including the Keycloak subject owning the tag. */
@Schema(name = "TagResponse")
public record TagResponse(
        @Schema(example = "3") Long id,
        @Schema(example = "java") String name,
        @Schema(description = "Keycloak user id of the tag owner") String ownerId) {

    public static TagResponse from(Tag t) {
        return new TagResponse(t.getId(), t.getName(), t.getOwnerId());
    }
}
