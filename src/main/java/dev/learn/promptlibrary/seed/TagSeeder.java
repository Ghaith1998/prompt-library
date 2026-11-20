package dev.learn.promptlibrary.seed;

import dev.learn.promptlibrary.tag.TagRepository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

/** Ensures two owner-scoped demo tags exist for each of the 50 fixed users. */
@ApplicationScoped
public class TagSeeder implements Seeder {

    private static final int TAGS_PER_OWNER = 2;

    private final TagRepository tags;

    public TagSeeder(TagRepository tags) {
        this.tags = tags;
    }

    @Override
    public int order() {
        return 20;
    }

    @Override
    @Transactional
    public void seed() {
        for (String owner : SeedUsers.all()) {
            for (int i = 1; i <= TAGS_PER_OWNER; i++) {
                tags.findOrCreate(owner, "tag-%02d".formatted(i));
            }
        }
    }
}
