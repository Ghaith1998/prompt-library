package dev.learn.promptlibrary.security;

import java.util.Optional;
import java.util.Set;

import io.quarkus.security.identity.SecurityIdentity;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.jwt.JsonWebToken;

/**
 * Everything we need to know about the caller comes from the (already validated) token.
 * No DB call and no Keycloak call needed.
 */
@ApplicationScoped
public class CurrentUser {

    @Inject
    SecurityIdentity identity;

    private Optional<JsonWebToken> token() {
        return identity.getPrincipal() instanceof JsonWebToken t ? Optional.of(t) : Optional.empty();
    }

    private Optional<String> claim(String name) {
        return token().map(t -> t.<Object>getClaim(name)).map(Object::toString).filter(s -> !s.isBlank());
    }

    /** Keycloak user id ("sub"). Falls back to the principal name (used by @TestSecurity in tests). */
    public String id() {
        return token().map(JsonWebToken::getSubject)
                .filter(s -> !s.isBlank())
                .orElseGet(() -> identity.getPrincipal().getName());
    }

    public String username() {
        return claim("preferred_username").orElseGet(() -> identity.getPrincipal().getName());
    }

    public Optional<String> email() {
        return claim("email");
    }

    public Set<String> roles() {
        return identity.getRoles();
    }

    public boolean isAdmin() {
        return identity.hasRole("admin");
    }
}
