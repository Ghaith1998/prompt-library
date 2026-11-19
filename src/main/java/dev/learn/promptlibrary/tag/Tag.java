package dev.learn.promptlibrary.tag;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import dev.learn.promptlibrary.common.BaseEntity;
import dev.learn.promptlibrary.prompt.Prompt;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * An owner-scoped label. The database key `(owner_id, name)` permits the same
 * text for different users without sharing the actual Tag entity.
 */
@Entity
@Table(name = "tags", uniqueConstraints = @UniqueConstraint(
        name = "uq_tags_owner_name", columnNames = {"owner_id", "name"}))
public class Tag extends BaseEntity {

    @Column(nullable = false, length = 50)
    private String name;

    /** The Keycloak user id ("sub" claim) of the tag owner. */
    @Column(name = "owner_id", nullable = false, length = 36)
    private String ownerId;

    /** n:m, INVERSE side (the join table is defined on Prompt.tags). Read-only mirror. */
    @ManyToMany(mappedBy = "tags")
    private Set<Prompt> prompts = new HashSet<>();

    public static String normalizeName(String name) {
        return name == null ? null : name.trim().toLowerCase(Locale.ROOT);
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getOwnerId() { return ownerId; }
    public void setOwnerId(String ownerId) { this.ownerId = ownerId; }
    public Set<Prompt> getPrompts() { return prompts; }

    // Entity with a generated id: equal only if persisted and same id.
    // Name is mutable and only unique per owner, so it is not a safe equality key.
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Tag other)) return false;
        return getId() != null && getId().equals(other.getId());
    }

    @Override
    public int hashCode() { return Tag.class.hashCode(); }
}
