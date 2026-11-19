package dev.learn.promptlibrary.category;

import java.util.List;

import dev.learn.promptlibrary.category.dto.CategoryRequest;
import dev.learn.promptlibrary.category.dto.CategoryResponse;
import dev.learn.promptlibrary.common.ConflictException;

import io.quarkus.panache.common.Sort;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

/** Service layer: business rules + transaction boundaries. */
@ApplicationScoped
public class CategoryService {

    private final CategoryRepository categories;

    public CategoryService(CategoryRepository categories) {
        this.categories = categories;
    }

    @Transactional
    public List<CategoryResponse> list() {
        return categories.listAll(Sort.by("name")).stream().map(CategoryResponse::from).toList();
    }

    @Transactional
    public CategoryResponse create(CategoryRequest request) {
        String name = request.name().trim();
        if (categories.findByName(name).isPresent()) {
            throw new ConflictException("Category '%s' already exists".formatted(name));
        }
        Category category = new Category();
        category.setName(name);
        categories.persist(category);
        return CategoryResponse.from(category);
    }
}
