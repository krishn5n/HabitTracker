# Habit Tracker Backend — File & API Reference

Stack: Java 25, Spring Boot 4.1.1, H2 (file-based, `./data/habittracker`), Spring Data JPA (JPQL-only), webauthn4j, JWT (jjwt), springdoc-openapi.

---

## 1. `auth` — WebAuthn login/registration + JWT

| File | Type | Purpose |
|---|---|---|
| `AuthController` | Controller | `/api/auth/webauthn/**` — 4 endpoints below |
| `WebauthnService` | Service | Registration/login ceremonies via webauthn4j, issues JWT |
| `JwtService` | Service | Signs/parses HS256 JWT (`app.jwt.secret`, `app.jwt.expiration-minutes`) |
| `ChallengeStore` | Component | In-memory `ConcurrentHashMap<email, Challenge>`, 5-min TTL, swept every 60s. Not persisted. |
| `WebauthnCredential` | Entity → `webauthn_credential` | One row per registered authenticator |
| `WebauthnCredentialRepository` | Repository | `findByCredentialId`, `findByUser` |

### `WebauthnCredential` fields
`credentialSeqId` (PK) · `user` (FK → User) · `credentialId` (base64url, unique) · `publicKeyCose` (base64 of serialized `AttestedCredentialData`) · `signCount` (replay counter) · `aaguid` · `transports` (comma-joined) · `createdAt`

### Endpoints

| Method | Path | Request DTO | Response DTO |
|---|---|---|---|
| POST | `/registration/options` | `RegistrationOptionsRequest(email, displayName)` | `RegistrationOptionsResponse(rp, user, challenge, pubKeyCredParams, timeout, attestation)` |
| POST | `/registration/verify` | `RegistrationVerifyRequest(email, displayName, credentialId, attestationObject, clientDataJSON, transports)` | `UserResponse` (creates user + credential) |
| POST | `/login/options` | `LoginOptionsRequest(email)` | `LoginOptionsResponse(challenge, timeout, rpId, userVerification, allowCredentials)` |
| POST | `/login/verify` | `LoginVerifyRequest(email, credentialId, authenticatorData, clientDataJSON, signature, userHandle?)` | `LoginVerifyResponse(token, userSeqId, email)` |

All 4 are `permitAll` (open) — this is the only unauthenticated surface in the app.

---

## 2. `config` — security, JWT filter, error handling

| File | Purpose |
|---|---|
| `SecurityConfig` | Stateless `SecurityFilterChain`. `/api/auth/**`, swagger, h2-console → open. Everything else → authenticated. CSRF disabled (stateless API). |
| `JwtAuthFilter` | `OncePerRequestFilter`. Reads `Authorization: Bearer <token>`, parses via `JwtService`, sets `SecurityContext` principal = `userSeqId` (a bare `Long`, no `UserDetails`). |
| `GlobalExceptionHandler` | `@RestControllerAdvice`. `EntityNotFoundException` → 404, `IllegalArgumentException`/`IllegalStateException` → 400. Returns RFC 7807 `ProblemDetail`. |
| `WebAuthnConfig` | Beans: `ObjectConverter`, `AttestedCredentialDataConverter`, `WebAuthnManager` (non-strict — attestation `"none"`, no MDS trust-chain check). |

Because the principal is a plain `Long`, every controller that needs "current user" takes `@AuthenticationPrincipal Long userSeqId` directly (see `EntryController`, `DashboardController`).

---

## 3. `user`

| File | Type |
|---|---|
| `User` | Entity → `app_user` |
| `Gender` | enum: `MALE, FEMALE, OTHER, UNSPECIFIED` |
| `UserController` | `/api/users/{userSeqId}` — GET, PUT |
| `UserService` | get / partial-update (field creation happens only via WebAuthn registration, no standalone POST) |
| `UserRepository` | `findByEmail` |

### `User` fields
`userSeqId` (PK) · `displayName` · `email` (unique) · `gender` · `dateOfBirth` · `weightKg` · `heightCm` · `createdAt` · `updatedAt`

Age is **not stored** — derived from `dateOfBirth` at response time (`UserResponse.ageFrom`).

| Method | Path | Request DTO | Response DTO |
|---|---|---|---|
| GET | `/api/users/{userSeqId}` | — | `UserResponse` |
| PUT | `/api/users/{userSeqId}` | `UserUpdateRequest(displayName?, gender?, dateOfBirth?, weightKg?, heightCm?)` | `UserResponse` |

`UserResponse`: `userSeqId, displayName, email, gender, dateOfBirth, age, weightKg, heightCm`

---

## 4. `habit`

| File | Type |
|---|---|
| `Habit` | Entity → `habit` |
| `HabitController` | `/api/habits` — CRUD |
| `HabitService` | create / list (paginated) / get / update / soft-delete |
| `HabitRepository` | `findByUser` (paginated), `findActiveWithFields` (JOIN FETCH, used by dashboard only) |

### `Habit` fields
`habitSeqId` (PK) · `user` (FK) · `name` · `description` · `active` (soft-delete flag, default `true`) · `createdAt` · `fields` (`OneToMany` → `HabitField`, lazy)

| Method | Path | Request DTO | Response DTO |
|---|---|---|---|
| POST | `/api/habits` | `HabitCreateRequest(userSeqId, name, description?)` | `HabitResponse` |
| GET | `/api/habits?userSeqId=&activeOnly=&page=&size=&sort=` | `HabitQueryParams(userSeqId, activeOnly)` + `Pageable` | `Page<HabitResponse>` |
| GET | `/api/habits/{habitSeqId}` | — | `HabitResponse` |
| PUT | `/api/habits/{habitSeqId}` | `HabitUpdateRequest(name?, description?)` | `HabitResponse` |
| DELETE | `/api/habits/{habitSeqId}` | — | sets `active=false` (soft delete, never removes row) |

`HabitResponse`: `habitSeqId, userSeqId, name, description, active`

---

## 5. `habitfield`

| File | Type |
|---|---|
| `HabitField` | Entity → `habit_field` |
| `HabitFieldType` | enum: `NUMBER, TEXT, BOOLEAN, DURATION` |
| `HabitFieldController` | `/api/habits/{habitSeqId}/fields` + `/api/habit-fields/{habitFieldSeqId}` |
| `HabitFieldService` | create / list (paginated) / update / delete, goal validation |
| `HabitFieldRepository` | `findByHabit` (paginated) |

### `HabitField` fields
`habitFieldSeqId` (PK) · `habit` (FK) · `fieldName` (defaults to habit name if not given) · `fieldType` · `unit` (e.g. "kg", "min", "reps") · **`goalValue`** (nullable `BigDecimal(12,2)` — target threshold for completion) · `createdAt`

**Goal rule**: `goalValue` is only accepted when `fieldType` is `NUMBER` or `DURATION`. Setting it on `TEXT`/`BOOLEAN` → 400.

| Method | Path | Request DTO | Response DTO |
|---|---|---|---|
| POST | `/api/habits/{habitSeqId}/fields` | `HabitFieldCreateRequest(fieldName?, fieldType, unit?, goalValue?)` | `HabitFieldResponse` |
| GET | `/api/habits/{habitSeqId}/fields?page=&size=&sort=` | — + `Pageable` | `Page<HabitFieldResponse>` |
| PUT | `/api/habit-fields/{habitFieldSeqId}` | `HabitFieldUpdateRequest(fieldName?, unit?, goalValue?)` | `HabitFieldResponse` |
| DELETE | `/api/habit-fields/{habitFieldSeqId}` | — | hard delete |

`HabitFieldResponse`: `habitFieldSeqId, habitSeqId, fieldName, fieldType, unit, goalValue`

---

## 6. `entry` — daily values + goal completion + consistency calendar

| File | Type |
|---|---|
| `Entry` | Entity → `entry` |
| `EntryController` | `/api/habit-fields/{habitFieldSeqId}/**` |
| `EntryService` | upsert-by-day, list (paginated), latest, consistency |
| `EntryRepository` | JPQL lookups incl. window query for calendar, batch query for dashboard |

### `Entry` fields
`entrySeqId` (PK) · `habitField` (FK) · `entryValue` (string, parsed per field type) · `entryDate` (natural key component) · `entryTimestamp` (first-write instant, immutable) · `updatedAt` (bumped every upsert) · `updatedBy` (FK → User, from JWT principal) · **`completed`** (boolean, computed + stored at write time)

Unique constraint: `(habit_field_seq_id, entry_date)` — one row per field per day, upsert semantics, not insert-per-entry.

**Completion rule** (computed in `EntryService.upsertEntry`, frozen at write time — not recalculated if the goal changes later):
- `habitField.goalValue == null` → `completed = true` (any entry counts — fields without a goal, e.g. `TEXT`)
- else parse `entryValue` as `BigDecimal` (non-numeric → 400) and `completed = entryValue >= goalValue`

| Method | Path | Request DTO | Response DTO |
|---|---|---|---|
| PUT | `/entries` | `EntryUpsertRequest(entryValue, entryDate?)` (defaults to today) | `EntryResponse` |
| GET | `/entries?fromDate=&toDate=&page=&size=&sort=` | `EntryQueryParams(fromDate?, toDate?)` + `Pageable` | `Page<EntryResponse>` |
| GET | `/entries/latest` | — | `EntryResponse` (404 if none) |
| GET | `/consistency` | — | `ConsistencyResponse` |

`EntryResponse`: `entrySeqId, habitFieldSeqId, entryValue, entryDate, entryTimestamp, updatedAt, updatedBy, completed`

`ConsistencyResponse`: `habitFieldSeqId, windowStart, windowEnd, entries: List<EntryStatus(entryDate, completed)>`
- Window = `max(earliest entry date, today − 4 weeks)` → `today`
- Returns **every day that has an entry** in that window with its `completed` flag — lets a calendar UI two-tone color hit-goal vs missed-goal days. Days with no entry at all are simply absent from the list (frontend treats as "no attempt").

---

## 7. `dashboard` — today's snapshot

| File | Type |
|---|---|
| `DashboardController` | `GET /api/dashboard` |
| `DashboardService` | aggregates active habits + today's entry per field, no N+1 |

`userSeqId` always comes from the JWT principal (`@AuthenticationPrincipal`), never a query param — prevents querying another user's data.

`DashboardResponse`:
```
habits: [
  { habitSeqId, name,
    fields: [
      { habitFieldSeqId, fieldName, fieldType, unit,
        completedToday,   // = today's entry.completed, not just "entry exists"
        todayValue, updatedAt
      }
    ]
  }
]
```

Not paginated (single bounded "today" view, not a general list).

---

## 8. Pagination

All multi-row list endpoints return Spring Data `Page<T>` and accept standard `page`/`size`/`sort` query params:
- `GET /api/habits`
- `GET /api/habits/{habitSeqId}/fields`
- `GET /api/habit-fields/{habitFieldSeqId}/entries`

Dashboard and consistency are intentionally **not** paginated — both are bounded, single-resource views (today's snapshot / a 4-week window), not open-ended lists.

---

## 9. Feature summary

- **Passwordless auth**: WebAuthn (FIDO2) registration + login via webauthn4j, non-strict manager (attestation `"none"`), stateless JWT issued on successful login/registration, 5-min in-memory challenge cache with TTL sweep.
- **Stateless security**: JWT Bearer auth on every `/api/**` route except `/api/auth/**`; JWT principal is the raw `userSeqId`.
- **Habits**: CRUD with soft delete (`active` flag, never hard-deleted).
- **Habit fields**: typed (`NUMBER`/`TEXT`/`BOOLEAN`/`DURATION`), each with an optional unit and an optional numeric **goal**.
- **Goal-based completion**: an entry is marked `completed` against its field's goal (`>=`) at write time; fields with no goal count any entry as completed. Goal edits don't retroactively change past days.
- **Entries**: upsert-by-natural-key (one row per field per day), full history query with date-range filter, "latest value" lookup (for prefilling input UIs).
- **Consistency / calendar data**: per-day completed status over a rolling 4-week (or since-inception, whichever is shorter) window — built for a calendar heatmap view.
- **Dashboard**: single aggregated "today" view across all active habits/fields, batch-fetched (no N+1), reflects real goal completion.
- **Pagination**: every open-ended list endpoint is paginated.
- **Error handling**: global `@RestControllerAdvice` maps domain exceptions to correct HTTP status + RFC 7807 problem-detail bodies.
- **Persistence**: H2 file-based DB (`./data/habittracker`, `AUTO_SERVER=TRUE`) — survives app restarts, not in-memory.
- **API docs**: springdoc-openapi, browsable at `/swagger-ui.html` / `/v3/api-docs`.
