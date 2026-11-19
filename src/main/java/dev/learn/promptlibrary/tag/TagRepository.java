package dev.learn.promptlibrary.tag;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.panache.common.Parameters;
import io.quarkus.panache.common.Sort;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class TagRepository implements PanacheRepository<Tag> {

    public Optional<Tag> findByName(String ownerId, String name) {
        return find("ownerId = ?1 and name = ?2", ownerId, name).firstResultOptional();
    }

    /** Must be called inside a transaction. (Two concurrent creators could still collide on the unique index.) */
    public Tag findOrCreate(String ownerId, String name) {
        String normalized = Tag.normalizeName(name);
        return findByName(ownerId, normalized).orElseGet(() -> {
            Tag tag = new Tag();
            tag.setOwnerId(ownerId);
            tag.setName(normalized);
            persist(tag);
            return tag;
        });
    }

    /**
     * @param ownerId only this owner's tags; null = everybody's (admin)
     * @param q       optional free text searched in the tag name
     */
    public List<Tag> search(String ownerId, String q) {
        String query = "";
        Parameters params = new Parameters();

        if (ownerId != null) {
            query = "ownerId = :owner";
            params.and("owner", ownerId);
        }
        if (q != null && !q.isBlank()) {
            String filter = "lower(name) like :q";
            query = query.isEmpty() ? filter : query + " and " + filter;
            params.and("q", "%" + q.trim().toLowerCase(Locale.ROOT) + "%");
        }

        if (query.isEmpty()) {
            return findAll(Sort.by("name")).list();
        }
        return find(query, Sort.by("name"), params).list();
    }
}
