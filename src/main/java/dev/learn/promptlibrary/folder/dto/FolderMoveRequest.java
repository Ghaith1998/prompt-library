package dev.learn.promptlibrary.folder.dto;

import jakarta.validation.constraints.Positive;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(name = "FolderMoveRequest", description = "Move a folder under another folder, or to the root")
public record FolderMoveRequest(
        @Schema(description = "Target parent folder id; null moves the folder to the root", example = "42")
        @Positive Long parentId) {
}
