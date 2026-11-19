package dev.learn.promptlibrary.seed;

import java.util.List;

import dev.learn.promptlibrary.category.Category;
import dev.learn.promptlibrary.category.CategoryRepository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class CategorySeeder implements Seeder {

    private final CategoryRepository categories;

    public CategorySeeder(CategoryRepository categories) {
        this.categories = categories;
    }

    @Override
    public int order() {
        return 10;
    }

    @Override
    @Transactional
    public void seed() {
        for (String name : List.of("Coding", "Writing", "Marketing")) {
            if (categories.findByName(name).isEmpty()) {
                Category category = new Category();
                category.setName(name);
                categories.persist(category);
            }
        }
    }
}
