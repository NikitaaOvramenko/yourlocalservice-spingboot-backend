# Phase 1 authentication

The frontend uses JSON bearer-token authentication. Login/refresh response fields
are unchanged: `{ accessToken, refreshToken, user }`.

| Endpoint | Body | Success |
| --- | --- | --- |
| `POST /api/auth/login` | `{ email, password }` | `200`, tokens and user |
| `POST /api/auth/refresh` | `{ refreshToken }` | `200`, rotated tokens and user |
| `POST /api/auth/logout` | `{ refreshToken }` | `204`, session revoked |
| `GET /api/auth/me` | Bearer access token | `200`, user |

The token endpoints do not need an Authorization header. Invalid/expired refresh
tokens return a generic `401` Problem Detail. Incorrect login credentials return
`401`, and unverified login/refresh returns `403`. Admin endpoints still require
ADMIN. A user who becomes unverified can no longer use an existing access token.

## Session behavior

- Flyway V7 adds `auth_session`. Rebuild/restart the backend to apply it.
- Existing tokens without purpose/session claims become invalid. Sign in again.
- Each login creates an independent seven-day session. Access tokens last one hour;
  a session expiring sooner also invalidates them. Refresh does not extend the
  absolute seven-day lifetime.
- JWT `tokenUse` separates access from refresh. `sid` identifies the session. Every
  refresh token has a random `jti`, including tokens issued within the same second.
- Only a SHA-256 hash of the current refresh token is stored. Refresh checks and
  replaces that hash under a PostgreSQL row lock. Concurrent uses of one refresh
  token produce one success; subsequent reuse returns 401.
- Logout locks and deletes the session, immediately revoking its access tokens and
  refresh tokens. Repeated logout with a still-valid signed refresh token is 204.
  A pre-rotation refresh token can revoke its own session, so logout remains safe
  when racing refresh. Separate logins remain active.
- Expired session rows are pruned on login. Account deletion cascades to sessions.
- Auth DTO `toString()` methods redact credentials/tokens from framework debug logs.
- Session validation happens on every bearer request; transient database errors
  are not mislabeled as invalid JWTs.

## Verification

Use the repository's normal Maven verify command with Testcontainers, or a
disposable PostgreSQL database via the existing test.database properties:

```text
mvn -B -Dtest.database.url=jdbc:postgresql://127.0.0.1:55439/phase1_auth_final -Dtest.database.username=admin_test verify
```

`AuthenticationWorkflowTest` exercises actual signatures, database migration,
refresh rotation/concurrency, token-purpose enforcement, logout and session
isolation. `JwtServiceTest`, `AuthServiceTest`, and `AdminApiSecurityTest` cover
expiry, malformed tokens, verification/role changes and sensitive log redaction.

For the frontend's opt-in live browser test, start this built jar on port 18080
with `src/test/resources/application-test.properties` as an additional config
location, an explicit disposable datasource, and CORS allowing
`http://127.0.0.1:3100`. Never use test properties or the test signing key in a
production instance. The frontend README contains the matching test command.

Public registration remains available on the backend for existing consumers; the
admin frontend has no registration page. HttpOnly cookies and login rate limiting
are separate changes, not part of this JSON-token contract.
