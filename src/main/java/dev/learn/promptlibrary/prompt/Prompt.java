package dev.learn.promptlibrary.prompt;

import java.util.HashSet;
import java.util.Set;

import dev.learn.promptlibrary.category.Category;
import dev.learn.promptlibrary.common.BaseEntity;
import dev.learn.promptlibrary.folder.Folder;
import dev.learn.promptlibrary.tag.Tag;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/**
 * Persistent prompt content and its organization metadata.
 * Ownership is a Keycloak subject, not a foreign key to a local user table.
 */
/**
 * Persistent prompt content and its organization metadata.
 * Ownership is a Keycloak subject, not a foreign key to a local user table.
 */
@Entity
@Table(name = "prompts")
public class Prompt extends BaseEntity {

    @Column(nullable = false, length = 200)
    private String title;

    // columnDefinition only documents the type; the real DDL lives in the Flyway migration
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    // EnumType.STRING: stored as text. Never use ORDINAL (reordering the enum would corrupt data).
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PromptStatus status = PromptStatus.DRAFT;

    @Column(name = "is_favorite", nullable = false)
    private boolean favorite;

    /** The Keycloak user id ("sub" claim). Plain string: users live in Keycloak, not in our DB. */
    @Column(name = "owner_id", nullable = false, length = 36)
    private String ownerId;

    /**
     * n:1, OWNING side (this table holds the foreign key).
     * LAZY on purpose: @ManyToOne is EAGER by default, which causes surprise queries.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    /** The user-owned folder this prompt belongs to. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "folder_id", nullable = false)
    private Folder folder;

    /**
     * n:m, OWNING side: defines the join table. Always use Set (not List) for n:m.
     * No cascade here: tags are shared between prompts.
     */
    @ManyToMany
    @JoinTable(name = "prompt_tags",
            joinColumns = @JoinColumn(name = "prompt_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id"))
    private Set<Tag> tags = new HashSet<>();

    /** Optimistic locking: a concurrent update of the same row fails instead of silently overwriting. */
    @Version
    private long version;

    /*
     * Helpers only touch the OWNING side. We deliberately do not update Tag.prompts:
     * that would force Hibernate to load every prompt of the tag just to add one element.
     */
    public void addTag(Tag tag) { tags.add(tag); }
    public void removeTag(Tag tag) { tags.remove(tag); }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public PromptStatus getStatus() { return status; }
    public void setStatus(PromptStatus status) { this.status = status; }
    public boolean isFavorite() { return favorite; }
    public void setFavorite(boolean favorite) { this.favorite = favorite; }
    public String getOwnerId() { return ownerId; }
    public void setOwnerId(String ownerId) { this.ownerId = ownerId; }
    public Category getCategory() { return category; }
    public void setCategory(Category category) { this.category = category; }
    public Folder getFolder() { return folder; }
    public void setFolder(Folder folder) { this.folder = folder; }
    public Set<Tag> getTags() { return tags; }
    public long getVersion() { return version; }

    // Entity with a generated id: equal only if persisted and same id; constant hashCode per class
    // (the standard recipe that stays stable when the id is assigned after persist()).
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Prompt other)) return false;
        return getId() != null && getId().equals(other.getId());
    }

    @Override
    public int hashCode() { return Prompt.class.hashCode(); }
}
