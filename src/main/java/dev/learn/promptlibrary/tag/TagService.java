package dev.learn.promptlibrary.tag;

import java.util.List;

import dev.learn.promptlibrary.common.BaseService;
import dev.learn.promptlibrary.common.ConflictException;
import dev.learn.promptlibrary.common.ResourceNotFoundException;
import dev.learn.promptlibrary.prompt.PromptRepository;
import dev.learn.promptlibrary.security.CurrentUser;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class TagService extends BaseService<Tag> {

    private final TagRepository tags;
    private final PromptRepository prompts;
    private final CurrentUser currentUser;

    public TagService(TagRepository tags, PromptRepository prompts, CurrentUser currentUser) {
        this.tags = tags;
        this.prompts = prompts;
        this.currentUser = currentUser;
    }

    @Transactional
    public List<TagResponse> list(String q) {
        String owner = currentUser.isAdmin() ? null : currentUser.id();
        return tags.search(owner, q).stream().map(TagResponse::from).toList();
    }

    @Transactional
    public TagResponse get(Long id) {
        return TagResponse.from(loadAccessible(id));
    }

    @Transactional
    public TagResponse create(TagRequest request) {
        String name = Tag.normalizeName(request.name());
        if (tags.findByName(currentUser.id(), name).isPresent()) {
            throw new ConflictException("Tag '%s' already exists".formatted(name));
        }
        Tag tag = new Tag();
        tag.setOwnerId(currentUser.id());
        tag.setName(name);
        tags.persist(tag);
        return TagResponse.from(tag);
    }

    @Transactional
    public TagResponse update(Long id, TagRequest request) {
        Tag tag = loadAccessible(id);
        String name = Tag.normalizeName(request.name());
        if (!name.equals(tag.getName()) && tags.findByName(tag.getOwnerId(), name).isPresent()) {
            throw new ConflictException("Tag '%s' already exists".formatted(name));
        }
        tag.setName(name);
        return TagResponse.from(tag);
    }

    @Transactional
    public void delete(Long id) {
        Tag tag = loadAccessible(id);

        if (prompts.countByTagId(id) > 0) {
            throw new ConflictException("Tag '%s' is in use by prompts".formatted(tag.getName()));
        }
        tags.delete(tag);
    }

    private Tag loadAccessible(Long id) {
        Tag tag = loadRequired(id);
        if (!tag.getOwnerId().equals(currentUser.id()) && !currentUser.isAdmin()) {
            // 404 instead of 403: do not reveal that the id exists
            throw new ResourceNotFoundException("Tag %d not found".formatted(id));
        }
        return tag;
    }

    @Override
    protected TagRepository repository() {
        return tags;
    }

    @Override
    protected String entityName() {
        return "Tag";
    }
}
