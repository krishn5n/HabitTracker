# Frontend Integration Guide

Base URL (dev): `http://localhost:8080`
API docs (live, browsable): `http://localhost:8080/swagger-ui.html`

Every response below is JSON. Dates are ISO-8601 (`"2026-10-16"`), timestamps are ISO-8601 local date-time (`"2026-10-16T23:21:16.466716"`), decimals (`BigDecimal`) serialize as plain JSON numbers (`20.00`).

---

## 0. The mental model (read this first)

```
User
 └─ Habit            (e.g. "Push ups")              — soft-deletable
     └─ HabitField   (e.g. "Reps", type NUMBER, goal 20)
         └─ Entry    (one row PER DAY, upserted — not appended)
```

- A **Habit** is the thing the user is tracking ("Push ups", "Drink water").
- A **HabitField** is what gets a number/value — a habit can have more than one field (e.g. "Push ups" could track both reps and duration), though usually just one.
- A **HabitField** optionally has a **goal** (a target number). If set, an **Entry** is only "completed" when its value reaches that goal.
- An **Entry** is the value logged for one field on one specific day. Logging twice on the same day **overwrites**, it does not create a second row — so there's always exactly one entry per field per day.
- **Consistency** = the data behind a calendar heatmap: for each day in the last ~4 weeks, was the goal hit or not.
- **Dashboard** = "what does today look like across all my habits" — one call, no per-habit looping needed.

---

## 1. Auth — WebAuthn (passkey) + JWT

No username/password. The browser's native `navigator.credentials` API does the cryptographic work; this backend only issues challenges and verifies the result.

**All 4 auth endpoints are the only unauthenticated routes in the whole API.** Every other call below needs:
```
Authorization: Bearer <token>
```

### 1a. Register a new user (first-time passkey setup)

```
POST /api/auth/webauthn/registration/options
```
Request body:
| field | type | required | notes |
|---|---|---|---|
| `email` | string | **yes** | max 255, must be valid email |
| `displayName` | string | **yes** | max 100 |

Response (`RegistrationOptionsResponse`) — feed this almost directly into `navigator.credentials.create({ publicKey: ... })`:
```json
{
  "rp": { "id": "localhost", "name": "Habit Tracker" },
  "user": { "id": "<base64url>", "name": "<email>", "displayName": "<displayName>" },
  "challenge": "<base64url>",
  "pubKeyCredParams": [{ "type": "public-key", "alg": -7 }, { "type": "public-key", "alg": -257 }],
  "timeout": 60000,
  "attestation": "none"
}
```
**FE must base64url-decode** `challenge` and `user.id` into `ArrayBuffer`s before passing to the WebAuthn API (standard `navigator.credentials.create` input shape).

```
POST /api/auth/webauthn/registration/verify
```
After `navigator.credentials.create(...)` resolves, base64url-**encode** the raw `ArrayBuffer`s from the result and send:

| field | type | required | source |
|---|---|---|---|
| `email` | string | **yes** | same email as step 1a |
| `displayName` | string | **yes** | same displayName |
| `credentialId` | string (base64url) | **yes** | `credential.id` (already base64url) |
| `attestationObject` | string (base64url) | **yes** | `credential.response.attestationObject` |
| `clientDataJSON` | string (base64url) | **yes** | `credential.response.clientDataJSON` |
| `transports` | string[] | no | `credential.response.getTransports()` (e.g. `["internal"]`) |

Response: `UserResponse` (see §3) — the newly created user. **This call does not log the user in** — it only registers the credential. Call login next.

### 1b. Login

```
POST /api/auth/webauthn/login/options
```
| field | required |
|---|---|
| `email` | **yes** |

Response (`LoginOptionsResponse`) — feed into `navigator.credentials.get({ publicKey: ... })`:
```json
{
  "challenge": "<base64url>",
  "timeout": 60000,
  "rpId": "localhost",
  "userVerification": "preferred",
  "allowCredentials": [{ "type": "public-key", "id": "<base64url credentialId>" }]
}
```

```
POST /api/auth/webauthn/login/verify
```
| field | type | required | source |
|---|---|---|---|
| `email` | string | **yes** | |
| `credentialId` | string (base64url) | **yes** | `assertion.id` |
| `authenticatorData` | string (base64url) | **yes** | `assertion.response.authenticatorData` |
| `clientDataJSON` | string (base64url) | **yes** | `assertion.response.clientDataJSON` |
| `signature` | string (base64url) | **yes** | `assertion.response.signature` |
| `userHandle` | string (base64url) | no | `assertion.response.userHandle`, if present |

Response (`LoginVerifyResponse`):
```json
{ "token": "<JWT>", "userSeqId": 1, "email": "user@example.com" }
```
**Store the token** (memory or secure storage — not plain `localStorage` if you can help it) and send it as `Authorization: Bearer <token>` on every other call. Token expires in 60 minutes (`app.jwt.expiration-minutes`); there is no refresh endpoint yet — expired token means log in again.

---

## 2. Pagination (applies to all list endpoints)

Three list endpoints — habits, habit-fields, entries — accept and return Spring's standard paging shape. Query params (all optional):

| param | default | notes |
|---|---|---|
| `page` | `0` | 0-indexed |
| `size` | `20` | page size |
| `sort` | — | e.g. `sort=name,asc` or repeat for multi-sort |

Response shape (`Page<T>`):
```json
{
  "content": [ /* array of the DTO */ ],
  "totalElements": 42,
  "totalPages": 3,
  "number": 0,
  "size": 20,
  "first": true,
  "last": false
}
```
Use `content` for the rows, the rest for pager UI.

---

## 3. User

```
GET /api/users/{userSeqId}
PUT /api/users/{userSeqId}
```

`UserUpdateRequest` — **everything optional**, only non-null fields are changed (partial update, no way to null out a field once set):
| field | type | required | constraints |
|---|---|---|---|
| `displayName` | string | no | max 100 |
| `gender` | enum | no | `MALE` \| `FEMALE` \| `OTHER` \| `UNSPECIFIED` |
| `dateOfBirth` | date | no | `YYYY-MM-DD` |
| `weightKg` | number | no | |
| `heightCm` | number | no | |

`UserResponse`:
```json
{
  "userSeqId": 1, "displayName": "Jane", "email": "jane@x.com",
  "gender": "FEMALE", "dateOfBirth": "1990-01-01", "age": 36,
  "weightKg": 60.50, "heightCm": 165.00
}
```
`age` is computed server-side — don't try to derive it yourself, just display it.

---

## 4. Habit

```
POST   /api/habits
GET    /api/habits?userSeqId=&activeOnly=&page=&size=&sort=
GET    /api/habits/{habitSeqId}
PUT    /api/habits/{habitSeqId}
DELETE /api/habits/{habitSeqId}
```

`HabitCreateRequest`:
| field | type | required | constraints |
|---|---|---|---|
| `userSeqId` | number | **yes** | |
| `name` | string | **yes** | max 100 |
| `description` | string | no | max 500 |

`HabitQueryParams` (GET query string):
| param | required | notes |
|---|---|---|
| `userSeqId` | **yes** | |
| `activeOnly` | no | boolean, default `false` — `true` hides soft-deleted habits |

`HabitUpdateRequest`: `name` (optional, max 100), `description` (optional, max 500) — partial update.

`DELETE` **does not remove the row** — it sets `active=false` (soft delete). A deleted habit still shows up with `activeOnly=false`.

`HabitResponse`:
```json
{ "habitSeqId": 1, "userSeqId": 1, "name": "Push ups", "description": null, "active": true }
```

---

## 5. HabitField

```
POST   /api/habits/{habitSeqId}/fields
GET    /api/habits/{habitSeqId}/fields?page=&size=&sort=
GET    /api/habit-fields/{habitFieldSeqId}
PUT    /api/habit-fields/{habitFieldSeqId}
DELETE /api/habit-fields/{habitFieldSeqId}
```

`HabitFieldCreateRequest`:
| field | type | required | constraints |
|---|---|---|---|
| `fieldName` | string | no | max 100 — defaults to the parent habit's name if omitted |
| `fieldType` | enum | **yes** | `NUMBER` \| `TEXT` \| `BOOLEAN` \| `DURATION` |
| `unit` | string | no | max 30, e.g. `"kg"`, `"min"`, `"reps"` |
| `goalValue` | number | no | **only valid when `fieldType` is `NUMBER` or `DURATION`** — sending it on `TEXT`/`BOOLEAN` → `400` |

**Use this field to drive your input widget**: `fieldType` tells you what control to render (number input, text box, toggle, duration picker). `unit` is a label to show next to the value. `goalValue`, if present, is what a progress bar / "X of Y" indicator should target.

`HabitFieldUpdateRequest`: `fieldName`, `unit`, `goalValue` — all optional, partial update. Same goal/type rule applies on update.

`HabitFieldResponse`:
```json
{
  "habitFieldSeqId": 1, "habitSeqId": 1, "fieldName": "Push ups",
  "fieldType": "NUMBER", "unit": "reps", "goalValue": 20.00
}
```
`goalValue: null` means **no goal set** — in that case, logging any entry at all counts as "done" for the day (see §6 completion rule).

`GET /api/habit-fields/{habitFieldSeqId}` returns the same `HabitFieldResponse` shape as one row of the list above — use it to fetch a single field directly (e.g. a field-detail screen) instead of re-fetching and filtering the parent habit's list. 404 if the id doesn't exist.

`DELETE` here is a **hard delete** (unlike Habit).

---

## 6. Entry — daily logging + goal completion

```
PUT  /api/habit-fields/{habitFieldSeqId}/entries
GET  /api/habit-fields/{habitFieldSeqId}/entries?fromDate=&toDate=&page=&size=&sort=
GET  /api/habit-fields/{habitFieldSeqId}/entries/latest
GET  /api/habit-fields/{habitFieldSeqId}/consistency
```

### Logging a value (`PUT /entries`)
This is an **upsert**, always `PUT` (never `POST`). Call it every time the user logs/edits today's value — logging twice on the same day just updates the same row.

`EntryUpsertRequest`:
| field | type | required | constraints |
|---|---|---|---|
| `entryValue` | string | **yes** | max 255. Send numbers as numeric strings (`"25"`, `"3.5"`) even for `NUMBER`/`DURATION` fields — the field is a string on the wire. For `BOOLEAN` fields send `"true"`/`"false"`. |
| `entryDate` | date | no | `YYYY-MM-DD`, **defaults to today** if omitted |

**Completion is computed server-side, you don't calculate it:**
- Field has no `goalValue` → `completed` is always `true` once any value is logged.
- Field has a `goalValue` → `entryValue` is parsed as a number and `completed = (value >= goalValue)`. **Equal to the goal counts as completed.** If `entryValue` isn't numeric on a goal-bearing field, the API returns `400`.

Response (`EntryResponse`):
```json
{
  "entrySeqId": 2, "habitFieldSeqId": 1, "entryValue": "20",
  "entryDate": "2026-10-11", "entryTimestamp": "2026-10-16T23:21:16.622293",
  "updatedAt": "2026-10-16T23:21:16.622293", "updatedBy": 1,
  "completed": true
}
```
Use `completed` directly to drive a checkmark/progress state — don't re-derive it from `entryValue` and some locally-cached goal, the server is the source of truth (and it's frozen at write time, so it stays correct even if the goal is edited later).

### History (`GET /entries`)
Query params `fromDate`/`toDate` (both optional, inclusive, `YYYY-MM-DD`) plus standard pagination. Rows sorted newest first.

### Prefill ("what did I log last time") (`GET /entries/latest`)
Returns the single most recent `EntryResponse` (by date, regardless of today) — use this to prefill an input with the user's last value instead of starting from zero. **404 if the field has never been logged.**

### Calendar data (`GET /consistency`)
```json
{
  "habitFieldSeqId": 1,
  "windowStart": "2026-10-10",
  "windowEnd": "2026-10-16",
  "entries": [
    { "entryDate": "2026-10-10", "completed": false },
    { "entryDate": "2026-10-11", "completed": true },
    { "entryDate": "2026-10-12", "completed": true }
  ]
}
```
Window is `max(first-ever entry date, today − 4 weeks)` through today. **Build your calendar like this:**
- date present with `completed: true` → green/filled
- date present with `completed: false` → attempted but missed goal — amber/outline
- date **absent** from the list → no entry logged that day — empty/gray

Do not assume every date in the window is present — only days with an actual entry appear.

---

## 7. Dashboard — "today" at a glance

```
GET /api/dashboard
```
No params — the user is taken from your JWT automatically, you can't (and don't need to) pass `userSeqId`.

```json
{
  "habits": [
    {
      "habitSeqId": 1, "name": "Push ups",
      "fields": [
        {
          "habitFieldSeqId": 1, "fieldName": "Push ups", "fieldType": "NUMBER", "unit": "reps",
          "goalValue": 20.00, "completedToday": false, "todayValue": null, "updatedAt": null
        }
      ]
    }
  ]
}
```
One call to render a full home screen: loop `habits` → `fields`, use `completedToday` for a checkmark, `todayValue` for an inline display, `updatedAt` for a "last updated 2m ago" label, `goalValue` + `todayValue` together for a progress bar ("12 / 20 reps"). `goalValue` is `null` when the field has no goal configured — in that case any logged value counts as complete, so just show presence/absence, not a progress fraction. All of `completedToday/todayValue/updatedAt` come back as `false`/`null`/`null` together when nothing's been logged today yet — not an error, just an empty state.

---

## 8. Errors

All errors are RFC 7807 `application/problem+json`:
```json
{
  "type": "about:blank",
  "title": "Bad Request",
  "status": 400,
  "detail": "entryValue must be numeric for a goal-based field: abc",
  "instance": "/api/habit-fields/1/entries"
}
```
| status | meaning |
|---|---|
| `400` | bad input — invalid enum, wrong type for a goal field, goal set on a non-numeric field type, malformed request body |
| `401`/`403` | missing/invalid/expired JWT, or hitting a route you're not allowed to |
| `404` | referenced id doesn't exist (habit, habit field, user, entry, credential) |

`detail` is safe to show directly or log — it's a plain human-readable message, not a stack trace.

---

## 9. Quick endpoint index

| Method | Path | Auth | Paginated |
|---|---|---|---|
| POST | `/api/auth/webauthn/registration/options` | open | — |
| POST | `/api/auth/webauthn/registration/verify` | open | — |
| POST | `/api/auth/webauthn/login/options` | open | — |
| POST | `/api/auth/webauthn/login/verify` | open | — |
| GET/PUT | `/api/users/{userSeqId}` | JWT | — |
| POST/GET | `/api/habits` | JWT | GET: yes |
| GET/PUT/DELETE | `/api/habits/{habitSeqId}` | JWT | — |
| POST/GET | `/api/habits/{habitSeqId}/fields` | JWT | GET: yes |
| GET/PUT/DELETE | `/api/habit-fields/{habitFieldSeqId}` | JWT | — |
| PUT/GET | `/api/habit-fields/{habitFieldSeqId}/entries` | JWT | GET: yes |
| GET | `/api/habit-fields/{habitFieldSeqId}/entries/latest` | JWT | — |
| GET | `/api/habit-fields/{habitFieldSeqId}/consistency` | JWT | — |
| GET | `/api/dashboard` | JWT | — |
