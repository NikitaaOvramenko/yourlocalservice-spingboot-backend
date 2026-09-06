# YourLocal[Service]

![Java](https://img.shields.io/badge/Java-17-orange.svg)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0-green.svg)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-blue.svg)
![Flyway](https://img.shields.io/badge/Flyway-managed%20schema-red.svg)

## Overview

A single backend serving several "Your Local Service" sites. Each site is an
**Organization** with its own slug, its own catalog of **Services**, and its own
business inbox. Clients submit **Quotes** against an organization; a quote can become a
**Job**, and a completed job can receive a **Review**.

The API is organization-scoped by URL: `POST /api/orgs/yourlocalpaints/quotes`.

## Architecture

Layered, package-per-aggregate:

- **Controller** — HTTP and response shaping.
- **Service** — business logic and transaction boundaries.
- **Repository** — Spring Data JPA.
- **Domain** — entities and DTOs, kept separate so the API contract and the schema can
  move independently.

One top-level folder per entity, each with `model/` plus whichever of `repository/`,
`service/`, `controller/`, `dto/`, `enums/`, `exception/`, `mapper/`, `event/` it
actually needs. The folder name matches the entity class inside it.

```
client/          location/       organization/
serviceoffering/ organizationserviceoffering/
quote/           quotelineitem/
job/             joblineitem/    review/

common/     shared @MappedSuperclass (created_at / updated_at)
email/ file/ configuration/     supporting services, not aggregates
web/                            @RestControllerAdvice, RFC 9457 ProblemDetail
```

Line items are managed through nested admin controllers and services under their
parent quote or job. Ownership checks prevent an item from being edited through a
different parent's URL.

### Entity-Relationship Diagram

```mermaid
erDiagram
    CLIENT       ||--o{ LOCATION            : has
    CLIENT       ||--o{ QUOTE               : requests
    CLIENT       ||--o{ JOB                 : owns
    LOCATION     ||--o{ QUOTE               : "sited at"
    LOCATION     ||--o{ JOB                 : "sited at"

    ORGANIZATION ||--o{ ORGANIZATION_SERVICE : offers
    SERVICE      ||--o{ ORGANIZATION_SERVICE : "offered by"

    ORGANIZATION ||--o{ QUOTE               : receives
    ORGANIZATION ||--o{ JOB                 : performs

    QUOTE        ||--o{ QUOTE_SERVICE       : "line items"
    SERVICE      ||--o{ QUOTE_SERVICE       : "quoted as"
    QUOTE        ||--o{ QUOTE_PICTURE       : "S3 keys"

    QUOTE        ||--o| JOB                 : "becomes (optional)"

    JOB          ||--o{ JOB_SERVICE         : "line items"
    SERVICE      ||--o{ JOB_SERVICE         : "performed as"
    JOB          ||--o| REVIEW              : "reviewed by"

    CLIENT {
        bigint  id PK
        varchar first_name
        varchar last_name
        varchar email UK
        varchar phone
    }
    LOCATION {
        bigint  id PK
        bigint  client_id FK
        varchar country
        varchar province_state
        varchar city
        varchar street
        varchar postal_code
    }
    ORGANIZATION {
        bigint  id PK
        varchar name
        varchar slug UK
        varchar contact_email
        boolean active
        varchar smtp_host "+7 mail cols, all nullable"
    }
    SERVICE {
        bigint  id PK
        varchar name
        varchar slug UK
        text    description
        boolean active
    }
    ORGANIZATION_SERVICE {
        bigint organization_id PK_FK
        bigint service_id PK_FK
    }
    QUOTE {
        bigint      id PK
        bigint      client_id FK
        bigint      organization_id FK
        bigint      location_id FK
        text        description
        varchar     status
        timestamptz created_at
        timestamptz updated_at
        timestamptz expires_at
    }
    QUOTE_SERVICE {
        bigint  id PK
        bigint  quote_id FK
        bigint  service_id FK
        numeric price
        integer quantity
        text    description
    }
    QUOTE_PICTURE {
        bigint  quote_id PK_FK
        varchar object_key
        integer position PK
    }
    JOB {
        bigint      id PK
        bigint      quote_id FK "nullable"
        bigint      client_id FK
        bigint      organization_id FK
        bigint      location_id FK
        text        description
        varchar     status
        timestamptz scheduled_at
        timestamptz started_at
        timestamptz completed_at
        timestamptz created_at
        timestamptz updated_at
    }
    JOB_SERVICE {
        bigint  id PK
        bigint  job_id FK
        bigint  service_id FK
        numeric price
        integer quantity
        text    description
        varchar status
    }
    REVIEW {
        bigint      id PK
        bigint      job_id FK-UK
        integer     rating
        text        comment
        varchar     status
        timestamptz created_at
    }
```

### Modelling notes

- **`Service` is `ServiceOffering` in Java**, and the join/line-item tables map to
  `OrganizationServiceOffering`, `QuoteLineItem` and `JobLineItem`. The table names are
  unchanged; the class names differ only to avoid colliding with Spring's `@Service`
  and with the business service classes.
- **Organization↔Service is an explicit join entity, not `@ManyToMany`.** The common
  query is "does this org offer this service?", which is a single primary-key probe
  against `organization_service`; `@ManyToMany` would mean loading the org's whole
  catalog to answer it, and cannot carry future columns like a price override.
- **`job.quote_id` is nullable**, with a *partial* unique index
  (`WHERE quote_id IS NOT NULL`). One quote yields at most one job, but a walk-in job
  needs no quote at all.
- **`job` keeps its own `client_id` / `organization_id` / `location_id`** rather than
  reading them through the quote. They are job-level business data: a job can be
  relocated or reassigned after quoting without rewriting the original quote.
- **`price` is a unit price, not a line total.** The total is `price * quantity`,
  computed and never stored. All money is `BigDecimal` / `NUMERIC(12,2)`.
- **All `@ManyToOne` / `@OneToOne` are explicitly `LAZY`**, and `open-in-view` is off
  so a missing fetch fails loudly in tests rather than becoming N+1 in production.

## API

### Submit a quote (current)

`POST /api/orgs/{slug}/quotes` → `201 Created` with a `Location` header.

```json
{
  "client":   { "firstName": "John", "lastName": "Doe",
                "email": "john.doe@example.com", "phone": "+14165551234" },
  "location": { "country": "CANADA", "provinceState": "ON", "city": "Toronto",
                "street": "5 King St", "postalCode": "M5H 1A1" },
  "services": [ { "serviceId": 21, "quantity": 1, "description": "old sofa" } ],
  "description": "Basement cleanout",
  "pictureKeys": []
}
```

`country` is one of `USA`, `CANADA`. `serviceId` values come from the catalog endpoint
below — a request naming a service the organization does not offer is rejected.

### Discover an organization's catalog

`GET /api/orgs/{slug}/services` → `[{ "id", "name", "slug", "description" }]`

`GET /api/orgs/{slug}` → `{ "id", "name", "slug" }`

### Authentication

Stateless JWT. `POST /api/auth/login` returns an access token (1h), a refresh token
(7d) and the user; send the access token as `Authorization: Bearer <token>`.

| Endpoint | Auth | Notes |
| :--- | :--- | :--- |
| `POST /api/auth/register` | public | Creates an **unverified** account — see below |
| `POST /api/auth/login` | public | `{email, password}` → `{accessToken, refreshToken, user}` |
| `POST /api/auth/refresh` | public | `{refreshToken}` → a fresh pair |
| `GET /api/auth/me` | bearer | The authenticated user |

**Authorization is deny-by-default.** Only the customer-facing quote funnel is public —
quote submission, upload minting, the org catalog, the legacy form endpoint, and
`/actuator/health`. Anything else, *including any endpoint added later*, requires a
token. That is the point: new admin endpoints are protected unless someone deliberately
opens them.

Failed logins return the same `401 Incorrect email or password` whether the address
exists or not, so the endpoint cannot be used to discover which emails have accounts.

#### Email verification

Registration creates the account with `verified = false` and emails a link; login
rejects unverified accounts with `403` until it is followed.

| Endpoint | Auth | Notes |
| :--- | :--- | :--- |
| `POST /api/auth/verify/send` | public | `{email}` — resend a link |
| `GET /api/auth/verify/{token}` | public | Followed from the email; plain-text response |

A token is a random UUID row in `email_verification` with an expiry, and is **single
use**: it is deleted when followed, and deleted when an expired one is presented, so the
table only ever holds outstanding links. Issuing a new link clears the previous one, so
a user never has two live tokens.

`POST /verify/send` answers identically whether or not the address has an account —
otherwise it would be a way to ask "is this person registered here?", which the login
endpoint already avoids leaking.

An expired link returns `410 Gone` rather than `400`, so a client can tell "this link
died" from "this link is wrong" and offer a resend.

Configure with **`VERIFICATION_BASE_URL`** (must be the public URL — a localhost value
means every emailed link is dead on arrival) and `VERIFICATION_TTL_MINUTES` (default 60).

The seeded admin from `ADMIN_PASSWORD` starts verified, so there is always one account
that can sign in.

### Upload a photo

`POST /api/orgs/{slug}/uploads` with `{ "fileName": "basement.jpg" }`
→ `{ "key": "orgs/tcs/quotes/2026/09/04/<uuid>-basement.jpg", "url": "<presigned S3 PUT URL>" }`

PUT the file to `url`, then pass the returned `key` in the quote's `pictureKeys`. One
call per file. The presigned PUT is valid for 10 minutes.

**The client never constructs the key.** The prefix comes from the resolved org slug and
uniqueness from a server-generated UUID, so a caller cannot address another
organization's prefix — only the file name is client-supplied, and it is sanitised
(anything outside `\p{L}\p{N}._-` collapsed to `-`, dot runs collapsed, truncated
keeping the extension).

This replaces `GET /api/upload/{name}`, which signed whatever key arrived — meaning any
caller could overwrite any object in the bucket. A POST body also avoids the fact that
Tomcat rejects `%2F` inside a path segment with a 400, so a slash-bearing key could not
travel in the URL at all.

Key shape is `orgs/<slug>/quotes/<yyyy>/<MM>/<dd>/<uuid>-<name>`:

- the **`quotes`** segment lets an S3 lifecycle rule target quote photos alone — a rule
  scoped to `orgs/tcs/quotes/` expires them without touching anything else that org
  keeps in the bucket;
- the **date** is zero-padded so objects sort chronologically in the console, and is
  **UTC** — deterministic, no DST edges, consistent across orgs in several countries.
  The cost is an off-by-one (an 8pm EDT quote files under the next day), which is fine
  because `quote.created_at` is the authoritative timestamp and real lookups go through
  SQL. Per-org timezones would be a column on `organization`, not a constant.

Nothing reads the key's shape — `object_key` stores it verbatim, so objects written
under older shapes keep resolving and there is nothing to backfill.

### Submit a quote (deprecated)

`POST /api/email/form` — the original endpoint, unchanged on the wire, kept so the
deployed frontends keep working. It resolves the free-text `workType` to an
organization and each `service` string to a catalog row, then calls the same code path
as the endpoint above. New clients should not use it.

```json
{
  "name": "John", "lastname": "Doe", "email": "john.doe@example.com",
  "phone": "+14165551234", "workType": "YourLocalJunkRemoval",
  "service": ["Furniture Removal"], "country": "CANADA", "town": "Toronto",
  "street": "5 King St", "postal_code": "M5H 1A1",
  "description": "Basement cleanout", "images": []
}
```

Note `service` and `images` are **arrays**.

> **This endpoint no longer accepts the strings the sites used to post.** There is no
> `legacy_work_type` mapping table any more, so `workType` must match an organization's
> slug or display name — `"YourLocalJunkRemoval"` resolves, `"Junk Removal"` returns
> `404`. Point the frontends at `POST /api/orgs/{slug}/quotes` instead.

Other behaviour changes: an unrecognised `workType` is a `404` rather than silently
sending mail with no `From` address; the response's `to` field now contains the client's
email (it previously returned their last name, due to a mapper bug); and `message` is
now `"Request received"` rather than `"Email Sent Successfully !"`, which had stopped
being true once mail moved off the request thread. **Check no frontend string-matches
that message before deploying.**

### Errors

Failures return RFC 9457 `ProblemDetail`. `400` invalid body or unknown service,
`404` unknown organization slug, `409` data conflict, `422` service exists but is not
offered by that organization.

### Admin API

Every route below starts with `/api/admin` and requires an **ADMIN** bearer token.
Access is global across organizations. Anonymous callers receive 401; authenticated
members receive 403. The public `/api/orgs/**` routes never return admin quote or job lists.

| Resource | Endpoints | Behavior |
| :--- | :--- | :--- |
| Quotes | `GET/POST /quotes`, `GET/PATCH /quotes/{id}` | List/filter, enter phone leads, edit status, description and expiry |
| Quote items | `GET/POST /quotes/{quoteId}/items`, `PATCH/DELETE /quotes/{quoteId}/items/{itemId}` | Price, edit and remove lines |
| Jobs | `GET/POST /jobs`, `GET/PATCH /jobs/{id}` | Convert quotes or enter walk-in work; update status and schedule |
| Job items | `GET/POST /jobs/{jobId}/items`, `PATCH/DELETE /jobs/{jobId}/items/{itemId}` | Price and track individual services |
| Users | `GET/POST /users`, `GET/PATCH/DELETE /users/{id}`, `POST /users/{id}/verify` | Create accounts, change roles, force verification and delete |
| Organizations | `GET/POST /organizations`, `GET/PATCH /organizations/{id}` | Includes inactive businesses and mail settings |
| Organization services | `GET/PUT /organizations/{id}/services` | Includes inactive services; PUT replaces the set using `{"serviceIds":[1,2]}` |
| Service catalogue | `GET/POST /services`, `PATCH /services/{id}` | Includes inactive entries; create or edit catalogue services |

Quote and job lists accept `orgSlug`, `status`, and `clientEmail`; user lists accept
`role` and `verified`. These lists accept `page` (zero-based), `size`, and
`sort` (for example, `createdAt,desc`) and return
`{content, page, size, totalElements, totalPages}`. Quote/job lists contain summaries;
the individual GET endpoints contain client, location and line-item details.

**PATCH:** omitted or null fields stay unchanged, including nested mail fields.
Nullable values cannot be cleared with null. Any valid status enum is accepted; there
is no state machine. Quotes and jobs have no DELETE: use quote status `DECLINED` or
`EXPIRED`, and job status `CANCELLED`. Organizations and catalogue services are
retired with `{"active":false}`.

Create an admin quote with `{"orgSlug":"tcs","quote":{...}}`, where `quote` is the
public submission body shown above. **This path sends no email.** Public submissions
continue to send the confirmation and business notification after commit.

Create a job from a quote:

```json
{"quoteId":42,"scheduledAt":"2026-10-01T14:00:00Z"}
```

This copies the client, organization, location, description, and independent line
items with their prices. A second job for the same quote returns 409 naming the
existing job. For walk-in work, omit `quoteId` and send `orgSlug`, `client`,
`location`, and `services` instead. Each service accepts `serviceId`, `quantity`,
`unitPrice`, `description`, and job-service `status` (default `PENDING`).
Mixing the two creation modes returns 400. `organizationSlug` is also accepted as
an alias for `orgSlug` in creation bodies.

Nested item edits check that the item belongs to the addressed quote/job and return
404 otherwise. Adding a service the organization does not offer returns 422.
A quote can contain each service once (duplicates return 409); jobs may repeat a service.

User creation accepts `email`, `name`, `password`, `role`, and `verified`
(default true). PATCH accepts `role` and `verified`; the verify endpoint force-verifies
an account without SMTP. Responses never include password hashes. Administrators
cannot delete or demote themselves, or remove the last administrator (409).
Role changes apply on the next request because JWT authentication reloads database roles.

Organization creation requires `name`, `slug`, and `contactEmail`; `active`
defaults to true. PATCH accepts `name`, `contactEmail`, `active`, and
`mailSettings` (`mail` is an accepted alias). Mail settings accept `host`, `port`,
`username`, `passwordEnv`, `sslEnabled`, `starttlsEnabled`, `fromEmail`, and
`fromName`. When a host is configured, port, username, passwordEnv and fromEmail
must also be present in the resulting settings. Incomplete settings return 400 naming
the missing fields. `passwordEnv` is an environment-variable **name**, never a resolved
password. Organization responses expose it as `smtpPasswordEnv`.

### OpenAPI and Swagger UI

Open [Swagger UI](http://localhost:8080/swagger-ui.html) locally; the OpenAPI document
is at [`/v3/api-docs`](http://localhost:8080/v3/api-docs). Both are public when enabled.
Use **Authorize** with the access token from `POST /api/auth/login` to exercise admin routes.

`SWAGGER_ENABLED` defaults to `true` for local development. **Set
`SWAGGER_ENABLED=false` in production (including Render)** to disable both the
document and UI routes; they then return 404. The flag controls both
`springdoc.api-docs.enabled` and `springdoc.swagger-ui.enabled`.

## Database migrations

**Flyway owns the schema.** The application runs with `ddl-auto=validate`, so Hibernate
verifies the mappings against the migrated schema at startup and refuses to boot on a
mismatch.

- Migrations live in `sping-all-purpose-server/src/main/resources/db/migration`.
- **Never edit a migration that has been applied** — add a new `V<n>__` file.
- Column types in `V1` must match what Hibernate expects. They were derived from
  Hibernate's own schema export (`src/test/java/.../SchemaExporter.java`, a `main` you
  can re-run whenever the mappings change).

Migrating an existing database that predates Flyway is a **one-time manual wipe**,
deliberately not scripted as a migration:

```sql
DROP SCHEMA public CASCADE; CREATE SCHEMA public;
```

`V2` seeds the four live sites and their service catalogs, transcribed verbatim from
each site's quote form:

| Slug | Organization | Services | Business inbox |
| :--- | :--- | ---: | :--- |
| `yourlocalpaints` | YourLocalPaints | 4 | info@yourlocalservice.co |
| `yourlocalhandyman` | YourLocalHandyman | 6 | info@yourlocalservice.co |
| `tcs` | TCS (tcs-on.ca) | 10 | tcs.ontario@gmail.com |
| `yourlocaljunkremoval` | YourLocalJunkRemoval | 4 | info@yourlocalservice.co |

Service names must stay in step with the forms — a client can only submit a service
that exists in the catalog, so renaming a form option without a migration causes
rejected submissions. The catalog is global and scoped per org through
`organization_service`, which is what keeps genuinely different jobs apart:
Handyman's *Appliance Repair* vs JunkRemoval's *Appliance Removal*, Handyman's
*Junk Removal* vs the JunkRemoval org's own list, and Paints' *Deck & fence
staining/painting* vs TCS's *Deck & Fences*.

## Outbound mail

Each organization can send through its own SMTP account. Columns on `organization`
(`smtp_host`, `smtp_port`, `smtp_username`, `smtp_password_env`, `smtp_ssl_enabled`,
`smtp_starttls_enabled`, `from_email`, `from_name`), not a separate table — an org
sends through exactly one account at a time.

**Why it exists:** `From` used to be the single authenticated account for every org,
with the org's own address only in `Reply-To`. Under DMARC that is the pattern that
gets mail spam-foldered or rejected, and it is unfixable for an org whose address is on
a domain you don't sign — like TCS on `gmail.com`.

**Null means "use the global sender".** An org with `smtp_host` unset uses the
application-wide `spring.mail.*` configuration, which is the pre-existing behaviour.
A database `CHECK` rejects half-configured rows, because those only fail at send time,
after the quote is committed and the client has been told it was received.

**Passwords are not stored.** `smtp_password_env` holds the *name* of an environment
variable; the secret stays in the environment, so database dumps carry no credentials.
The cost is that onboarding an org with its own sender needs that variable added to the
deployment.

TCS is already configured this way by `V4__configure_tcs_mail_sender.sql`, so it
survives a database reset. All it needs from the environment is **`SMTP_PASS_TCS`**, set
to a Google *app password* (2-Step Verification must be on for the account — the option
does not appear otherwise). Without that variable TCS's business notification fails and
is logged; quotes are unaffected, since mail is sent after commit.

Onboarding another organization onto its own sender is an `UPDATE` in a new migration
plus one environment variable:

```sql
UPDATE organization SET smtp_host = '...', smtp_port = 587, smtp_username = '...',
    smtp_password_env = 'SMTP_PASS_SOMEORG', smtp_ssl_enabled = FALSE,
    smtp_starttls_enabled = TRUE, from_email = '...', from_name = '...'
WHERE slug = '...';
```

There is no failover: the configured account is used, and a failure is logged without
touching the already-committed quote.

Sending happens **after the transaction commits and on a background thread**, so neither
a slow SMTP host nor a failing one affects the client.

`QuoteSubmissionService` publishes a `QuoteSubmittedEvent` carrying the finished
`QuoteResponse` and the `Organization` — both assembled while the transaction is still
open. `QuoteEmailListener` picks it up with `@Async` + `@TransactionalEventListener(AFTER_COMMIT)`
and hands it to `EmailService`. There is no transaction and no repository in that path:
the event already holds everything the email needs, so nothing is ever detached and
nothing lazy-loads.

Two consequences worth knowing:

- `Organization` deliberately has **no collections**. That is what makes it safe to
  carry on an event and read after the session closes; adding a `@OneToMany` to it would
  break mail delivery at runtime rather than at compile time.
- `EmailService` takes DTOs, not entities, so it is a plain unit test with no Spring
  context and no database — see `EmailServiceTest`.

### Quote photos

The business notification is `multipart/mixed` with a plain-text and an HTML
alternative. Photos are **attached inline** (referenced by `cid:`) up to a **5MB** raw
budget; anything beyond it, or that cannot be read from S3, degrades to a presigned
link instead of being dropped.

Why a budget rather than attaching everything: MIME base64-encodes attachments, adding
about a third, and `emailTaskExecutor` runs up to 4 sends at once — each holding its
bytes plus an encoded copy. 5MB caps the worst case at roughly 4 × 5MB of raw photo
data in flight. Raise it only alongside the heap.

Sizes are checked with `headObject` before any download, so a quote with thirty photos
does not pull thirty files down to attach three.

**Presigned GET links last 7 days** — SigV4's hard maximum. There is no longer option
and no "never expires"; permanent access is exactly why photos are attached rather than
only linked. They were previously 60 minutes, which meant an evening quote had dead
photo links by morning.

The plain-text alternative lists every photo as a link, since text cannot render an
inline image. Note Outlook tends to also show inline images in its attachment tray —
cosmetic, and not controllable from the sending side.

Jakarta Mail defaults every socket timeout to **infinite**. `connectiontimeout`,
`timeout` and `writetimeout` are set (10s) both in `application.properties` and in the
per-org senders, or a host that accepts a connection then goes quiet would occupy a
mail thread forever.

The pool is deliberately small and bounded — 2 core, 4 max, 100 queued, `CallerRunsPolicy`
so a flood degrades to synchronous sending rather than dropping mail, and
`waitForTasksToCompleteOnShutdown` so in-flight emails finish on restart. Emails queued
at a hard kill are still lost; a real delivery guarantee would need an outbox table.

**You need one variable per distinct SMTP account, not per organization.** Today that
is one: the three `yourlocalservice.co` brands share a mailbox and leave their SMTP
columns NULL, so only TCS needs `SMTP_PASS_TCS`.

**Where this design runs out.** Env-var references are deliberately sized for a handful
of organizations — every new sending account needs a deploy to add its variable. Past
roughly ten, switch to one of:

- *If the orgs are all your own brands:* one provider (SES/Postmark) with every domain
  verified, and `From` set per message. The `smtp_*` credential columns disappear
  entirely; only `from_email` and `from_name` remain. Adding an org becomes an `INSERT`
  plus a DNS record.
- *If each org brings its own mailbox:* encrypt the password in the row (AES-GCM via a
  JPA `AttributeConverter`, master key in the environment). Adding an org becomes an
  `INSERT` with no deploy, at the cost of owning key rotation.

Either is a contained change — an `AttributeConverter` or dropping columns — not a
redesign, because the settings already live on `organization` rather than being
scattered through configuration.

## Getting started

### Prerequisites

Java 17+, Maven 3.8+, PostgreSQL 16+, and Docker (for the Testcontainers-backed tests).

### Environment variables

Variables with defaults are marked below; configure the remaining values before startup.

| Variable          | Description                | Example                                    |
| :---------------- | :------------------------- | :----------------------------------------- |
| `DB_URL`          | PostgreSQL connection URL  | `jdbc:postgresql://localhost:5432/yls`     |
| `DB_USER`         | Database username          | `postgres`                                 |
| `DB_PASS`         | Database password          | `secret`                                   |
| `EMAIL_SENDER`    | SMTP account (the `From`)  | `no-reply@example.com`                      |
| `SMTP_PASS`       | SMTP password              | `...`                                       |
| `SMTP_PORT`       | SMTP port                  | `465`                                       |
| `SMTP_HOST`       | SMTP host (default `smtp0001.neo.space`) | `smtp.gmail.com`              |
| `SMTP_SSL`        | Implicit TLS (default `true`) | `false` for port 587                     |
| `SMTP_STARTTLS`   | STARTTLS (default `false`) | `true` for port 587                         |
| `CORS_ALLOWED`    | Allowed origins, comma-separated | `http://localhost:3000`              |
| `AWS_ACCESS_KEY`  | AWS access key id          | `AKIA...`                                   |
| `AWS_SECRET_KEY`  | AWS secret access key      | `...`                                       |
| `AWS_REGION`      | Bucket region              | `us-east-2`                                 |
| `AWS_BUCKET_NAME` | S3 bucket for quote photos | `yls-uploads`                               |
| `SWAGGER_ENABLED` | Enable Swagger UI and OpenAPI (default `true`; set `false` in production) | `false` |
| `PORT`            | HTTP port (default `8080`) | `8080`                                      |
| `JWT_KEY`         | HMAC-SHA256 signing key, **32+ bytes** | a long random string          |
| `ADMIN_PASSWORD`  | Password for the seeded admin account | `...`                          |
| `VERIFICATION_BASE_URL` | Public URL verification links point at | `https://api.example.com` |
| `VERIFICATION_TTL_MINUTES` | Verification link lifetime (default `60`) | `60`             |

Plus one variable per organization that has its own SMTP account, named by that org's
`smtp_password_env` (e.g. `SMTP_PASS_TCS`). Orgs using the global sender need none.

Mail goes through `smtp0001.neo.space` on implicit SSL (`ssl.enable=true`,
`starttls.enable=false`), configured in `application.properties`.

### Running locally

```bash
docker run --name ylsdb -e POSTGRES_PASSWORD=secret -e POSTGRES_DB=yls -p 5432:5432 -d postgres:16
```

```bash
cd sping-all-purpose-server && ./mvnw clean verify
```

```bash
cd sping-all-purpose-server && ./mvnw spring-boot:run
```

Flyway applies migrations `V1` through `V6` on first start. Confirm with:

```sql
SELECT version, description, success FROM flyway_schema_history ORDER BY installed_rank;
```

### Tests

Unit tests and MVC security/OpenAPI tests run without a database. They cover quote
pricing and ownership, quote-to-job conversion, admin lockout guards, mail validation,
email-event behavior, all admin routes' 401/403 responses, and the Swagger off switch.

Run the checks that do not need Docker:

```bash
cd sping-all-purpose-server
./mvnw "-Dtest=*,!SchemaValidationTest,!QuoteSubmissionServiceTest,!LegacyQuoteEndpointTest,!SpingAllPurposeServerApplicationTests,!AdminWorkflowTest" verify
```

The five excluded integration test classes use real PostgreSQL. Run
`./mvnw verify` with Docker to include them using PostgreSQL 16 through
Testcontainers because identity columns, `timestamptz`, and partial unique indexes
are not faithfully reproduced by H2. `SchemaValidationTest` starts Flyway and
Hibernate validation against all twelve entity mappings.

Alternatively, run every test against a disposable local PostgreSQL database:

```bash
./mvnw -Dtest.database.url=jdbc:postgresql://localhost:5432/yls_test \
  -Dtest.database.username=postgres -Dtest.database.password=test_password verify
```

Create that test database first. The suite applies migrations and writes test data;
use a fresh database for each verification run. With no `test.database.url`, the suite
starts its own Testcontainers instance. `AdminWorkflowTest` exercises real JWT login,
quote creation and pricing, job conversion, independent copied prices, and paged reads
through the full HTTP/security/service/database stack with SMTP mocked.

For a local smoke check, log in as an administrator and request
`GET /api/admin/quotes?orgSlug=tcs` with the access token. Without a token it must
return 401; `GET /api/orgs/tcs/quotes` must return 405. Check the admin paths in
`/v3/api-docs`, then restart with `SWAGGER_ENABLED=false` and confirm it returns 404.

## Tech stack

- **Java 17**, **Spring Boot 4** — layered DI, convention over configuration.
- **PostgreSQL** — partial indexes, `timestamptz`, and identity columns are all used.
- **Flyway** — versioned, reviewable schema. Requires both `flyway-core` and
  `flyway-database-postgresql`; since Flyway 10 the Postgres support is a separate
  module and `flyway-core` alone fails at startup.
- **Spring Data JPA**, **Bean Validation**, **Lombok** (only `@Getter`/`@Setter`/
  `@NoArgsConstructor` on entities — `@Data`, `@ToString` and generated
  `equals`/`hashCode` traverse lazy associations and are deliberately avoided).
- **AWS SDK v2** — presigned S3 URLs for quote photos.
- **Spring Security** — stateless JWT, ADMIN URL rules and method authorization.

## Known gaps

1. Review CRUD, password reset/change, and organization-scoped staff permissions are
   not implemented. Admin-set passwords remain a temporary account-creation mechanism.
2. No rate limiting on the public quote funnel.
3. No currency field; quote totals are recomputed from current line items.
4. `expires_at` has no background sweeper.
5. The service catalogue is global: changing a shared service affects every organization
   that offers it.
6. Email runs asynchronously after commit but has no durable outbox for process crashes.

## Roadmap

1. Review API and account password management.
2. Rate limiting on quote submission.
3. Organization-scoped staff permissions when needed.
4. Durable outbound email delivery.

The pre-refactor design sketch is kept at [`docs/legacy/`](./docs/legacy/) for history;
the diagram above is the current model.
