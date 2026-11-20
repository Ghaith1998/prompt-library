package dev.learn.promptlibrary.prompt;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import dev.learn.promptlibrary.category.Category;
import dev.learn.promptlibrary.category.CategoryRepository;
import dev.learn.promptlibrary.common.BaseService;
import dev.learn.promptlibrary.common.ResourceNotFoundException;
import dev.learn.promptlibrary.folder.Folder;
import dev.learn.promptlibrary.folder.FolderService;
import dev.learn.promptlibrary.prompt.dto.PromptRequest;
import dev.learn.promptlibrary.prompt.dto.PromptResponse;
import dev.learn.promptlibrary.security.CurrentUser;
import dev.learn.promptlibrary.tag.Tag;
import dev.learn.promptlibrary.tag.TagRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

/**
 * Business rules live here:
 *  - normal users see/modify only their own prompts, admins see everything
 *  - every method is @Transactional and maps entities to DTOs INSIDE the transaction
 *    (Quarkus has no "open session in view": lazy relations only load while the transaction is open)
 */
@ApplicationScoped
public class PromptService extends BaseService<Prompt> {

    private final PromptRepository prompts;
    private final CategoryRepository categories;
    private final TagRepository tags;
    private final FolderService folders;
    private final CurrentUser currentUser;

    public PromptService(PromptRepository prompts, CategoryRepository categories,
                         TagRepository tags, FolderService folders, CurrentUser currentUser) {
        this.prompts = prompts;
        this.categories = categories;
        this.tags = tags;
        this.folders = folders;
        this.currentUser = currentUser;
    }

    @Transactional
    public List<PromptResponse> list(String q) {
        String owner = currentUser.isAdmin() ? null : currentUser.id();
        return prompts.search(owner, q).stream().map(PromptResponse::from).toList();
    }

    @Transactional
    public PromptResponse get(Long id) {
        return PromptResponse.from(loadAccessible(id));
    }

    @Transactional
    public PromptResponse create(PromptRequest request) {
        Prompt prompt = new Prompt();
        prompt.setOwnerId(currentUser.id());
        apply(prompt, request);
        prompts.persist(prompt);
        return PromptResponse.from(prompt);
    }

    @Transactional
    public PromptResponse update(Long id, PromptRequest request) {
        Prompt prompt = loadAccessible(id);
        apply(prompt, request);
        prompts.flush(); // so updatedAt / version are fresh in the response
        return PromptResponse.from(prompt);
    }

    @Transactional
    public void delete(Long id) {
        prompts.delete(loadAccessible(id));
    }

    /** Moves a prompt to a folder owned by the same Keycloak subject and returns fresh DTO state. */
    @Transactional
    public PromptResponse moveToFolder(Long id, Long folderId) {
        Prompt prompt = loadAccessible(id);
        prompt.setFolder(folders.loadOwnedByOwner(folderId, prompt.getOwnerId()));
        prompts.flush(); // so the response reflects the new folder
        return PromptResponse.from(prompt);
    }

    // ---------------------------------------------------------------- helpers

    /**
     * Loads the fetch-graph for a prompt and returns 404 rather than 403 when it
     * belongs to another user. Admins bypass only the ownership visibility rule.
     */
    private Prompt loadAccessible(Long id) {
        Prompt prompt = prompts.findDetailed(id)
                .orElseThrow(() -> new ResourceNotFoundException("Prompt %d not found".formatted(id)));
        boolean owner = prompt.getOwnerId().equals(currentUser.id());
        if (!owner && !currentUser.isAdmin()) {
            // 404 instead of 403: do not reveal that the id exists
            throw new ResourceNotFoundException("Prompt %d not found".formatted(id));
        }
        return prompt;
    }

    /**
     * Applies the write contract to a new or existing prompt. Validation-backed
     * DTO values become associations here, while omitted optional fields keep
     * their create/update semantics.
     */
    private void apply(Prompt prompt, PromptRequest request) {
        Category category = loadCategoryIfExist(request.categoryId());
        Folder folder = resolveFolder(prompt, request.folderId());

        prompt.setTitle(request.title().trim());
        prompt.setContent(request.content());
        prompt.setCategory(category);
        prompt.setFolder(folder);
        if (request.status() != null) {
            prompt.setStatus(request.status());
        }
        if (request.favorite() != null) {
            prompt.setFavorite(request.favorite());
        }
        syncTags(prompt, request.tags());
    }

    /**
     * Explicit folder changes must reference the prompt owner's folder. With no
     * explicit folder, an existing prompt keeps its placement and a new prompt
     * lazily receives the owner's default folder.
     */
    private Folder resolveFolder(Prompt prompt, Long folderId) {
        if (folderId != null) {
            return folders.loadOwnedByOwner(folderId, prompt.getOwnerId());
        }
        if (prompt.getFolder() != null) {
            return prompt.getFolder();
        }
        return folders.ensureDefaultFolder(prompt.getOwnerId());
    }

    /**
     * Reconciles prompt tags with the requested names. The set copy avoids
     * concurrent modification; names are normalized consistently; missing tags
     * are created under the prompt owner, not the current admin caller.
     */
    private void syncTags(Prompt prompt, Set<String> requested) {
        Set<String> wanted = requested == null ? Set.of() : requested.stream()
                .filter(Objects::nonNull)
                .map(Tag::normalizeName)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());

        // remove tags that are no longer wanted (copy first: we modify the set while iterating)
        new ArrayList<>(prompt.getTags()).stream()
                .filter(t -> !wanted.contains(t.getName()))
                .forEach(prompt::removeTag);

        // add the wanted ones (creating missing tags for the prompt owner on the fly)
        wanted.forEach(name -> prompt.addTag(tags.findOrCreate(prompt.getOwnerId(), name)));
    }

    private Category loadCategoryIfExist(Long categoryId) {
        return categories.findByIdOptional(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Category %d not found".formatted(categoryId)));
    }

    @Override
    protected PromptRepository repository() {
        return prompts;
    }

    @Override
    protected String entityName() {
        return "Prompt";
    }
}
