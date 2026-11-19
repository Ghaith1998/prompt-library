package dev.learn.promptlibrary.prompt;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import dev.learn.promptlibrary.category.Category;
import dev.learn.promptlibrary.category.CategoryRepository;
import dev.learn.promptlibrary.common.BaseService;
import dev.learn.promptlibrary.common.ResourceNotFoundException;
import dev.learn.promptlibrary.prompt.dto.PromptRequest;
import dev.learn.promptlibrary.prompt.dto.PromptResponse;
import dev.learn.promptlibrary.security.CurrentUser;
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
    private final CurrentUser currentUser;

    public PromptService(PromptRepository prompts, CategoryRepository categories,
                         TagRepository tags, CurrentUser currentUser) {
        this.prompts = prompts;
        this.categories = categories;
        this.tags = tags;
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

    // ---------------------------------------------------------------- helpers

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

    private void apply(Prompt prompt, PromptRequest request) {
        Category category = loadCategoryIfExist(request.categoryId());

        prompt.setTitle(request.title().trim());
        prompt.setContent(request.content());
        prompt.setCategory(category);
        if (request.status() != null) {
            prompt.setStatus(request.status());
        }
        if (request.favorite() != null) {
            prompt.setFavorite(request.favorite());
        }
        syncTags(prompt, request.tags());
    }

    private void syncTags(Prompt prompt, Set<String> requested) {
        Set<String> wanted = requested == null ? Set.of() : requested.stream()
                .filter(Objects::nonNull)
                .map(s -> s.trim().toLowerCase(Locale.ROOT))
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());

        // remove tags that are no longer wanted (copy first: we modify the set while iterating)
        new ArrayList<>(prompt.getTags()).stream()
                .filter(t -> !wanted.contains(t.getName()))
                .forEach(prompt::removeTag);

        // add the wanted ones (creating missing tags on the fly)
        wanted.forEach(name -> prompt.addTag(tags.findOrCreate(name)));
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
