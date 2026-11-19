package dev.learn.promptlibrary.folder.dto;

import dev.learn.promptlibrary.folder.Folder;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

/** Flat folder projection; parentId lets clients reconstruct the tree. */
@Schema(name = "FolderResponse")
public record FolderResponse(
        @Schema(example = "7") Long id,
        @Schema(example = "Backend") String name,
        @Schema(description = "Keycloak user id of the folder owner") String ownerId,
        @Schema(example = "42") Long parentId,
        @Schema(example = "false") boolean isDefault) {

    public static FolderResponse from(Folder folder) {
        return new FolderResponse(
                folder.getId(),
                folder.getName(),
                folder.getOwnerId(),
                folder.getParent() == null ? null : folder.getParent().getId(),
                folder.isDefaultFolder());
    }
}
