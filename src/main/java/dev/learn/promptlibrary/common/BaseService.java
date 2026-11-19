package dev.learn.promptlibrary.common;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;

/**
 * Shared behavior for services that load entities by their Long id.
 * Concrete services keep their own scope, constructor injection and transaction boundaries.
 */
public abstract class BaseService<E extends BaseEntity> {

    protected abstract PanacheRepositoryBase<E, Long> repository();

    protected abstract String entityName();

    protected final E loadRequired(Long id) {
        return repository().findByIdOptional(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "%s %d not found".formatted(entityName(), id)));
    }
}
