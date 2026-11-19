package dev.learn.promptlibrary.tag;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import dev.learn.promptlibrary.common.BaseEntity;
import dev.learn.promptlibrary.prompt.Prompt;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "tags")
public class Tag extends BaseEntity {

    @Column(nullable = false, unique = true, length = 50)
    private String name;

    /** n:m, INVERSE side (the join table is defined on Prompt.tags). Read-only mirror. */
    @ManyToMany(mappedBy = "tags")
    private Set<Prompt> prompts = new HashSet<>();

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Set<Prompt> getPrompts() { return prompts; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Tag other)) return false;
        return Objects.equals(getName(), other.getName());
    }

    @Override
    public int hashCode() { return Objects.hashCode(getName()); }
}
