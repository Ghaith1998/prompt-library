package dev.learn.promptlibrary.category;

import java.util.List;

import dev.learn.promptlibrary.category.dto.CategoryRequest;
import dev.learn.promptlibrary.category.dto.CategoryResponse;
import dev.learn.promptlibrary.common.BaseService;
import dev.learn.promptlibrary.common.ConflictException;
import dev.learn.promptlibrary.prompt.PromptRepository;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

/** Service layer: business rules + transaction boundaries. */
@ApplicationScoped
public class CategoryService extends BaseService<Category> {

    private final CategoryRepository categories;
    private final PromptRepository prompts;

    public CategoryService(CategoryRepository categories, PromptRepository prompts) {
        this.categories = categories;
        this.prompts = prompts;
    }

    @Transactional
    public List<CategoryResponse> list() {
        return categories.listAll(Sort.by("name")).stream().map(CategoryResponse::from).toList();
    }

    @Transactional
    public CategoryResponse create(CategoryRequest request) {
        String name = request.name().trim();
        validateNameUnique(name);

        Category category = new Category();
        category.setName(name);
        categories.persist(category);
        
        return CategoryResponse.from(category);
    }

    @Transactional
    public void delete(Long id) {
        Category category = loadRequired(id);

        if (prompts.countByCategoryId(id) > 0) {
            throw new ConflictException("Category '%s' is in use by prompts".formatted(category.getName()));
        }
        categories.delete(category);
    }

    @Transactional
    public CategoryResponse update(Long id, CategoryRequest request) {
        Category category = loadRequired(id);
        String name = request.name().trim();
        validateNameUnique(name);

        category.setName(name);
        return CategoryResponse.from(category);
    }

    @Override
    protected CategoryRepository repository() {
        return categories;
    }

    @Override
    protected String entityName() {
        return "Category";
    }
}
