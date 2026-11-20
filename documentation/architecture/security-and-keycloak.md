# Security and Keycloak

## Security model

The application has two distinct concerns:

1. **Validate bearer JWTs on normal API requests** with Quarkus OIDC (`quarkus.oidc.application-type=service`).
2. **Call Keycloak itself** for local/script login and self-registration, via Java's `HttpClient`.

There are no user entities in MySQL and no application-managed passwords, browser sessions, cookies, or OAuth callback routes. User identity consists of Keycloak UUIDs in `realm_access/sub` used as `owner_id` strings.

## Endpoint protection

- `quarkus.security.jaxrs.deny-unannotated-endpoints=true`: failure to annotate any new endpoint makes it denied by default.
- Feature resources use class-level `@Authenticated`.
- Category create/update/delete additionally uses `@RolesAllowed("admin")`.
- Only `/api/auth/login` and `/api/auth/register` are `@PermitAll`.

Roles within JWT are expected at `realm_access/roles`. Current code checks specifically for `"admin"` where needed; an authenticated user without that role follows the normal owner-bound rules.

## Identity adapter: `CurrentUser`

`CurrentUser` wraps `SecurityIdentity`. If its principal is a `JsonWebToken`, IDs come from the `sub` claim, names from `preferred_username`, and email from `email`. In test mode, `@TestSecurity` has no real JWT and the implementation intentionally falls back to the principal name. `isAdmin` is just a `SecurityIdentity.hasRole("admin")` check. `/api/me` returns this data without extra DB or Keycloak lookups.

## Registration and login client separation

`KeycloakConfig` binds the `app.keycloak` namespace:

- `base-url`, `realm` — used to construct Keycloak token/admin endpoints;
- `client-id`, `client-secret` — for password-grant login;
- `register-client-id`, `register-client-secret` — for registration's client-credentials flow.

Optional login secret support allows public clients in alternative deployments. Current defaults use `prompt-library-api` for both operations, matching the bundled realm. `register-client-secret` is required for the registration client to obtain its admin token.

## Login HTTP flow

```text
POST /api/auth/login
  -> KeycloakService.login
  -> POST {baseUrl}/realms/{realm}/protocol/openid-connect/token
     grant_type=password, client_id=..., optional client_secret
  -> 200   LoginResponse(accessToken, refreshToken, expiresIn, tokenType)
  -> 400/401 InvalidCredentialsException -> HTTP 401
  -> other UpstreamException            -> HTTP 502
```

## Registration HTTP flow

```text
POST /api/auth/register
  -> KeycloakService.adminToken (synchronized)
       if cached token is valid for > 30 seconds, reuse it
       else client_credentials + register-client-id/secret
  -> POST {baseUrl}/admin/realms/{realm}/users (Bearer admin token)
  -> 201 created: read ID from Location
  -> 409 already exists -> ConflictException -> HTTP 409
  -> everything else -> UpstreamException -> HTTP 502
```

The brief overlap window before the admin token expires is why refresh occurs 30 seconds early. Synchronization avoids concurrent refresh attempts on one application-scoped service.

The service needs `realm-management.manage-users` on the service account, or Keycloak will reject user creation. `InvalidCredentialsException` is for user login failures; Keycloak transport/configuration/permission errors are `UpstreamException` and avoid exposing raw protocol details in API bodies.

## Local realm and operational notes

- Realm file: `keycloak/import/prompt-library-realm.json`.
- Local Docker realm name/client: `prompt-library`, `prompt-library-api`.
- First Keycloak start imports it; already imported realms are not updated by restarting Keycloak. Delete the Keycloak data store (H2 volume, or schema if external DB is used) to re-import a changed realm definition.
- Bootstrap admin on Compose is `admin / admin`; that is **not** a realm `admin` role. Realm users govern the API (alice/bob/carol are admin/user, remaining generated users are normal user role).
- Dev preview accounts: `alice/alice`, `bob/bob`, `carol/carol`. Additional generated names `user04..user50` use their username as password. All 50 fixed IDs match `SeedUsers.idFor` so data can resolve to real realm users.

If login changes client, host, or realm, update both OIDC validation (`quarkus.oidc.*`) and outbound calls (`app.keycloak.*`); one validates incoming tokens, the other creates them/users.
