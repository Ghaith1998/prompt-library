package dev.learn.promptlibrary.folder.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(name = "FolderRequest", description = "Payload to create a folder")
public record FolderRequest(
        @Schema(description = "Folder name, unique per owner", example = "Backend")
        @NotBlank @Size(max = 100) String name,

        @Schema(description = "Optional parent folder; omitted or null creates a root folder", example = "42")
        @Positive Long parentId) {
}
