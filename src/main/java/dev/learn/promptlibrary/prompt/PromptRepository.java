package dev.learn.promptlibrary.prompt;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Prompt persistence queries. The fetch graph preloads category, folder and tags
 * so DTO mapping inside a service transaction does not trigger N+1/lazy access.
 */
@ApplicationScoped
public class PromptRepository implements PanacheRepository<Prompt> {

    // "join fetch" loads category + tags in ONE query (avoids the N+1 problem when mapping to DTOs)
    private static final String FETCH =
            "from Prompt p join fetch p.category join fetch p.folder left join fetch p.tags";

    /**
     * @param ownerId only this owner's prompts; null = everybody's (admin)
     * @param q       optional free text searched in title and content
     */
    /**
     * Builds a parameterized owner/query filter dynamically. Null owner means
     * admin-wide visibility; q is a case-insensitive substring match.
     */
    public List<Prompt> search(String ownerId, String q) {
        List<String> conditions = new ArrayList<>();
        Map<String, Object> params = new HashMap<>();

        if (ownerId != null) {
            conditions.add("p.ownerId = :owner");
            params.put("owner", ownerId);
        }
        if (q != null && !q.isBlank()) {
            conditions.add("(lower(p.title) like :q or lower(p.content) like :q)");
            params.put("q", "%" + q.trim().toLowerCase(Locale.ROOT) + "%");
        }

        String where = conditions.isEmpty() ? "" : " where " + String.join(" and ", conditions);
        return find(FETCH + where + " order by p.createdAt desc", params).list();
    }

    public Optional<Prompt> findDetailed(Long id) {
        // list() + findFirst(): firstResult() would add a row LIMIT, which is unsafe together with a collection join fetch
        return find(FETCH + " where p.id = ?1", id).list().stream().findFirst();
    }

    public long countByCategoryId(Long categoryId) {
        return count("category.id", categoryId);
    }

    public long countByTagId(Long tagId) {
        return find("select p from Prompt p join p.tags t where t.id = ?1", tagId).count();
    }

    public long countByFolderId(Long folderId) {
        return count("folder.id", folderId);
    }
}
