package dev.learn.promptlibrary.auth;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import dev.learn.promptlibrary.auth.dto.LoginResponse;
import dev.learn.promptlibrary.auth.dto.RegisterRequest;
import dev.learn.promptlibrary.common.ConflictException;
import dev.learn.promptlibrary.common.InvalidCredentialsException;
import dev.learn.promptlibrary.common.UpstreamException;

import jakarta.enterprise.context.ApplicationScoped;

import org.jboss.logging.Logger;

/**
 * "Headless" Keycloak access using only the JDK HttpClient (no extra Quarkus extension needed, so you
 * can see exactly which HTTP calls happen):
 *
 *   login    -> POST /realms/{realm}/protocol/openid-connect/token      (grant_type=password)
 *   register -> POST /admin/realms/{realm}/users                        (Bearer = service-account token)
 *
 * The service account token comes from grant_type=client_credentials and is cached until shortly before it expires.
 */
@ApplicationScoped
public class KeycloakService {

    private static final Logger LOG = Logger.getLogger(KeycloakService.class);

    private final KeycloakConfig config;
    private final ObjectMapper mapper;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

    private String adminToken;
    private Instant adminTokenExpiresAt = Instant.EPOCH;

    public KeycloakService(KeycloakConfig config, ObjectMapper mapper) {
        this.config = config;
        this.mapper = mapper;
    }

    // ------------------------------------------------------------------ login

    public LoginResponse login(String username, String password) {
        Map<String, String> fields = new HashMap<>(Map.of(
                "grant_type", "password",
                "client_id", config.clientId(),
                "username", username,
                "password", password));
        config.clientSecret()
                .filter(secret -> !secret.isBlank())
                .ifPresent(secret -> fields.put("client_secret", secret));

        HttpResponse<String> response = post(tokenUrl(), "application/x-www-form-urlencoded", form(fields), null);

        int status = response.statusCode();
        if (status == 200) {
            TokenPayload t = read(response.body(), TokenPayload.class);
            return new LoginResponse(t.accessToken, t.refreshToken, t.expiresIn, t.tokenType);
        }
        if (status == 400 || status == 401) {
            // wrong password, disabled user, or "Account is not fully set up"
            LOG.warnf("Keycloak rejected login for '%s': %s", username, response.body());
            throw new InvalidCredentialsException("Invalid username or password");
        }
        throw upstream("login", response);
    }

    // --------------------------------------------------------------- register

    /** @return the id of the new Keycloak user */
    public String register(RegisterRequest request) {
        Map<String, Object> user = Map.of(
                "username", request.username(),
                "email", request.email(),
                "firstName", request.firstName(),
                "lastName", request.lastName(),
                "enabled", true,
                "emailVerified", true, // learning project: skip e-mail verification
                "credentials", List.of(Map.of(
                        "type", "password",
                        "value", request.password(),
                        "temporary", false)));

        HttpResponse<String> response = post(
                config.baseUrl() + "/admin/realms/" + config.realm() + "/users",
                "application/json",
                json(user),
                "Bearer " + adminToken());

        int status = response.statusCode();
        if (status == 201) {
            String location = response.headers().firstValue("Location").orElse("");
            return location.substring(location.lastIndexOf('/') + 1);
        }
        if (status == 409) {
            throw new ConflictException("Username or e-mail is already registered");
        }
        throw upstream("register", response);
    }

    // ------------------------------------------------------------------ admin

    /**
     * Returns a cached client-credentials token, refreshing 30 seconds before
     * expiry. Synchronization prevents parallel callers from stampeding Keycloak.
     */
    private synchronized String adminToken() {
        if (adminToken != null && Instant.now().isBefore(adminTokenExpiresAt.minusSeconds(30))) {
            return adminToken;
        }
        HttpResponse<String> response = post(tokenUrl(), "application/x-www-form-urlencoded", form(Map.of(
                "grant_type", "client_credentials",
                "client_id", config.registerClientId(),
                "client_secret", config.registerClientSecret())), null);

        if (response.statusCode() != 200) {
            throw upstream("service-account token", response);
        }
        TokenPayload t = read(response.body(), TokenPayload.class);
        adminToken = t.accessToken;
        adminTokenExpiresAt = Instant.now().plusSeconds(t.expiresIn);
        return adminToken;
    }

    // ---------------------------------------------------------------- helpers

    private String tokenUrl() {
        return config.baseUrl() + "/realms/" + config.realm() + "/protocol/openid-connect/token";
    }

    private HttpResponse<String> post(String url, String contentType, String body, String authorization) {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(10))
                .header("Content-Type", contentType)
                .POST(HttpRequest.BodyPublishers.ofString(body));
        if (authorization != null) {
            builder.header("Authorization", authorization);
        }
        try {
            return http.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        } catch (IOException e) {
            throw new UpstreamException("Keycloak is not reachable at " + config.baseUrl(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new UpstreamException("Interrupted while calling Keycloak", e);
        }
    }

    private static String form(Map<String, String> fields) {
        return fields.entrySet().stream()
                .map(e -> URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8) + "="
                        + URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
                .collect(Collectors.joining("&"));
    }

    private String json(Object value) {
        try {
            return mapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new UpstreamException("Cannot serialize request", e);
        }
    }

    private <T> T read(String body, Class<T> type) {
        try {
            return mapper.readValue(body, type);
        } catch (JsonProcessingException e) {
            throw new UpstreamException("Unexpected response from Keycloak", e);
        }
    }

    private UpstreamException upstream(String action, HttpResponse<String> response) {
        LOG.errorf("Keycloak %s failed: HTTP %d %s", action, response.statusCode(), response.body());
        return new UpstreamException("Keycloak " + action + " failed (HTTP " + response.statusCode()
                + "). Check the app log and the service-account roles.");
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class TokenPayload {
        @JsonProperty("access_token") public String accessToken;
        @JsonProperty("refresh_token") public String refreshToken;
        @JsonProperty("expires_in") public long expiresIn;
        @JsonProperty("token_type") public String tokenType;
    }
}
