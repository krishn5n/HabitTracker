# Data sync: encrypted snapshots in a private GitHub repo

Render free web services have no durable disk and spin down after 15 minutes of
inactivity, so the H2 file on disk is lost on every restart. The durable copy of
the data is an encrypted JSON snapshot committed to a **private** data
repository; the local H2 file is only a working cache.

```
boot            GET snapshot -> decrypt -> import into H2 -> readiness gate opens
every request   idle timer reset to 12 min; writes mark the data dirty
timer fires     export -> gzip -> AES-256-GCM -> PUT /contents (one commit)
SIGTERM         one last best-effort push
```

## Why not the previous approach

The earlier version shelled out to `git` and pushed `data/habittracker.mv.db`
into *this* repository. Three problems, all fatal on Render:

1. A push to the code repo triggers a redeploy, which restarts the app, which
   pushes again — a deploy loop.
2. The runtime container is not a git clone with push credentials, and may not
   have a `git` binary at all.
3. The H2 file is version-coupled, needs `CHECKPOINT`, and bloats the repo.

The snapshot is logical JSON over the GitHub Contents API instead: no git, no
clone, no H2 version coupling.

## Timers

| Timer | Window | Reset by |
| --- | --- | --- |
| Idle | `DATA_SYNC_IDLE_MINUTES` (12) | every inbound request, and its own firing |
| Hard | `DATA_SYNC_HARD_MINUTES` (30) | only its own firing |

Whichever expires first pushes; both in one tick is a single push. A tick runs
every `DATA_SYNC_TICK_SECONDS` (15), so a push is at most one tick late.

The idle timer is reset by traffic on purpose: traffic also resets Render's
15-minute spin-down clock, so while requests keep arriving there is nothing to
race against. 12 < 15 means the snapshot is written before a quiet instance is
suspended. The consequence is that under steady traffic the idle timer never
fires and the guaranteed cadence is the 30-minute hard timer plus the SIGTERM
push.

A push is skipped when nothing has been written since the last one, so an idle
instance does not produce empty commits. On failure the dirty flag stays set and
the next tick retries.

## Encryption

The whole snapshot is gzipped and sealed with AES-256-GCM (fresh 12-byte IV per
push, 128-bit tag) before it leaves the process. The committed file is
`{"v":1,"alg":"A256GCM","compression":"gzip","iv":"…","ct":"…"}` — email
addresses, WebAuthn credential ids and public keys, habit names and entry values
are all inside the ciphertext. The database on local disk stays plaintext so
queries and email lookups are unaffected.

There is no password column today; auth is WebAuthn only. If password login is
added later, store only a hash — it is covered by this encryption automatically.

**Losing `DATA_ENCRYPTION_KEY` means losing all data.** Keep a copy outside
Render.

## Setup

1. Create a **private** repo, e.g. `krishn5n/habittracker-data`, with a `main`
   branch and at least one commit. Nothing else goes in it.
2. Create a fine-grained PAT scoped to that one repository, permission
   **Contents: Read and write**. Note its expiry date — pushes fail silently in
   the logs once it expires.
3. Generate a key: `openssl rand -base64 32`
4. Set these environment variables on the Render service:

| Variable | Value |
| --- | --- |
| `DATA_SYNC_ENABLED` | `true` |
| `DATA_REPO` | `krishn5n/habittracker-data` |
| `DATA_REPO_BRANCH` | `main` |
| `DATA_REPO_PATH` | `snapshots/habittracker.json.enc` |
| `DATA_REPO_TOKEN` | the fine-grained PAT |
| `DATA_ENCRYPTION_KEY` | the base64 key from step 3 |
| `DATA_SYNC_IDLE_MINUTES` | `12` (optional) |
| `DATA_SYNC_HARD_MINUTES` | `30` (optional) |
| `DATA_SYNC_TICK_SECONDS` | `15` (optional) |
| `DATA_SYNC_PUSH_ON_SHUTDOWN` | `true` (optional) |
| `H2_CONSOLE_ENABLED` | `false` |
| `JWT_SECRET`, `CORS_ALLOWED_ORIGINS` | as before |

Exact formats, lengths and failure modes for every variable, plus the Render
service settings, are in [RENDER_DEPLOY.md](RENDER_DEPLOY.md).

Locally `DATA_SYNC_ENABLED` stays `false`: the app then uses the H2 file
directly and never contacts GitHub.

## Startup behaviour

`DataSyncFilter` answers `503 {"error":"warming up"}` with `Retry-After: 5` for
every request until the restore has finished, so no client can read a
half-restored database. The scheduler also refuses to push before the restore
completes — otherwise an empty database would overwrite a good snapshot.

If the snapshot file does not exist yet (first deploy) the app logs a warning
and starts empty; the first push creates it. If the fetch or decrypt fails, the
app logs the error and serves whatever is on local disk rather than refusing to
boot.

## Schema changes

Add new tables to `SnapshotTables.ALL`, parent-first. Columns need no
registration: the exporter reads them from result-set metadata, and the importer
restores only columns that still exist, skipping removed ones with a warning —
so an older snapshot stays loadable after a migration.

After a restore each identity sequence is moved to `MAX(id) + 1`, which is what
keeps the next insert from colliding with a restored row.

## Known costs

- **Repo growth.** One commit per push, old blobs kept forever: roughly 1500
  commits a month at the 30-minute cadence. The snapshot is a few KB gzipped so
  this is fine for a long time; if it ever matters, replace the history with a
  single orphan commit.
- **Cold start.** First request after a spin-down pays Boot startup plus the
  snapshot fetch and import, roughly 30–60s.
- **Worst-case loss.** Writes since the last push, if the SIGTERM push does not
  complete. Shorten `DATA_SYNC_HARD_MINUTES` to narrow it.
- A managed Postgres (Neon, Supabase, Render Postgres) removes all of the above.
  This design exists to keep the data inside a private repo you own.
