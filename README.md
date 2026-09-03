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
web/        @RestControllerAdvice returning RFC 9457 ProblemDetail
email/ file/ configuration/                    supporting services, not aggregates
```

Line items are not aggregate roots — they have no controller and no service, because
they are created and deleted through their parent (`Quote` / `Job`) via cascade. Their
folders carry only `model/`, `repository/`, and (for `quotelineitem/`) the request and
response DTOs nested inside the quote payload.

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

### Upload a photo

`GET /api/upload/{name}` → `{ "url": "<presigned S3 PUT URL>" }`

PUT the file to that URL, then pass `{name}` in `pictureKeys` on the quote.

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
sending mail with no `From` address, and the response's `to` field now contains the
client's email (it previously returned their last name, due to a mapper bug).

### Errors

Failures return RFC 9457 `ProblemDetail`. `400` invalid body or unknown service,
`404` unknown organization slug, `409` data conflict, `422` service exists but is not
offered by that organization.

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

To move TCS onto its own free Gmail sender — set `SMTP_PASS_TCS` to a Google
**app password** (not the account password), then:

```sql
UPDATE organization SET smtp_host = 'smtp.gmail.com', smtp_port = 587,
    smtp_username = 'tcs.ontario@gmail.com', smtp_password_env = 'SMTP_PASS_TCS',
    smtp_ssl_enabled = FALSE, smtp_starttls_enabled = TRUE,
    from_email = 'tcs.ontario@gmail.com', from_name = 'TCS'
WHERE slug = 'tcs';
```

There is no failover: the configured account is used, and a failure is logged without
touching the already-committed quote. Sending is still synchronous, so a dead SMTP host
adds its connect timeout to the HTTP response — the reason to add `@Async` eventually.

## Getting started

### Prerequisites

Java 17+, Maven 3.8+, PostgreSQL 16+, and Docker (for the Testcontainers-backed tests).

### Environment variables

All are required and have no defaults except `PORT`; the application will not start
without them.

| Variable          | Description                | Example                                    |
| :---------------- | :------------------------- | :----------------------------------------- |
| `DB_URL`          | PostgreSQL connection URL  | `jdbc:postgresql://localhost:5432/yls`     |
| `DB_USER`         | Database username          | `postgres`                                 |
| `DB_PASS`         | Database password          | `secret`                                   |
| `EMAIL_SENDER`    | SMTP account (the `From`)  | `no-reply@example.com`                      |
| `SMTP_PASS`       | SMTP password              | `...`                                       |
| `SMTP_PORT`       | SMTP port                  | `465`                                       |
| `CORS_ALLOWED`    | Allowed origins, comma-separated | `http://localhost:3000`              |
| `AWS_ACCESS_KEY`  | AWS access key id          | `AKIA...`                                   |
| `AWS_SECRET_KEY`  | AWS secret access key      | `...`                                       |
| `AWS_REGION`      | Bucket region              | `us-east-2`                                 |
| `AWS_BUCKET_NAME` | S3 bucket for quote photos | `yls-uploads`                               |
| `PORT`            | HTTP port (default `8080`) | `8080`                                      |

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

Flyway applies `V1` and `V2` on first start. Confirm with:

```sql
SELECT version, description, success FROM flyway_schema_history ORDER BY installed_rank;
```

### Tests

Tests run against a real PostgreSQL via Testcontainers rather than H2 — the baseline
uses `GENERATED BY DEFAULT AS IDENTITY`, `timestamptz` and a partial unique index,
which H2's Postgres compatibility mode does not reproduce faithfully. `SchemaValidationTest`
is the important one: it boots the whole context with Flyway and `validate`, proving the
migrations and all ten mappings agree.

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
- **Spring Security** — currently permissive; see below.

## Known gaps

Tracked deliberately rather than silently:

1. **No authentication.** Every endpoint is `permitAll()`. This is why `Job`, `Review`
   and line-item prices have entities and repositories but no REST API, and why there
   is no `GET /quotes/{id}` — a sequential id on an unauthenticated read endpoint would
   let anyone enumerate clients' names, emails, phones and addresses.
2. **No rate limiting.** Quote submission is unauthenticated and sends two emails.
3. **`GET /api/upload/{name}`** issues a presigned PUT for a caller-controlled object
   key. Keys should be server-generated.
4. **No currency**, despite `Country` spanning the USA and Canada; and a quote stores
   no frozen total, so an accepted quote's total must be recomputed from lines that may
   have changed since.
5. **`expires_at` has no sweeper.** Expiry is computed on read.
6. **The service catalog is global** — two organizations share a `service` row, so
   editing a description affects both.
7. **Emails are sent synchronously** after the transaction commits. A failure can no
   longer roll back a saved quote, but the HTTP response still waits on SMTP.

## Roadmap

1. Authentication (JWT/OAuth2), then the Job and Review APIs behind it.
2. Rate limiting on quote submission.
3. Server-generated S3 keys.
4. Async email dispatch with a bounded executor.
5. OpenAPI/Swagger documentation.

The pre-refactor design sketch is kept at [`docs/legacy/`](./docs/legacy/) for history;
the diagram above is the current model.
