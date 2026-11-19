package dev.learn.promptlibrary.auth;

import io.smallrye.config.ConfigMapping;

/** Type-safe binding of the app.keycloak.* properties (Quarkus' answer to @ConfigurationProperties). */
@ConfigMapping(prefix = "app.keycloak")
public interface KeycloakConfig {

    /** e.g. http://localhost:8180 */
    String baseUrl();

    String realm();

    /** Confidential client with "service accounts" + "direct access grants" enabled. */
    String clientId();

    String clientSecret();
}
