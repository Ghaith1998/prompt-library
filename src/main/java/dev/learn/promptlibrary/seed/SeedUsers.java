package dev.learn.promptlibrary.seed;

import java.util.List;
import java.util.stream.IntStream;

/**
 * These ids are fixed in keycloak/import/prompt-library-realm.json so seeded data belongs to real users.
 * Users 1-3 are alice, bob and carol; users 4-50 are generated seed users.
 */
public final class SeedUsers {

    public static final String ALICE_ID = idFor(1);
    public static final String BOB_ID = idFor(2);
    public static final String CAROL_ID = idFor(3);

    private SeedUsers() {
    }

    public static String idFor(int index) {
        return "6f1c2b0e-0000-4000-8000-%012d".formatted(index);
    }

    public static List<String> all() {
        return IntStream.rangeClosed(1, 50).mapToObj(SeedUsers::idFor).toList();
    }
}
