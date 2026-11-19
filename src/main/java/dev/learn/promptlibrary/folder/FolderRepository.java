package dev.learn.promptlibrary.folder;

import java.util.List;
import java.util.Optional;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.panache.common.Sort;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * Folder lookup and cheap count helpers. A default folder is specifically a
 * root (parent IS NULL) named "default" for one owner.
 */
@ApplicationScoped
public class FolderRepository implements PanacheRepository<Folder> {

    public Optional<Folder> findDefault(String ownerId) {
        return find("ownerId = ?1 and name = ?2 and parent is null",
                ownerId, Folder.DEFAULT_NAME).firstResultOptional();
    }

    public Optional<Folder> findByOwnerAndName(String ownerId, String name) {
        return find("ownerId = ?1 and name = ?2", ownerId, name).firstResultOptional();
    }

    /** @param ownerId only this owner's folders; null = everybody's (admin) */
    public List<Folder> listByOwner(String ownerId) {
        if (ownerId == null) {
            return findAll(Sort.by("name")).list();
        }
        return find("ownerId = ?1", Sort.by("name"), ownerId).list();
    }

    public long countChildren(Long parentId) {
        return count("parent.id", parentId);
    }
}
