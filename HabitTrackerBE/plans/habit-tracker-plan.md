# Habit Tracker — Implementation Plan

## Stack
- Java 25, Spring Boot 3.5.x, Maven
- H2 (file-based, not in-memory, for persistence across restarts)
- Spring Data JPA (JPQL-only custom queries, no derived-method magic)
- webauthn4j (+ webauthn4j-spring-security optional, but plan uses raw webauthn4j for full ceremony control)
- Bean Validation (jakarta.validation) on records
- springdoc-openapi with `@ParameterObject` for GET query params

## Package structure
```
com.habittracker
├── config          (SecurityConfig, WebAuthnConfig, OpenApiConfig)
├── user
│   ├── User.java, Gender.java (enum)
│   ├── UserController, UserService, UserRepository
│   └── dto: UserCreateRequest, UserUpdateRequest, UserResponse
├── auth (webauthn)
│   ├── WebauthnCredential.java
│   ├── AuthController, WebauthnService, WebauthnCredentialRepository
│   ├── ChallengeStore (in-memory TTL cache, not persisted)
│   └── dto: RegistrationOptionsRequest, RegistrationVerifyRequest,
│            LoginOptionsRequest, LoginVerifyRequest
├── habit
│   ├── Habit.java
│   ├── HabitController, HabitService, HabitRepository
│   └── dto: HabitCreateRequest, HabitUpdateRequest, HabitResponse
├── habitfield
│   ├── HabitField.java, HabitFieldType.java (enum: NUMBER, TEXT, BOOLEAN, DURATION)
│   ├── HabitFieldController, HabitFieldService, HabitFieldRepository
│   └── dto: HabitFieldCreateRequest, HabitFieldUpdateRequest, HabitFieldResponse
└── entry
    ├── Entry.java
    ├── EntryController, EntryService, EntryRepository
    └── dto: EntryCreateRequest, EntryResponse, ConsistencyQueryParams, ConsistencyResponse
```

## Schema

### `app_user`
| column | type | notes |
|---|---|---|
| user_seq_id | BIGINT PK, identity | |
| display_name | VARCHAR(100) NOT NULL | |
| email | VARCHAR(255) UNIQUE NOT NULL | login identifier for WebAuthn user handle lookup |
| gender | VARCHAR(20) | enum: MALE, FEMALE, OTHER, UNSPECIFIED |
| date_of_birth | DATE NOT NULL | age derived from this, not stored separately (avoids stale data) |
| weight_kg | DECIMAL(5,2) | nullable, optional |
| height_cm | DECIMAL(5,2) | nullable, optional |
| created_at | TIMESTAMP NOT NULL | |
| updated_at | TIMESTAMP NOT NULL | |

Decision: drop separate "age" column — derive from `date_of_birth` in DTO/service to avoid data drift. Flag this if you want age stored instead.

### `webauthn_credential`
| column | type | notes |
|---|---|---|
| credential_seq_id | BIGINT PK, identity | |
| user_seq_id | BIGINT FK -> app_user | |
| credential_id | VARCHAR(512) UNIQUE NOT NULL | base64url, from authenticator |
| public_key_cose | VARCHAR(2048) NOT NULL | base64 COSE key |
| sign_count | BIGINT NOT NULL | replay-attack counter |
| aaguid | VARCHAR(64) | authenticator model id |
| transports | VARCHAR(100) | comma-joined: usb,nfc,ble,internal |
| created_at | TIMESTAMP NOT NULL | |

### `habit`
| column | type | notes |
|---|---|---|
| habit_seq_id | BIGINT PK, identity | |
| user_seq_id | BIGINT FK -> app_user NOT NULL | |
| name | VARCHAR(100) NOT NULL | |
| description | VARCHAR(500) | nullable |
| active | BOOLEAN NOT NULL DEFAULT TRUE | soft-disable instead of delete |
| created_at | TIMESTAMP NOT NULL | |

### `habit_field`
| column | type | notes |
|---|---|---|
| habit_field_seq_id | BIGINT PK, identity | |
| habit_seq_id | BIGINT FK -> habit NOT NULL | |
| field_name | VARCHAR(100) NOT NULL | if not given at creation, service defaults to habit.name |
| field_type | VARCHAR(20) NOT NULL | enum `HabitFieldType`: NUMBER, TEXT, BOOLEAN, DURATION — stored as STRING via `@Enumerated(EnumType.STRING)` |
| unit | VARCHAR(30) | e.g. "kg", "min", "reps"; nullable for TEXT/BOOLEAN |
| created_at | TIMESTAMP NOT NULL | |

### `entry`

Revised: one row per `(habit_field_seq_id, entry_date)` — no duplicate rows for same day. A field like "water intake" that accumulates through the day gets its single row's value replaced/updated, not a new row inserted. `updated_at`/`updated_by` become the source of truth for "what is today's final value and who last touched it."

| column | type | notes |
|---|---|---|
| entry_seq_id | BIGINT PK, identity | |
| habit_field_seq_id | BIGINT FK -> habit_field NOT NULL | |
| entry_value | VARCHAR(255) NOT NULL | stored as string, parsed/validated per `HabitFieldType` in service layer |
| entry_date | DATE NOT NULL | natural key component — the day this entry belongs to |
| entry_timestamp | TIMESTAMP NOT NULL | first-created instant, immutable after insert |
| updated_at | TIMESTAMP NOT NULL | bumped on every upsert — source of truth for "latest" |
| updated_by | BIGINT FK -> app_user NOT NULL | who last wrote this row, from JWT principal |

Constraint: `UNIQUE (habit_field_seq_id, entry_date)` — enforces the one-row-per-day rule at the DB level, not just in service logic.

## HabitFieldType enum (dual-level, per your requirement)
```java
public enum HabitFieldType {
    NUMBER, TEXT, BOOLEAN, DURATION
}
```
- Column level: `@Enumerated(EnumType.STRING)` on `HabitField.fieldType`.
- Service/API level: same enum exposed in `HabitFieldResponse.fieldType` so frontend gets a stable string contract (`"NUMBER"`, `"TEXT"`, `"BOOLEAN"`, `"DURATION"`) to drive input widgets.

## WebAuthn flow (webauthn4j)
1. `POST /api/auth/webauthn/registration/options` — body: `{email, displayName}` (if new user) → generates challenge, stores in `ChallengeStore` keyed by email, returns `PublicKeyCredentialCreationOptions`.
2. `POST /api/auth/webauthn/registration/verify` — body: attestation response + email → verifies via webauthn4j, creates `User` + `WebauthnCredential`, returns `UserResponse`.
3. `POST /api/auth/webauthn/login/options` — body: `{email}` → challenge + allowCredentials list from stored credentials.
4. `POST /api/auth/webauthn/login/verify` — body: assertion response + email → verifies signature/counter, issues session (Spring Session or JWT — **needs your call, see Open Questions**).

`ChallengeStore`: in-memory `ConcurrentHashMap<String, ChallengeEntry>` with a scheduled sweep for TTL expiry (5 min). Not a DB entity — ephemeral by nature.

## Consistency endpoint

`GET /api/habit-fields/{habitFieldSeqId}/consistency`

Logic (per your answer: "any entry exists that day = completed"; simplified further now that `entry_date` is unique per field per day, so no DISTINCT needed):
1. Find `habitField` by id (404 if absent).
2. Find earliest `entry_date` for that field (`MIN(entryDate)` via JPQL) → `inceptionDate`.
3. `windowStart = max(inceptionDate, today.minusWeeks(4))`.
4. JPQL: `SELECT e.entryDate FROM Entry e WHERE e.habitField.habitFieldSeqId = :id AND e.entryDate >= :windowStart ORDER BY e.entryDate`.
5. Return `ConsistencyResponse(habitFieldSeqId, windowStart, windowEnd, completedDates: List<LocalDate>)`.

No value-threshold logic needed anywhere (per your "any entry" answer) — simplifies service to a pure existence check, no per-type branching.

## REST API surface

### User
| Method | Path | Body/Params | Notes |
|---|---|---|---|
| GET | `/api/users/{userSeqId}` | — | |
| PUT | `/api/users/{userSeqId}` | `UserUpdateRequest` | partial profile update |

(User creation happens via WebAuthn registration verify, not a standalone POST.)

### Habit
| Method | Path | Body/Params |
|---|---|---|
| POST | `/api/habits` | `HabitCreateRequest { userSeqId, name, description? }` |
| GET | `/api/habits` | `@ParameterObject HabitQueryParams { userSeqId, activeOnly? }` |
| GET | `/api/habits/{habitSeqId}` | — |
| PUT | `/api/habits/{habitSeqId}` | `HabitUpdateRequest` |
| DELETE | `/api/habits/{habitSeqId}` | — (soft delete: sets `active=false`) |

### HabitField
| Method | Path | Body/Params |
|---|---|---|
| POST | `/api/habits/{habitSeqId}/fields` | `HabitFieldCreateRequest { fieldName?, fieldType, unit? }` |
| GET | `/api/habits/{habitSeqId}/fields` | — |
| PUT | `/api/habit-fields/{habitFieldSeqId}` | `HabitFieldUpdateRequest` |
| DELETE | `/api/habit-fields/{habitFieldSeqId}` | — |

### Entry
| Method | Path | Body/Params |
|---|---|---|
| PUT | `/api/habit-fields/{habitFieldSeqId}/entries` | `EntryUpsertRequest { entryValue, entryDate? }` (default today) — **upsert**: finds row by `(habitFieldSeqId, entryDate)`, creates if absent, else updates `entryValue`/`updatedAt`/`updatedBy`. PUT chosen over POST because this is idempotent-by-natural-key, not an always-new-row create. |
| GET | `/api/habit-fields/{habitFieldSeqId}/entries` | `@ParameterObject EntryQueryParams { fromDate?, toDate? }` |
| GET | `/api/habit-fields/{habitFieldSeqId}/entries/latest` | — (prefill value for slider UX, see below — returns today's row if present, else most recent prior day) |
| GET | `/api/habit-fields/{habitFieldSeqId}/consistency` | — |

### Dashboard
| Method | Path | Body/Params |
|---|---|---|
| GET | `/api/dashboard` | — (current user resolved from JWT principal, never a query param — prevents querying another user's data) |

Response: habits grouped with nested fields, each annotated with today's completion + value.
```java
public record DashboardResponse(List<HabitDashboardItem> habits) {}
public record HabitDashboardItem(
    Long habitSeqId, String name,
    List<HabitFieldDashboardItem> fields
) {}
public record HabitFieldDashboardItem(
    Long habitFieldSeqId, String fieldName, HabitFieldType fieldType, String unit,
    boolean completedToday, String todayValue, LocalDateTime updatedAt
) {}
```
JPQL: `SELECT h FROM Habit h LEFT JOIN FETCH h.fields hf WHERE h.user.userSeqId = :userSeqId AND h.active = true`, then a second JPQL batch-fetch for today's entries (`WHERE hf.habitFieldSeqId IN :ids AND e.entryDate = :today`) mapped into the response in the service layer — avoids N+1 and avoids a fan-out join across habit→field→entry in one query.

## DTO validation example (records, validation inline)
```java
public record HabitFieldCreateRequest(
    @Size(max = 100) String fieldName,
    @NotNull HabitFieldType fieldType,
    @Size(max = 30) String unit
) {}

public record EntryCreateRequest(
    @NotBlank @Size(max = 255) String entryValue,
    LocalDateTime entryTimestamp
) {}
```

## Repository rule
All custom lookups via `@Query("JPQL ...")` on `JpaRepository<Entity, Long>` — no `findByXAndY` derived-name methods, to keep every query explicit and reviewable. Example:
```java
@Query("SELECT h FROM Habit h WHERE h.user.userSeqId = :userSeqId AND (:activeOnly = false OR h.active = true)")
List<Habit> findByUser(@Param("userSeqId") Long userSeqId, @Param("activeOnly") boolean activeOnly);
```

## Decisions locked
1. **Auth token**: JWT after WebAuthn assertion verify succeeds. Issue signed JWT (HS256 or RS256, key in `application.yml`/env) with `userSeqId`, `email`, `exp`. Stateless — no server session table. `Authorization: Bearer <token>` on all `/api/**` except `/api/auth/**`. Add `JwtAuthFilter` (OncePerRequestFilter) + `JwtService` (issue/parse/validate).
2. **Email uniqueness**: `app_user.email` has `UNIQUE NOT NULL` constraint (already in schema above) — sole login identifier, no separate username field.
3. **Habit delete**: soft delete via `active` flag (already in schema above) — `DELETE /api/habits/{id}` sets `active=false`, never removes row.

## Frontend slider UX for numeric entries

Problem: alarm-clock-style slider good for coarse pick, bad for 2-decimal precision (e.g. weight 72.45 kg) — rotating a slider to exact cent-value impossible.

Recommendation — two-tier control, not slider alone:
1. **Prefill with last entry value** for that `habitField` (fetch latest via `GET .../entries?limit=1` or dedicated `GET .../entries/latest`) — slider starts where user left off, not zero. Backend needs a small endpoint/query for "last entry value" (`ORDER BY entryTimestamp DESC` JPQL, `LIMIT 1` via `Pageable`/`Page.getContent().get(0)`).
2. **Coarse slider** moves in whole/half units (step = 1 or 0.5, driven by `unit` metadata — e.g. kg uses 0.5, minutes use 1).
3. **Fine adjust**: +/- nudge buttons beside slider, step 0.01, for the decimal tail. Tap-and-hold repeats.
4. Numeric readout above slider is itself tappable to open a direct numeric keypad as an escape hatch — never force slider-only entry.

Backend impact: add `GET /api/habit-fields/{habitFieldSeqId}/entries/latest` → `EntryResponse` (404/empty if none) so frontend can prefill. No schema change needed — `entry_value` already stores string, service parses to `BigDecimal`/double for NUMBER/DURATION types, rounds display to 2dp, raw precision preserved in storage (store as string exactly as typed, don't round on write).

## Build order
1. Project scaffold (Maven, Spring Boot 3.5, Java 25, H2 file DB, springdoc-openapi, webauthn4j deps).
2. `user` module (entity, repo, DTOs, controller/service) minus auth.
3. `auth` module (webauthn4j registration + login ceremonies, challenge store).
4. `habit` module.
5. `habitfield` module (enum wired at entity + DTO level).
6. `entry` module + consistency endpoint.
7. Wire Spring Security to require authenticated session on all `/api/**` except `/api/auth/**`.
8. Smoke test via `/swagger-ui.html`.
