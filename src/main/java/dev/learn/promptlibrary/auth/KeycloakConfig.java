package dev.learn.promptlibrary.auth;

import java.util.Optional;

import io.smallrye.config.ConfigMapping;

/** Type-safe binding of the app.keycloak.* properties (Quarkus' answer to @ConfigurationProperties). */
@ConfigMapping(prefix = "app.keycloak")
public interface KeycloakConfig {

    /** e.g. http://localhost:8180 */
    String baseUrl();

    String realm();

    /** Password-grant client. The secret is optional to permit public-client deployments. */
    String clientId();

    Optional<String> clientSecret();

    /** Registration client: client_credentials + service account. */
    String registerClientId();

    /** Required for the client-credentials token used by registration. */
    String registerClientSecret();
}
