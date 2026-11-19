package dev.learn.promptlibrary.category;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import dev.learn.promptlibrary.common.BaseEntity;
import dev.learn.promptlibrary.prompt.Prompt;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

/**
 * Global classification for prompts (not owner-scoped, unlike folders or tags).
 * Its unique name is also the business key used by equals/hashCode.
 */
@Entity
@Table(name = "categories")
public class Category extends BaseEntity {

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    /**
     * 1:n, INVERSE side. "mappedBy" points to the field on the owning side (Prompt.category).
     * No cascade: deleting a category must never silently delete prompts.
     */
    @OneToMany(mappedBy = "category")
    private Set<Prompt> prompts = new HashSet<>();

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Set<Prompt> getPrompts() { return prompts; }

    // business-key equality (name is unique) -> safe in HashSets, also before the id exists
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Category other)) return false;
        return Objects.equals(getName(), other.getName());
    }

    @Override
    public int hashCode() { return Objects.hashCode(getName()); }
}
