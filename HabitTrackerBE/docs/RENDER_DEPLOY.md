# Deploying the backend to Render

Image: `HabitTrackerBE/Dockerfile`, two stages — Maven 3.10.0 on Temurin 25 to
build, `eclipse-temurin:25-jre` to run. Runs as a non-root user, keeps the JVM
as PID 1 so Render's SIGTERM reaches the final snapshot push, and listens on
`$PORT`.

## Service settings

| Setting | Value |
| --- | --- |
| Language / Runtime | Docker |
| Root Directory | `HabitTrackerBE` |
| Dockerfile Path | `./Dockerfile` |
| Health Check Path | `/healthz` |
| Instance Type | Free (512 MB) |

`/healthz` sits behind the data-sync readiness gate: it answers `503` while the
snapshot is being restored and `200 {"status":"up"}` afterwards, so Render only
routes traffic to an instance that can actually serve data.

The JVM is capped at `-XX:MaxRAMPercentage=70.0` with the serial GC, which fits
the 512 MB free instance. No persistent disk is needed or wanted.

## Environment variables

Required. The service will not work correctly without these.

| Variable | Type / format | Length | Example | If wrong |
| --- | --- | --- | --- | --- |
| `DATA_SYNC_ENABLED` | literal `true` or `false` (lowercase) | — | `true` | `false` means no GitHub sync at all: data dies with the instance |
| `DATA_REPO` | `owner/name`, no URL, no `.git` | — | `krishn5n/habittracker-data` | every push fails with HTTP 404 in the logs |
| `DATA_REPO_TOKEN` | GitHub fine-grained PAT, Contents **read and write**, that one repo only | ~93 chars, `github_pat_` prefix (a classic `ghp_` token of 40 chars also works) | `github_pat_11AB…` | 401/403 on every push; expired tokens fail the same way |
| `DATA_ENCRYPTION_KEY` | **standard base64 of exactly 32 random bytes** (AES-256). Padded, 44 characters, ends with `=`. Generate with `openssl rand -base64 32` | exactly 44 chars → 32 bytes | `k7Qv…n2A=` | **the app refuses to start**: `DATA_ENCRYPTION_KEY must decode to 32 bytes, got N` |
| `JWT_SECRET` | passphrase used as raw UTF-8 bytes for HS256 — it is **not** base64-decoded, so any random ASCII text works. Generate with `openssl rand -base64 48` and paste the whole string | **at least 32 characters** (256 bits) | `9bT1…` (64 chars) | shorter than 32 chars → startup fails in `JwtService` with a weak-key error |
| `CORS_ALLOWED_ORIGINS` | comma-separated exact origins, scheme + host + optional port, **no trailing slash**, no `*` | — | `https://habittracker.vercel.app` | browser calls fail CORS preflight; `*` is rejected outright because credentials are allowed |
| `WEBAUTHN_RP_ID` | bare registrable domain. No scheme, no port, no path, no slash. Must equal the frontend host or be a parent of it | — | `habittracker.vercel.app` | passkey registration and login fail in the browser with a security error |
| `WEBAUTHN_ORIGIN` | full frontend origin, scheme + host + optional port, no trailing slash. HTTPS required except on `localhost` | — | `https://habittracker.vercel.app` | WebAuthn verification rejects every assertion |

Recommended.

| Variable | Type / format | Default | Notes |
| --- | --- | --- | --- |
| `H2_CONSOLE_ENABLED` | `true` / `false` | `true` | Set `false` in production — the console is otherwise reachable unauthenticated at `/h2-console` |
| `WEBAUTHN_RP_NAME` | free text, shown in the passkey prompt | `Habit Tracker` | |
| `TZ` | IANA zone id | `Asia/Kolkata` (set in the Dockerfile) | Entry dates are stamped from the server clock, so this decides what "today" means |

Optional, only to change sync behaviour.

| Variable | Type | Default | Notes |
| --- | --- | --- | --- |
| `DATA_REPO_BRANCH` | existing branch name | `main` | Must already exist with at least one commit |
| `DATA_REPO_PATH` | repo-relative path, no leading slash | `snapshots/habittracker.json.enc` | |
| `DATA_SYNC_IDLE_MINUTES` | integer, minutes | `12` | Keep below 15 — that is Render's spin-down window |
| `DATA_SYNC_HARD_MINUTES` | integer, minutes | `30` | Lower it to narrow the worst-case data loss |
| `DATA_SYNC_TICK_SECONDS` | integer, seconds | `15` | Deadline-check resolution; sane range 5–60 |
| `DATA_SYNC_PUSH_ON_SHUTDOWN` | `true` / `false` | `true` | The SIGTERM push; leave on |

Set by Render itself — **do not add these manually**: `PORT` (the app reads it;
locally it falls back to 8080), and Render's own `RENDER_*` variables.

## Generating the two secrets

```sh
openssl rand -base64 32   # DATA_ENCRYPTION_KEY — must be exactly this, 32 bytes
openssl rand -base64 48   # JWT_SECRET — any long random string is fine
```

Keep `DATA_ENCRYPTION_KEY` somewhere other than Render. Every snapshot in the
data repository is unreadable without it, and rotating it orphans the existing
snapshots — to rotate, restore with the old key first, then redeploy with the
new one so the next push re-encrypts.

## First deploy order

1. Private data repo exists with a `main` branch and one commit (done:
   `krishn5n/habittracker-data`).
2. Create the PAT and the encryption key.
3. Create the Render service with the settings above and all required variables.
4. First boot logs `No snapshot at krishn5n/habittracker-data/snapshots/…` and
   starts empty — expected. The first push creates the file.
5. Register a user, then wait for a push (or redeploy to force the SIGTERM
   push) and confirm a new commit appears in the data repo.
6. Restart the service and confirm the user survives. That is the real test.

## Local run

```sh
docker build -t habittracker-be ./HabitTrackerBE
docker run --rm -p 8080:8080 -e JWT_SECRET=local-dev-secret-at-least-32-characters habittracker-be
```

Without `DATA_SYNC_ENABLED=true` nothing contacts GitHub and the H2 file inside
the container is used directly.

Sync internals and trade-offs: [DATA_SYNC.md](DATA_SYNC.md).
