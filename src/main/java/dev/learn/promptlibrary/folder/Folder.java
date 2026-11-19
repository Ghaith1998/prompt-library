package dev.learn.promptlibrary.folder;

import java.util.HashSet;
import java.util.Set;

import dev.learn.promptlibrary.common.BaseEntity;
import dev.learn.promptlibrary.prompt.Prompt;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * A user-owned folder node. Its optional parent forms a tree (root => null),
 * while its prompt set is one-to-many. The protected root default folder is
 * recognized by name rather than by a separate database boolean column.
 */
@Entity
@Table(name = "folders", uniqueConstraints = @UniqueConstraint(
        name = "uq_folders_owner_name", columnNames = {"owner_id", "name"}))
public class Folder extends BaseEntity {

    public static final String DEFAULT_NAME = "default";

    @Column(nullable = false, length = 100)
    private String name;

    /** The Keycloak user id ("sub" claim) of the folder owner. */
    @Column(name = "owner_id", nullable = false, length = 36)
    private String ownerId;

    /** Root folders have no parent. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Folder parent;

    /** 1:n, INVERSE side. No cascade: moving/deleting a parent must be explicit. */
    @OneToMany(mappedBy = "parent")
    private Set<Folder> children = new HashSet<>();

    /** 1:n, INVERSE side. Prompts belong to exactly one folder. */
    @OneToMany(mappedBy = "folder")
    private Set<Prompt> prompts = new HashSet<>();

    public boolean isDefaultFolder() {
        return parent == null && DEFAULT_NAME.equals(name);
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getOwnerId() { return ownerId; }
    public void setOwnerId(String ownerId) { this.ownerId = ownerId; }
    public Folder getParent() { return parent; }
    public void setParent(Folder parent) { this.parent = parent; }
    public Set<Folder> getChildren() { return children; }
    public Set<Prompt> getPrompts() { return prompts; }

    // Entity with a generated id: equal only if persisted and same id.
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Folder other)) return false;
        return getId() != null && getId().equals(other.getId());
    }

    @Override
    public int hashCode() { return Folder.class.hashCode(); }
}
