package dev.learn.promptlibrary.category;

import java.util.Optional;

import io.quarkus.hibernate.orm.panache.PanacheRepository;

import jakarta.enterprise.context.ApplicationScoped;

/** Repository layer: only talks to the DB. Panache gives find/list/persist/delete/count for free. */
@ApplicationScoped
public class CategoryRepository implements PanacheRepository<Category> {

    public Optional<Category> findByName(String name) {
        return find("name", name).firstResultOptional();
    }
}
