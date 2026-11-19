package dev.learn.promptlibrary.prompt.dto;

import java.time.Instant;
import java.util.List;

import dev.learn.promptlibrary.prompt.Prompt;
import dev.learn.promptlibrary.prompt.PromptStatus;
import dev.learn.promptlibrary.tag.Tag;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

/** Fully mapped response; associations must be loaded inside a service transaction. */
@Schema(name = "PromptResponse", description = "A prompt as returned by the API")
public record PromptResponse(
        @Schema(example = "42") Long id,
        @Schema(example = "Code review assistant") String title,
        @Schema(example = "You are a senior Java reviewer...") String content,
        @Schema(example = "DRAFT") PromptStatus status,
        @Schema(example = "false") boolean favorite,
        @Schema(example = "1") Long categoryId,
        @Schema(example = "Coding") String categoryName,
        @Schema(example = "7") Long folderId,
        @Schema(example = "Backend") String folderName,
        @Schema(example = "[\"java\", \"review\"]") List<String> tags,
        @Schema(description = "Keycloak user id of the owner") String ownerId,
        Instant createdAt,
        Instant updatedAt) {

    public static PromptResponse from(Prompt p) {
        return new PromptResponse(
                p.getId(),
                p.getTitle(),
                p.getContent(),
                p.getStatus(),
                p.isFavorite(),
                p.getCategory().getId(),
                p.getCategory().getName(),
                p.getFolder().getId(),
                p.getFolder().getName(),
                p.getTags().stream().map(Tag::getName).sorted().toList(),
                p.getOwnerId(),
                p.getCreatedAt(),
                p.getUpdatedAt());
    }
}
