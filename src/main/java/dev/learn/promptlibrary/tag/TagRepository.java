package dev.learn.promptlibrary.tag;

import java.util.Optional;

import io.quarkus.hibernate.orm.panache.PanacheRepository;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class TagRepository implements PanacheRepository<Tag> {

    public Optional<Tag> findByName(String name) {
        return find("name", name).firstResultOptional();
    }

    /** Must be called inside a transaction. (Two concurrent creators could still collide on the unique index.) */
    public Tag findOrCreate(String name) {
        return findByName(name).orElseGet(() -> {
            var tag = new Tag();
            tag.setName(name);
            persist(tag);
            return tag;
        });
    }
}
