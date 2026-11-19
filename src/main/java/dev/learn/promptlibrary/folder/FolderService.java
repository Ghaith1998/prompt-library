package dev.learn.promptlibrary.folder;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import dev.learn.promptlibrary.common.BaseService;
import dev.learn.promptlibrary.common.ConflictException;
import dev.learn.promptlibrary.common.ResourceNotFoundException;
import dev.learn.promptlibrary.folder.dto.FolderMoveRequest;
import dev.learn.promptlibrary.folder.dto.FolderRequest;
import dev.learn.promptlibrary.folder.dto.FolderResponse;
import dev.learn.promptlibrary.folder.dto.FolderUpdateRequest;
import dev.learn.promptlibrary.prompt.PromptRepository;
import dev.learn.promptlibrary.security.CurrentUser;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class FolderService extends BaseService<Folder> {

    private final FolderRepository folders;
    private final PromptRepository prompts;
    private final CurrentUser currentUser;

    public FolderService(FolderRepository folders, PromptRepository prompts, CurrentUser currentUser) {
        this.folders = folders;
        this.prompts = prompts;
        this.currentUser = currentUser;
    }

    @Transactional
    public List<FolderResponse> list() {
        ensureDefaultFolder(currentUser.id());
        String owner = currentUser.isAdmin() ? null : currentUser.id();
        return folders.listByOwner(owner).stream().map(FolderResponse::from).toList();
    }

    @Transactional
    public FolderResponse get(Long id) {
        return FolderResponse.from(loadAccessible(id));
    }

    @Transactional
    public FolderResponse create(FolderRequest request) {
        Folder parent = request.parentId() == null ? null : loadAccessible(request.parentId());
        String ownerId = parent == null ? currentUser.id() : parent.getOwnerId();
        ensureDefaultFolder(ownerId);

        String name = request.name().trim();
        if (Folder.DEFAULT_NAME.equals(name)) {
            throw new ConflictException("Folder name '%s' is reserved".formatted(Folder.DEFAULT_NAME));
        }
        if (folders.findByOwnerAndName(ownerId, name).isPresent()) {
            throw new ConflictException("Folder '%s' already exists".formatted(name));
        }

        Folder folder = new Folder();
        folder.setOwnerId(ownerId);
        folder.setName(name);
        folder.setParent(parent);
        folders.persist(folder);
        return FolderResponse.from(folder);
    }

    @Transactional
    public FolderResponse rename(Long id, FolderUpdateRequest request) {
        Folder folder = loadAccessible(id);
        if (folder.isDefaultFolder()) {
            throw new ConflictException("The default folder cannot be renamed");
        }

        String name = request.name().trim();
        if (Folder.DEFAULT_NAME.equals(name)) {
            throw new ConflictException("Folder name '%s' is reserved".formatted(Folder.DEFAULT_NAME));
        }
        if (!name.equals(folder.getName()) && folders.findByOwnerAndName(folder.getOwnerId(), name).isPresent()) {
            throw new ConflictException("Folder '%s' already exists".formatted(name));
        }
        folder.setName(name);
        return FolderResponse.from(folder);
    }

    @Transactional
    public void delete(Long id) {
        Folder folder = loadAccessible(id);
        if (folder.isDefaultFolder()) {
            throw new ConflictException("The default folder cannot be deleted");
        }
        if (prompts.countByFolderId(id) > 0) {
            throw new ConflictException("Folder '%s' is in use by prompts".formatted(folder.getName()));
        }
        if (folders.countChildren(id) > 0) {
            throw new ConflictException("Folder '%s' contains child folders".formatted(folder.getName()));
        }
        folders.delete(folder);
    }

    @Transactional
    public FolderResponse move(Long id, FolderMoveRequest request) {
        Folder folder = loadAccessible(id);
        if (folder.isDefaultFolder()) {
            throw new ConflictException("The default folder cannot be moved");
        }

        Long currentParentId = folder.getParent() == null ? null : folder.getParent().getId();
        if (Objects.equals(currentParentId, request.parentId())) {
            return FolderResponse.from(folder);
        }

        Folder parent = null;
        if (request.parentId() != null) {
            parent = loadAccessible(request.parentId());
            if (!parent.getOwnerId().equals(folder.getOwnerId())) {
                throw new ConflictException("Folder and parent must belong to the same owner");
            }
            ensureNotDescendant(folder, parent);
        }

        folder.setParent(parent);
        return FolderResponse.from(folder);
    }

    /**
     * Resolves a folder through an explicit owner. Used when the prompt owner, not the
     * current caller, determines which folder may be attached.
     */
    public Folder loadOwnedByOwner(Long id, String ownerId) {
        Folder folder = loadRequired(id);
        if (!folder.getOwnerId().equals(ownerId)) {
            throw new ResourceNotFoundException("Folder %d not found".formatted(id));
        }
        return folder;
    }

    @Transactional
    public Folder ensureDefaultFolder(String ownerId) {
        return folders.findDefault(ownerId).orElseGet(() -> {
            Folder folder = new Folder();
            folder.setOwnerId(ownerId);
            folder.setName(Folder.DEFAULT_NAME);
            folders.persist(folder);
            return folder;
        });
    }

    private void ensureNotDescendant(Folder folder, Folder target) {
        Set<Long> visited = new HashSet<>();
        Folder cursor = target;
        while (cursor != null) {
            if (cursor.getId().equals(folder.getId())) {
                throw new ConflictException("A folder cannot be moved inside itself");
            }
            if (!visited.add(cursor.getId())) {
                throw new ConflictException("Folder hierarchy contains a cycle");
            }
            cursor = cursor.getParent();
        }
    }

    private Folder loadAccessible(Long id) {
        Folder folder = loadRequired(id);
        if (!folder.getOwnerId().equals(currentUser.id()) && !currentUser.isAdmin()) {
            // 404 instead of 403: do not reveal that the id exists
            throw new ResourceNotFoundException("Folder %d not found".formatted(id));
        }
        return folder;
    }

    @Override
    protected FolderRepository repository() {
        return folders;
    }

    @Override
    protected String entityName() {
        return "Folder";
    }
}
