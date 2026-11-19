package dev.learn.promptlibrary.folder.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(name = "FolderUpdateRequest", description = "Payload to rename a folder")
public record FolderUpdateRequest(
        @Schema(description = "Folder name, unique per owner", example = "Backend")
        @NotBlank @Size(max = 100) String name) {
}
