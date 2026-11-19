package dev.learn.promptlibrary.seed;

import java.util.Comparator;

import io.quarkus.runtime.StartupEvent;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

/**
 * Runs every Seeder bean (sorted by order()) at startup, but only when app.seed.enabled=true
 * (on in %dev, off everywhere else).
 */
@ApplicationScoped
public class DatabaseSeeder {

    private static final Logger LOG = Logger.getLogger(DatabaseSeeder.class);

    @ConfigProperty(name = "app.seed.enabled", defaultValue = "false")
    boolean enabled;

    @Inject
    @Any
    Instance<Seeder> seeders;

    void onStart(@Observes StartupEvent event) {
        if (!enabled) {
            return;
        }
        LOG.info("Seeding database...");
        seeders.stream()
                .sorted(Comparator.comparingInt(Seeder::order))
                .forEach(seeder -> {
                    LOG.infof("  running %s", seeder.getClass().getSimpleName().replace("_ClientProxy", ""));
                    seeder.seed();
                });
        LOG.info("Seeding finished.");
    }
}
