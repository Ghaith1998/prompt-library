package dev.learn.promptlibrary.auth;

import java.util.Optional;

import io.smallrye.config.ConfigMapping;

/** Type-safe binding of the app.keycloak.* properties (Quarkus' answer to @ConfigurationProperties). */
@ConfigMapping(prefix = "app.keycloak")
public interface KeycloakConfig {

    /** e.g. http://localhost:8180 */
    String baseUrl();

    String realm();

    /** Login client: direct access grants. Public in prod, so the secret is optional. */
    String clientId();

    Optional<String> clientSecret();

    /** Registration client: client_credentials + service account. */
    String registerClientId();

    String registerClientSecret();
}
