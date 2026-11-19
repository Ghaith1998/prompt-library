package dev.learn.promptlibrary.seed;

import java.util.List;

import dev.learn.promptlibrary.category.Category;
import dev.learn.promptlibrary.category.CategoryRepository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class CategorySeeder implements Seeder {

    private static final int TARGET_COUNT = 100;
    private static final List<String> BASE_NAMES = List.of("Coding", "Writing", "Marketing");

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
        for (int i = 1; i <= TARGET_COUNT; i++) {
            String name = i <= BASE_NAMES.size()
                    ? BASE_NAMES.get(i - 1)
                    : "Category %03d".formatted(i);
            if (categories.findByName(name).isEmpty()) {
                Category category = new Category();
                category.setName(name);
                categories.persist(category);
            }
        }
    }
}
