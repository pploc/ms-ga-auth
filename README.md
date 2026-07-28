# ms-ga-auth - Authorization Management Service

## Overview

| Property         | Value                                     |
| ---------------- | ----------------------------------------- |
| **Language**     | Java 23                                   |
| **Framework**    | Spring Boot 3.2.3                         |
| **Database**     | PostgreSQL 15 (Spring Data JPA + Flyway) |
| **Port**         | 8082                                      |
| **Base Path**    | `/auth`                                   |
| **Architecture** | Hexagonal Architecture (Ports & Adapters) |

**Purpose:** The Authorization Service manages RBAC - defining roles, permissions, and their mappings, and assigning roles to users.

---

## Quick Start

### Prerequisites
- Java 23+
- Docker & Docker Compose

### Run with Docker Compose

```bash
docker-compose up -d
```

### Run Locally

```bash
./gradlew bootRun
```

---

## API Endpoints

### Role Management
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/auth/roles` | Get all roles |
| POST | `/auth/roles` | Create role |
| GET | `/auth/roles/{id}` | Get role by ID |
| PUT | `/auth/roles/{id}` | Update role |
| DELETE | `/auth/roles/{id}` | Delete role |
| GET | `/auth/roles/name/{name}` | Get role by name |
| GET | `/auth/roles/{id}/permissions` | Get role permissions |
| PUT | `/auth/roles/{id}/permissions` | Replace role permissions |

### Permission Management
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/auth/permissions` | Get all permissions |
| POST | `/auth/permissions` | Create permission |
| GET | `/auth/permissions/{id}` | Get permission by ID |
| PUT | `/auth/permissions/{id}` | Update permission |
| DELETE | `/auth/permissions/{id}` | Delete permission |
| GET | `/auth/permissions/resource/{resource}/action/{action}` | Get permission by resource + action |

### User Role Management
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/auth/users/{userId}/roles` | Get user's roles |
| POST | `/auth/users/{userId}/roles` | Assign role to user |
| DELETE | `/auth/users/{userId}/roles/{roleId}` | Revoke role |
| GET | `/auth/users/{userId}/roles/{roleId}/check` | Check whether a user holds a role |
| GET | `/auth/users/{userId}/roles/with-permissions` | Get roles with permissions |

---

## Default Roles

- **SUPER_ADMIN** - Full system access (system role)
- **GYM_ADMIN** - Gym management (system role)
- **TRAINER** - Trainer permissions (system role)
- **MEMBER** - Self-service (system role)
- **STAFF** - Front desk operations (system role)

---

## Local test environment

Brings up the service's dependencies in Docker so you can run the app from your IDE and drive it
with Postman. The app itself is deliberately *not* in the stack — you want to restart and debug it
without rebuilding an image.

```bash
./gradlew startTestEnvironment   # Postgres + Kafka + Schema Registry + Kafka UI
./gradlew bootRun                # the service, on http://localhost:8082
# ... drive it with Postman ...
./gradlew stopTestEnvironment    # stop, keeping data
./gradlew resetTestEnvironment   # stop and wipe volumes: empty database, empty topics
./gradlew testEnvironmentStatus  # container health
./gradlew testEnvironmentLogs    # last 200 lines from every container
```

| Service | Address | Notes |
|---|---|---|
| Postgres | `localhost:5434` | `auth_db` / `postgres` / `postgres` |
| Kafka | `localhost:9092` | Single broker, KRaft (no ZooKeeper) |
| Schema Registry | `http://localhost:8181` | `BACKWARD` compatibility, as in production |
| Kafka UI | `http://localhost:8100` | Browse events; Avro decoded via the registry |

These match the defaults in `application.yml`, so `bootRun` needs no extra configuration, and
Flyway applies the migrations on startup.

One broker, replication factor 1, so `bootRun` creates the topic to match. That is enough to drive
the API; it is not a test of durability — `acks=all` against a single broker means "the one broker
wrote it". Production should run 3 replicas with `min.insync.replicas` 2. Point `bootRun` at a real
cluster with `-PkafkaReplicas=3 -PkafkaMinIsr=2`.

### Driving it with curl

> **Tokens first (ADR-0003).** The service verifies every bearer token itself against
> `ms-ga-identifier`'s JWKS (`JWKS_URI`, default
> `http://localhost:8081/identity/.well-known/jwks.json`), so every protected call below needs
> `-H "$AUTH"` where `AUTH="Authorization: Bearer <token from a locally running identifier>"`.
> Without a reachable identifier everything but the bootstrap route
> (`GET /auth/users/{id}/roles/with-permissions`, token-free by design) answers
> `401 UNAUTHENTICATED`. The header is omitted from the transcripts below to keep them readable.

Everything below is copy-pasteable. `jq` is only used to pull ids out of responses, and `uuidgen`
to make one up — Git Bash on Windows has neither, so either run these from WSL or substitute:

```bash
# Git Bash fallbacks
uuidgen() { powershell -NoProfile -Command "[guid]::NewGuid().ToString()" | tr -d '\r'; }
# ...or just paste a literal UUID; nothing here depends on it being unique except across runs.
```

```bash
BASE=http://localhost:8082

# Is it up?
curl -s $BASE/auth/roles | jq

# Create a role and a couple of permissions, keeping their ids
ROLE=$(curl -s -X POST $BASE/auth/roles \
  -H 'Content-Type: application/json' \
  -d '{"name":"FRONT_DESK","description":"Reception staff"}' | jq -r .id)

READ=$(curl -s -X POST $BASE/auth/permissions \
  -H 'Content-Type: application/json' \
  -d '{"resource":"booking","action":"read"}' | jq -r .id)

CREATE=$(curl -s -X POST $BASE/auth/permissions \
  -H 'Content-Type: application/json' \
  -d '{"resource":"booking","action":"create"}' | jq -r .id)

# Grant them to the role (full replacement, not a merge)
curl -s -X PUT $BASE/auth/roles/$ROLE/permissions \
  -H 'Content-Type: application/json' \
  -d "{\"permissionIds\":[\"$READ\",\"$CREATE\"]}" | jq

# Assign the role to a user, then resolve what ms-ga-identifier would embed in the JWT
USER=$(uuidgen)
curl -s -X POST $BASE/auth/users/$USER/roles \
  -H 'Content-Type: application/json' \
  -d "{\"roleId\":\"$ROLE\"}" | jq

curl -s $BASE/auth/users/$USER/roles/with-permissions | jq
# => {"userId":"...","roles":["FRONT_DESK"],"permissions":["booking:read","booking:create"]}
```

Those two assignment calls each published an Avro event — see them at
`http://localhost:8100` under topic `auth.events`, decoded through the registry.

**Error responses.** Every failure uses the same envelope, so these are worth a look:

```bash
curl -s $BASE/auth/roles/$(uuidgen) | jq              # 404 ROLE_NOT_FOUND
curl -s $BASE/auth/roles/not-a-uuid | jq              # 400 MALFORMED_REQUEST
curl -s -X POST $BASE/auth/roles \
  -H 'Content-Type: application/json' -d '{"name":""}' | jq   # 400 with fieldErrors
curl -s -X POST $BASE/auth/roles \
  -H 'Content-Type: application/json' \
  -d '{"name":"FRONT_DESK"}' | jq                     # 409 ROLE_ALREADY_EXISTS

# Correlation id: send your own and it comes back, and appears as traceId
curl -si $BASE/auth/roles/$(uuidgen) -H 'X-Correlation-Id: my-trace-1' | grep -i correlation
```

**Idempotency.** The difference is the point — same request twice, with and without a key:

```bash
KEY=$(uuidgen)
BODY='{"name":"NIGHT_STAFF"}'

# With a key: 201, then the identical 201 replayed
curl -si -X POST $BASE/auth/roles -H "Idempotency-Key: $KEY" \
  -H 'Content-Type: application/json' -d "$BODY" | head -1
curl -si -X POST $BASE/auth/roles -H "Idempotency-Key: $KEY" \
  -H 'Content-Type: application/json' -d "$BODY" | grep -iE '^(HTTP|Idempotency-Replayed)'

# Reusing the key for a different body is refused rather than guessed at
curl -s -X POST $BASE/auth/roles -H "Idempotency-Key: $KEY" \
  -H 'Content-Type: application/json' -d '{"name":"SOMETHING_ELSE"}' | jq .code
# => "IDEMPOTENCY_KEY_REUSED"
```

**At-least-once.** Stop the broker and a mutating call fails *and rolls back*, rather than
committing a change nobody hears about:

```bash
docker stop ms-ga-auth-dev-kafka
curl -s -X POST $BASE/auth/users/$(uuidgen)/roles \
  -H 'Content-Type: application/json' -d "{\"roleId\":\"$ROLE\"}" | jq
# => 503 EVENT_PUBLISH_FAILED, and the assignment was not persisted
docker start ms-ga-auth-dev-kafka
```

That is the contract working, not a bug. Retry the same request with an `Idempotency-Key` once the
broker is back.

### Through the gateway

HAProxy and Kong sit behind a compose profile, because most of the time you want to hit the
service directly on `:8082`. Bring them up to test what callers actually traverse:

```bash
./gradlew startGateway     # test environment + HAProxy + Kong
```

| | | |
|---|---|---|
| HAProxy | `http://localhost:8000` | Public entry point; TLS terminates here |
| HAProxy stats | `http://localhost:8404` | |
| Kong | `http://localhost:8010` | Direct, bypassing HAProxy, for comparison |
| Kong admin | `http://localhost:8011` | Read-only (DB-less), localhost only |

Config lives in `deploy/kong/kong.yml` and `deploy/haproxy/haproxy.cfg`.

`kong.yml` is split into two marked sections, and the distinction matters:

- **Section 1 — owned by this repo.** The `ms-ga-auth` service entity, its routes, and the plugins
  scoped to them. Entity names are prefixed `ms-ga-auth-` so twelve fragments merge cleanly. This
  is the part CI should publish to the platform gateway.
- **Section 2 — owned by the platform.** The shared JWT verification credential and the global
  plugins. Every service needs them, so they belong in the platform config once. They are
  duplicated here only so `startGateway` boots a working Kong standalone — if twelve repos each
  shipped them, the merge would fail on the duplicate consumer key and duplicate global plugins.

Merging is decK's job in CI, not copy-paste:

```bash
deck file merge deploy/kong/kong.yml ../ms-ga-*/deploy/kong/kong.yml -o gateway.yml
deck gateway diff gateway.yml && deck gateway sync gateway.yml
```

...with section 2 taken from the platform repo rather than from any service. Until that pipeline
exists, section 2 is duplicated per service and will drift.

Everything under `/auth` needs a JWT carrying `role:manage` at the gateway — and the service
verifies the token again itself against `ms-ga-identifier`'s JWKS (ADR-0003), so neither layer
alone is the whole story:

```bash
curl -s http://localhost:8000/auth/roles | jq
# => 401 UNAUTHENTICATED, in the same envelope the service returns
```

Kong's rejections are shaped like the service's `ErrorResponse` deliberately, so a client sees one
error format regardless of which layer refused it.

**The login endpoint is the exception, and it is the constraint that shapes the whole config.**
`ms-ga-identifier` calls `GET /auth/users/{id}/roles/with-permissions` *while minting a token*, so
it has no JWT to present. Putting the `jwt` plugin across all of `/auth` deadlocks login: no
permissions without a token, no token without permissions. It gets its own route with a service
key and a source-IP restriction instead:

```bash
curl -s http://localhost:8000/auth/users/<uuid>/roles/with-permissions \
  -H 'X-Service-Key: dev-identifier-service-key' | jq
```

Three settings that are load-bearing rather than boilerplate:

- **`retries: 0` on the Kong service.** Kong retries five times by default, which would turn one
  timed-out `POST /auth/users/{id}/roles` into several assignments — the exact duplicate
  `Idempotency-Key` exists to prevent. Retrying is the client's decision, made safe by that header.
- **Timeouts step upward: 25s publish → 35s Kong → 40s HAProxy.** A mutating call blocks on the
  Kafka ack before responding. Cut a timeout below that and the request is killed mid-decision, so
  the caller gets a 504 and cannot tell whether the change committed.
- **`X-User-*` headers are stripped on the way in, at both layers.** The service treats them as
  verified identity, so without stripping, anyone who can reach the gateway could assert their own
  permissions.

Not included here, on purpose: the Redis JTI blacklist check. It runs against
`ms-ga-identifier`'s Redis and applies to every service, so it belongs in the platform config as a
global plugin, not copied into each fragment. `kong.yml` carries a sketch and the fail-closed
trade-off it implies.

### Kubernetes

```bash
helm upgrade --install ms-ga-auth deploy/helm/ms-ga-auth -n gymapi --create-namespace
helm diff upgrade ms-ga-auth deploy/helm/ms-ga-auth     # review first, with helm-diff
helm rollback ms-ga-auth                                 # if it goes wrong
```

```
deploy/helm/ms-ga-auth/
├── Chart.yaml
├── values.yaml          ← everything specific to this service
└── templates/           ← generic; mentions this service nowhere
    ├── _helpers.tpl  configmap.yaml  deployment.yaml  service.yaml
    ├── resilience.yaml (PDB + HPA)   networkpolicy.yaml  gateway.yaml
    └── NOTES.txt
```

**The split is deliberate.** Six sibling services need this same shape, so `templates/` is driven
entirely by values — the config, the network peers, the gateway plugins, even the authorization
Lua all live in `values.yaml`, and the helper defines are prefixed `gymapi.` rather than
`ms-ga-auth.`. When the second service needs it, `templates/` moves to a platform chart published
to an OCI registry, each service keeps its `values.yaml`, and this repo drops from 12 files to 1.
Doing that now, with one service, would be guessing at which parts actually vary.

Two things the chart does better than the kustomize manifests it replaces:

- **`checksum/config` is computed**, so a config change genuinely rolls the pods. Under kustomize
  it was a placeholder CI had to fill in.
- **`gateway.enabled: false`** on a cluster without Gateway API and Kong CRDs, instead of telling
  you to comment a line out of `kustomization.yaml`.

Secrets are never templated. `deploy/secret.example.yaml` documents the expected keys; supply the
Secret from External Secrets, Sealed Secrets, or whatever the platform runs.

**The login endpoint has no route, on purpose.** `ms-ga-identifier` calls
`/auth/users/{id}/roles/with-permissions` while minting a token, so it cannot require a JWT. In
docker-compose that meant a service key plus an IP allowlist. Here it simply is not published:
identifier reaches the ClusterIP Service directly, and the NetworkPolicy decides who may. An
endpoint with no route cannot be reached from outside, which beats any allowlist.

**The NetworkPolicy is load-bearing, not hygiene.** The service verifies JWTs itself, but the
bootstrap route (`/auth/users/{id}/roles/with-permissions`) is deliberately token-free, so "who
can open a connection" is still part of its security model. It default-denies and allows exactly
three ingress sources — the gateway, ms-ga-identifier, and Prometheus — plus egress to DNS,
Postgres, Kafka, the Schema Registry and ms-ga-identifier's JWKS endpoint.

**Timeouts and shutdown form a chain**, and every link has to be longer than the one inside it:

```
Kafka publish 25s  <  spring.lifecycle.timeout-per-shutdown-phase 30s
                   <  terminationGracePeriodSeconds 45s
```

Break it and a rolling update SIGKILLs a pod mid-publish, leaving the caller unable to tell whether
the change committed. `server.shutdown: graceful` and the 5s `preStop` sleep are what make a
rolling update lossless.

**The startup probe exists for Flyway.** Migrations run during startup, so first boot against a
fresh database is slow. Liveness does not begin until the startup probe passes, so a long
migration cannot be mistaken for a hung process and killed halfway.

**`gateway.yaml` and `deploy/kong/kong.yml` express the same policy twice** — KIC assembles config
from CRDs, DB-less Kong reads a declarative file. Change one, change the other. If the platform is
Kubernetes-only, delete `kong.yml` and run KIC locally instead; the CRDs are the canonical form,
and KIC's per-service merging is exactly what `kong.yml`'s fragment problem was asking for.

### Postman

Import the OpenAPI document rather than hand-writing requests — **Import → Link →**
`http://localhost:8082/v3/api-docs`. Every endpoint, example and error response comes with it, and
it stays current as the spec changes.

**One thing that will confuse you otherwise:** only SUPER_ADMIN is seeded with permissions, so
`GET /auth/users/{id}/roles/with-permissions` returns empty arrays for MEMBER, TRAINER, GYM_ADMIN
and STAFF until you grant some yourself, as above. See the known gaps.

---

## Testing

```bash
./gradlew test           # unit tests — fast, everything mocked
./gradlew componentTest  # the whole application over HTTP against H2
./gradlew check          # both, plus Spotless and the coverage gate
```

**Unit tests** (`src/test/java`) cover classes in isolation with Mockito, plus `@WebMvcTest` slices
for the controllers.

**Component tests** (`src/componentTest/java`) boot the real Spring context and drive it through
MockMvc. Nothing between the socket and the database is stubbed — the correlation-id filter, the
security chain, the idempotency filter, real controllers, real services and real JPA all take part.
Only two things are swapped: Postgres becomes H2, and `KafkaTemplate` is mocked so the broker can be
made to fail on demand. They caught two bugs the mocked unit tests could not see: role assignment
violating a NOT NULL constraint, and system roles reading back as non-system.

Coverage is measured across both suites and enforced by `jacocoTestCoverageVerification`
(95% instructions, 90% branches), which `check` depends on. Excluded as boilerplate: OpenAPI- and
MapStruct-generated code, JPA entities, domain records, Spring configuration and the main class.

**Known gap:** the Flyway migrations are Postgres-specific, so the component tests build their
schema from the entity mappings instead of running them. Verifying the migrations themselves needs
Testcontainers.

---

## OpenAPI Documentation

`api/openapi/auth-api.yaml` is the source of truth for every request and response
shape. `./gradlew openApiGenerate` turns it into the DTOs under
`build/generated/openapi` — generated code is never committed, and the task runs automatically
before `compileJava`.

Browse the live docs at `http://localhost:8082/swagger-ui.html` (raw document:
`/v3/api-docs`).

---

## Error Handling

Every non-2xx response uses one envelope:

```json
{
  "timestamp": "2026-07-26T09:15:22.481Z",
  "status": 400,
  "error": "Bad Request",
  "code": "VALIDATION_FAILED",
  "message": "Request validation failed for 1 field",
  "path": "/auth/roles",
  "traceId": "4f1c9b02-6f8e-4f2f-9d0c-2a7d5f9c1b3e",
  "fieldErrors": [
    { "field": "name", "rejectedValue": "", "message": "must not be blank" }
  ]
}
```

Branch on `code`, not on `message` (human-readable, may change) or on the status alone (several
codes share a status). `fieldErrors` is present only for `VALIDATION_FAILED`.

| Code | Status | Meaning |
|------|--------|---------|
| `VALIDATION_FAILED` | 400 | A body field broke a constraint; see `fieldErrors` |
| `MALFORMED_REQUEST` | 400 | Unparseable JSON, or a path/query value of the wrong type |
| `UNAUTHENTICATED` | 401 | Missing, malformed or expired bearer token |
| `ACCESS_DENIED` | 403 | Authenticated but not permitted |
| `SYSTEM_ROLE_IMMUTABLE` | 403 | The target is a seeded system role, read-only through this API |
| `ROLE_NOT_FOUND` | 404 | No role with that id or name |
| `PERMISSION_NOT_FOUND` | 404 | No permission with that id or `resource:action` pair |
| `USER_ROLE_NOT_FOUND` | 404 | The user does not hold that role |
| `ENDPOINT_NOT_FOUND` | 404 | No such route |
| `METHOD_NOT_ALLOWED` | 405 | Route exists, method does not |
| `ROLE_ALREADY_EXISTS` | 409 | Role name is taken |
| `PERMISSION_ALREADY_EXISTS` | 409 | `resource:action` pair is taken |
| `USER_ROLE_ALREADY_ASSIGNED` | 409 | The user already holds that role |
| `DATA_INTEGRITY_VIOLATION` | 409 | A database constraint the application did not pre-check |
| `IDEMPOTENCY_KEY_REUSED` | 409 | The `Idempotency-Key` was already used for a different request |
| `IDEMPOTENT_REQUEST_IN_PROGRESS` | 409 | An earlier attempt with this key is still running |
| `UNSUPPORTED_MEDIA_TYPE` | 415 | Body was not `application/json` |
| `INTERNAL_ERROR` | 500 | Unhandled failure; the cause is in the logs under `traceId` |
| `EVENT_PUBLISH_FAILED` | 503 | The change was rolled back because Kafka did not acknowledge the event — retry |

The codes live in `domain/exception/ErrorCode.java` and are published in the OpenAPI spec;
`ErrorResponseFactoryTest` fails the build if the two drift apart.

### Retries and idempotency

Domain events are published with **at-least-once** delivery. The service waits for the broker to
acknowledge the write and rolls the change back if it does not arrive, answering
**503 `EVENT_PUBLISH_FAILED`**. A committed RBAC change is therefore never silently unannounced —
at the cost of a consumer occasionally seeing the same event twice.

The producer is configured to match (`application.yml`):

| Setting | Value | Why |
|---|---|---|
| `acks` | `all` | The write is on every in-sync replica before it is acknowledged |
| `enable.idempotence` | `true` | The broker discards duplicates caused by the producer's own retries |
| `retries` | unbounded | Bounded in practice by `delivery.timeout.ms` |
| `delivery.timeout.ms` | 20s | The real retry budget |
| `max.in.flight.requests.per.connection` | 5 | Required for idempotence to hold ordering |
| `gymapi.events.min-insync-replicas` | 1 (dev) | **Raise to 2 in production** — `acks=all` with one in-sync replica is not durable against losing a broker |

Because the service now *asks* clients to retry, retrying has to be safe. Send an
`Idempotency-Key` header (1–255 printable ASCII characters, typically a UUID) on any
POST/PUT/PATCH/DELETE:

```bash
curl -X POST http://localhost:8082/auth/users/$USER/roles \
  -H 'Content-Type: application/json' \
  -H "Idempotency-Key: $(uuidgen)" \
  -d '{"roleId": "..."}'
```

The response is recorded against that key for 24 hours. A retry with the same key replays it
verbatim, with `Idempotency-Replayed: true`, without touching the database or the broker again.
Three cases are refused rather than guessed at:

| Situation | Result |
|---|---|
| Same key, different method/path/body | `409 IDEMPOTENCY_KEY_REUSED` |
| Retry while the first attempt is still running | `409 IDEMPOTENT_REQUEST_IN_PROGRESS` |
| Malformed key | `400 MALFORMED_REQUEST` |

Responses of 500 and above are deliberately **not** recorded, so retrying after one genuinely
re-runs the request. Requests without the header behave exactly as before.

Consumers of `auth.events` should expect redelivery: every payload carries a unique `eventId`
(also set as a Kafka record header) to de-duplicate on, and records are keyed by aggregate id so
events about one user or role stay ordered on one partition.

Kafka's producer-level idempotence does **not** cover any of this — it only de-duplicates the
producer client's own retries within a single `send()`. It cannot tell that two HTTP requests mean
the same thing, and it has no opinion about the database row written alongside.

**Known gap:** the broker ack and the database commit are still two separate commits. If the
process dies between them the event is out but the change is not. Closing that needs a
transactional outbox.

### Correlation ids

Every response carries an `X-Correlation-Id` header, echoed from the request when you send one and
generated otherwise. The same value appears as `traceId` in error bodies and as `correlation_id` in
the service logs — quote it when reporting a problem. 500 responses deliberately return a generic
message; the real cause is only ever logged.

---

## Kafka Events

Events are published to `auth.events` as **Avro**, serialized through the Confluent Schema
Registry. The contract is `api/avro/AuthEvent.avsc` — that file, not any Java class, is what
the analytic and notification services consume. `./gradlew generateAvroJava` turns it into
`SpecificRecord` classes under `build/generated-main-avro-java`; generated code is not committed,
same as the OpenAPI DTOs.

One envelope carries a union of the three payloads:

```
AuthEvent
├── eventId     string     — unique per event, stable across redeliveries
├── occurredAt  timestamp  — when the change committed, UTC
└── payload     union of
    ├── RoleAssigned      { userId, roleId, assignedBy? }
    ├── RoleRevoked       { userId, roleId }
    └── PermissionChanged { roleId, roleName, changeType }
```

A union rather than three topics or a type string means a consumer switches on the branch, so an
unhandled event type is a compile error rather than a silent no-op.

Each message is prefixed with the id of the schema it was written against, so a consumer resolves
the exact writer schema instead of guessing. The registry is configured `BACKWARD` compatible: a
schema change that would break existing consumers is refused **at publish time** rather than
surfacing as a parse error in someone else's service days later.

Records are keyed by aggregate id — the user for assignment events, the role for permission
changes — so everything touching one entity stays ordered on one partition. Two record headers,
`eventId` and `eventType`, let a consumer skip a redelivery or route a message without
deserializing the body.

**Evolving the schema:** additive, defaulted fields are backward compatible and safe. Removing or
renaming a field, or adding an enum symbol consumers do not know, is not. Set
`SCHEMA_AUTO_REGISTER=false` outside development and publish schemas from CI, so an incompatible
change is caught in review rather than by whichever pod deploys first.

Local registry: `http://localhost:8181` (8081 inside the compose network — the host port avoids
clashing with ms-ga-identifier).

---

## Technology Stack

- Spring Boot 3.2.3
- Spring Data JPA
- Spring Security + JWT
- Spring Kafka
- PostgreSQL 15
- Flyway
- MapStruct
- Lombok
- OpenAPI 3.0
