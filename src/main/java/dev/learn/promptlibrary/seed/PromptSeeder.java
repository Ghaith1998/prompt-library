package dev.learn.promptlibrary.seed;

import java.util.List;

import dev.learn.promptlibrary.category.Category;
import dev.learn.promptlibrary.category.CategoryRepository;
import dev.learn.promptlibrary.folder.Folder;
import dev.learn.promptlibrary.folder.FolderRepository;
import dev.learn.promptlibrary.folder.FolderService;
import dev.learn.promptlibrary.prompt.Prompt;
import dev.learn.promptlibrary.prompt.PromptRepository;
import dev.learn.promptlibrary.prompt.PromptStatus;
import dev.learn.promptlibrary.tag.Tag;
import dev.learn.promptlibrary.tag.TagRepository;

import io.quarkus.panache.common.Sort;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

/** Seeds through the ORM (entities + repositories), exactly like a Laravel seeder uses Eloquent models. */
@ApplicationScoped
public class PromptSeeder implements Seeder {

    private static final int TARGET_COUNT = 100;

    private final PromptRepository prompts;
    private final CategoryRepository categories;
    private final TagRepository tags;
    private final FolderRepository folders;
    private final FolderService folderService;

    public PromptSeeder(PromptRepository prompts, CategoryRepository categories,
                        TagRepository tags, FolderRepository folders, FolderService folderService) {
        this.prompts = prompts;
        this.categories = categories;
        this.tags = tags;
        this.folders = folders;
        this.folderService = folderService;
    }

    @Override
    public int order() {
        return 30;
    }

    @Override
    @Transactional
    public void seed() {
        if (prompts.count() >= TARGET_COUNT) {
            return;
        }

        List<Category> categoryList = categories.listAll(Sort.by("name"));
        if (categoryList.isEmpty()) {
            return;
        }

        List<String> owners = SeedUsers.all();
        for (int i = 1; i <= TARGET_COUNT && prompts.count() < TARGET_COUNT; i++) {
            String title = "Seed prompt %03d".formatted(i);
            if (prompts.find("title", title).firstResultOptional().isPresent()) {
                continue;
            }

            String ownerId = owners.get((i - 1) % owners.size());
            List<Folder> ownerFolders = foldersFor(ownerId);
            List<Tag> ownerTags = tagsFor(ownerId);
            int promptForOwner = (i - 1) / owners.size();

            Prompt prompt = new Prompt();
            prompt.setOwnerId(ownerId);
            prompt.setTitle(title);
            prompt.setContent("Seed content for prompt %03d.".formatted(i));
            prompt.setStatus(i % 3 == 0 ? PromptStatus.PUBLISHED : PromptStatus.DRAFT);
            prompt.setFavorite(i % 5 == 0);
            prompt.setCategory(categoryList.get((i - 1) % categoryList.size()));
            prompt.setFolder(ownerFolders.get(promptForOwner % ownerFolders.size()));

            int tagIndex = promptForOwner % ownerTags.size();
            for (int t = 0; t < Math.min(3, ownerTags.size()); t++) {
                prompt.addTag(ownerTags.get((tagIndex + t) % ownerTags.size()));
            }

            prompts.persist(prompt);
        }
    }

    private List<Folder> foldersFor(String ownerId) {
        List<Folder> ownerFolders = folders.listByOwner(ownerId);
        if (ownerFolders.isEmpty()) {
            folderService.ensureDefaultFolder(ownerId);
            return folders.listByOwner(ownerId);
        }
        return ownerFolders;
    }

    private List<Tag> tagsFor(String ownerId) {
        List<Tag> ownerTags = tags.search(ownerId, null);
        if (ownerTags.isEmpty()) {
            return List.of(tags.findOrCreate(ownerId, "tag-01"));
        }
        return ownerTags;
    }
}
