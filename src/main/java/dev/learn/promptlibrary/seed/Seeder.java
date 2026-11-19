package dev.learn.promptlibrary.seed;

/**
 * Laravel-style seeder: implement it, make it an @ApplicationScoped bean, and it is discovered automatically.
 * Seeders must be idempotent (safe to run on every start).
 */
public interface Seeder {

    /** Lower numbers run first (categories before prompts). */
    int order();

    void seed();
}
