package dev.learn.promptlibrary.common;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;

/**
 * Shared service primitive: load an entity by Long id or raise a consistent 404.
 * Subclasses supply their Panache repository and the name used in error messages.
 * Transactions, DTO mapping, ownership checks, and feature invariants must remain
 * in the concrete services; this is not a generic CRUD service.
 */
public abstract class BaseService<E extends BaseEntity> {

    protected abstract PanacheRepositoryBase<E, Long> repository();

    protected abstract String entityName();

    protected final E loadRequired(Long id) {
        return repository().findByIdOptional(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "%s %d not found".formatted(entityName(), id)));
    }

    /**
     * Rejects a duplicate globally scoped entity name. Per-owner resources such as
     * tags must perform their ownership-scoped duplicate check instead of using this.
     */
    protected final void validateNameUnique(String name) {
        if (repository().find("name", name).firstResultOptional().isPresent()) {
            throw new ConflictException("%s '%s' already exists".formatted(entityName(), name));
        }
    }
}
