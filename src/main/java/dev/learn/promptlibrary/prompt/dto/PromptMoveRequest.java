package dev.learn.promptlibrary.prompt.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

/** Identifies an existing folder as the new location of a prompt. */
@Schema(name = "PromptMoveRequest", description = "Move a prompt to another folder")
public record PromptMoveRequest(
        @Schema(description = "Target folder id", example = "7")
        @NotNull @Positive Long folderId) {
}
