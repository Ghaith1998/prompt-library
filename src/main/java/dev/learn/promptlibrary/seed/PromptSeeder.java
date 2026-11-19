package dev.learn.promptlibrary.seed;

import java.util.List;

import dev.learn.promptlibrary.category.CategoryRepository;
import dev.learn.promptlibrary.prompt.Prompt;
import dev.learn.promptlibrary.prompt.PromptRepository;
import dev.learn.promptlibrary.prompt.PromptStatus;
import dev.learn.promptlibrary.tag.TagRepository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

/** Seeds through the ORM (entities + repositories), exactly like a Laravel seeder uses Eloquent models. */
@ApplicationScoped
public class PromptSeeder implements Seeder {

    private record Row(String owner, String category, String title, String content,
                       PromptStatus status, boolean favorite, List<String> tags) {
    }

    private static final List<Row> ROWS = List.of(
            new Row(SeedUsers.ALICE_ID, "Coding", "Code review assistant",
                    "You are a senior Java reviewer. Review the following code for bugs, readability and performance.",
                    PromptStatus.PUBLISHED, true, List.of("java", "review")),
            new Row(SeedUsers.ALICE_ID, "Coding", "Unit test generator",
                    "Write JUnit 5 tests with edge cases for the following method.",
                    PromptStatus.PUBLISHED, false, List.of("java", "testing")),
            new Row(SeedUsers.ALICE_ID, "Writing", "Blog post outline",
                    "Create a clear outline with headings for a blog post about the given topic.",
                    PromptStatus.DRAFT, false, List.of("outline")),
            new Row(SeedUsers.BOB_ID, "Marketing", "Product launch tweet",
                    "Write three punchy tweets announcing the following product launch.",
                    PromptStatus.DRAFT, false, List.of("social")),
            new Row(SeedUsers.BOB_ID, "Writing", "Summarize in 5 bullets",
                    "Summarize the following text in exactly five bullet points.",
                    PromptStatus.PUBLISHED, true, List.of("summary", "outline")));

    private final PromptRepository prompts;
    private final CategoryRepository categories;
    private final TagRepository tags;

    public PromptSeeder(PromptRepository prompts, CategoryRepository categories, TagRepository tags) {
        this.prompts = prompts;
        this.categories = categories;
        this.tags = tags;
    }

    @Override
    public int order() {
        return 20;
    }

    @Override
    @Transactional
    public void seed() {
        if (prompts.count() > 0) {
            return; // idempotent
        }
        for (Row row : ROWS) {
            Prompt prompt = new Prompt();
            prompt.setOwnerId(row.owner());
            prompt.setTitle(row.title());
            prompt.setContent(row.content());
            prompt.setStatus(row.status());
            prompt.setFavorite(row.favorite());
            prompt.setCategory(categories.findByName(row.category()).orElseThrow());
            row.tags().forEach(name -> prompt.addTag(tags.findOrCreate(row.owner(), name)));
            prompts.persist(prompt);
        }
    }
}
