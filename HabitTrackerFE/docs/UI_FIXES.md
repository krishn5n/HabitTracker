# UI fix list (phone-first)

Scope: full app, phone viewport (320–430px) — React + Vite, plain CSS, custom tokens in `src/styles/tokens.css`. No component library, no Tailwind. Write every fix in plain CSS / React idioms already used in this repo.

Each item: root cause with `file:line`, then the fix. Ordered by severity (HIGH → LOW). Items 1–9 map to the issues reported; extra items found during review are appended at the end.

---

## HIGH

### 1. Pages scroll/jump on phone (loading screen, keyboard open) — root cause: `100vh`
**Files:** `src/styles/global.css:12`, `src/styles/global.css:63`, `src/pages/AuthPage.css:2`

```css
body { min-height: 100vh; }        /* global.css:12 */
.app-shell { min-height: 100vh; }  /* global.css:63 */
```

`100vh` on mobile Safari/Chrome is the *largest* viewport height (address bar collapsed). The actual visible viewport is shorter, so the page is always a little taller than the screen — this is exactly the "loading page is scrollable" symptom, and it also makes content judder when the on-screen keyboard opens (issue #2 below shares this cause).

**Fix:** swap to the dynamic viewport unit, with a fallback for older browsers:
```css
body { min-height: 100vh; min-height: 100dvh; }
.app-shell { min-height: 100vh; min-height: 100dvh; }
```
Same fix in `AuthPage.css:2` (`calc(100vh - 8rem)` → add a `calc(100dvh - 8rem)` override after it).

---

### 2. Settings page: weight/height fields pushed off-screen
**Files:** `src/pages/SettingsPage.css:23-33`, `src/styles/global.css:12` (same root cause as #1)

`.settings-form__row` is a correct 1-column-then-2-column grid — the CSS itself isn't broken. The reported "out of screen" is the on-screen keyboard: focusing the weight/height `<input type="number">` opens the keyboard, and because the page height is pinned by `100vh` (see #1) instead of the real visual viewport, the browser doesn't reliably scroll the focused field above the keyboard.

**Fix:**
1. Apply the `100dvh` fix from #1 first.
2. Add a belt-and-suspenders scroll-into-view on focus for the weight/height inputs in `SettingsPage.tsx`:
```tsx
onFocus={(e) => e.currentTarget.scrollIntoView({ block: "center", behavior: "smooth" })}
```
Add to both `#weight` and `#height` inputs (`SettingsPage.tsx:103-124`).

---

### 3. "Save changes" toast — bad position, feels disconnected from the action
**Files:** `src/context/ToastContext.css:1-10`, `src/pages/SettingsPage.tsx:127-129`

```css
.toast-stack {
  position: fixed;
  bottom: var(--space-lg);
  right: var(--space-lg);  /* ← desktop corner toast, on a phone-first single-column form */
  ...
}
```
On phone width, a bottom-right-anchored toast sits awkwardly close to (or under) the fixed bottom tab bar (`Nav.css:27-34`, `height: 3.25rem` + safe-area) and has no relationship to the "Save changes" button the user just pressed.

**Fix:** make the toast stack full-width and centered above the tab bar on phone, narrowing to the corner only at desktop widths:
```css
.toast-stack {
  position: fixed;
  left: var(--space-md);
  right: var(--space-md);
  bottom: calc(3.25rem + env(safe-area-inset-bottom, 0px) + var(--space-md)); /* clears the tab bar */
  z-index: var(--z-toast);
  display: flex;
  flex-direction: column-reverse;
  gap: var(--space-xs);
  max-width: none;
}

@media (min-width: 60rem) {
  .toast-stack {
    left: auto;
    right: var(--space-lg);
    bottom: var(--space-lg);
    max-width: min(22rem, calc(100vw - 2rem));
  }
}
```
(`60rem` matches the existing desktop breakpoint used everywhere else, e.g. `Nav.css:73`, `global.css:73`.)

---

### 4. Consistency calendar only ever shows 1–2 days instead of a real calendar
**Files:** `src/components/ConsistencyCalendar.tsx:8-21`, backend `GET /api/habit-fields/{id}/consistency`

The backend's window is `max(first-ever entry date, today − 4 weeks) → today` (see `HabitTrackerBE/docs/FRONTEND_INTEGRATION.md:294`). That means a field with only one entry ever logged gets back `windowStart == windowEnd == today` — a 1-day window — and the frontend takes that window **literally**:
```ts
const days = dateRange(data.windowStart, data.windowEnd); // collapses to 1 day
```
That's why the calendar "looks very bad": it's rendering exactly what the API's narrow window says, instead of a fixed trailing range.

**Fix (frontend-only, no backend change needed):** always render a fixed trailing window (default 28 days, matching the "last four weeks" language already used in the backend doc; make it a prop so it can be widened to 30+ later) ending **today**, and plot entries from the response onto that fixed range — absent days are empty, exactly as the backend doc already instructs (`FRONTEND_INTEGRATION.md:299`):

```tsx
interface ConsistencyCalendarProps {
  data: ConsistencyResponse;
  windowDays?: number; // default 28
}

function trailingDays(windowDays: number): string[] {
  const dates: string[] = [];
  const cursor = new Date();
  cursor.setDate(cursor.getDate() - (windowDays - 1));
  for (let i = 0; i < windowDays; i++) {
    dates.push(cursor.toISOString().slice(0, 10));
    cursor.setDate(cursor.getDate() + 1);
  }
  return dates;
}

export function ConsistencyCalendar({ data, windowDays = 28 }: ConsistencyCalendarProps) {
  const byDate = new Map(data.entries.map((entry) => [entry.entryDate, entry.completed]));
  const days = trailingDays(windowDays);
  // ...rest unchanged, byDate.get(date) still returns undefined for days outside data.windowStart..windowEnd, which already renders as "empty" — no extra logic needed
```

**Also fix the grid so it reads as a calendar, not a random mosaic** — group into 7-day weeks instead of `auto-fill`:
```css
/* ConsistencyCalendar.css:8-11 */
.consistency__grid {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: var(--space-2xs);
}
```

---

### 5. Habit-field IDs are sequential and guessable in the URL path
**Files:** `src/pages/HabitDetailPage.tsx:23,27,34`, `src/components/FieldPanel.tsx:26,33`, `src/components/EntryLogger.tsx:39`, `src/components/HabitFieldFormModal.tsx:38,43`

Every field/entry/consistency call uses the raw sequential `habitFieldSeqId` (and `habitSeqId`) straight in the path, e.g. `/api/habit-fields/${field.habitFieldSeqId}/consistency`. Sequential integer IDs in a URL are enumerable — a signed-in user can walk `1, 2, 3…` and probe other users' habit fields.

This is fundamentally a **backend authorization issue** (the doc at `FRONTEND_INTEGRATION.md:344` only says a disallowed route returns `401/403`, with no stated per-record ownership check) — the frontend cannot fix enumerability by itself, since the ID has to be in the URL for a REST path either way. Two things to do:

1. **Backend (file outside this repo, flag to that team):** every `/api/habits/{id}`, `/api/habit-fields/{id}`, `/api/habit-fields/{id}/entries*`, `/api/habit-fields/{id}/consistency` handler must verify the record's owning `userSeqId` matches the JWT subject, returning `403`/`404` otherwise (prefer `404` to avoid confirming existence). This is the actual fix — IDOR is a server-side concern, not something client-side code can close.
2. **Frontend hardening (defense in depth, can do now):** when a `403`/`404` comes back from any habit/field/entry call, treat it as "not found," route back to `/habits`, and don't leak the raw ID in any user-facing toast message.

---

## MEDIUM

### 6. Gender shown/selected in ALL CAPS
**File:** `src/pages/SettingsPage.tsx:8,82-88`

```tsx
const GENDERS: Gender[] = ["UNSPECIFIED", "FEMALE", "MALE", "OTHER"];
...
<option key={value} value={value}>{value}</option>
```
The enum values (`MALE`, `FEMALE`, `OTHER`, `UNSPECIFIED`) are rendered verbatim as option labels — that's the API's wire format, not display copy.

**Fix:** keep the enum as the `value`, map to Title Case for the label:
```tsx
const GENDER_LABELS: Record<Gender, string> = {
  UNSPECIFIED: "Unspecified",
  FEMALE: "Female",
  MALE: "Male",
  OTHER: "Other",
};

<select id="gender" className="control" value={gender} onChange={(e) => setGender(e.target.value as Gender)}>
  {GENDERS.map((value) => (
    <option key={value} value={value}>{GENDER_LABELS[value]}</option>
  ))}
</select>
```

---

### 7. Field placeholders not sentence/title-cased ("reps" instead of "Reps")
**Files:** `src/components/EntryLogger.tsx:85`, `src/components/CreateHabitModal.tsx:133`

```tsx
placeholder={goalValue ? `Goal ${goalValue}${unit ? ` ${unit}` : ""}` : unit ?? "Value"}
```
`unit` is stored lowercase (`"reps"`, `"kg"`, `"min"` — see `FRONTEND_INTEGRATION.md:230`) and shown as-is. This isn't a font/font-family bug (the whole app correctly uses `IBM Plex Sans` everywhere, see `tokens.css:35`) — it's a copy-casing bug: a lowercase unit used as a placeholder reads inconsistently next to Title Case labels like "Goal".

**Fix:** capitalize the first letter only when the unit is used as a standalone placeholder (don't mutate the stored value, don't capitalize it when shown inline after a number, e.g. "20 reps" should stay lowercase):
```tsx
const capitalize = (s: string) => s.charAt(0).toUpperCase() + s.slice(1);
...
placeholder={goalValue ? `Goal ${goalValue}${unit ? ` ${unit}` : ""}` : unit ? capitalize(unit) : "Value"}
```
Same treatment in `CreateHabitModal.tsx:133` (`placeholder={fieldType === "DURATION" ? "Min" : "Reps"}`).

---

### 8. No loading indicator while a habit/field/entry mutation is in flight on first paint
**Files:** `src/pages/SettingsPage.tsx:54`, `src/pages/HabitDetailPage.tsx:41-43`, `src/pages/DashboardPage.tsx:100`

These three already show a `Loading…` text line while fetching — that's fine and should stay (do **not** add spinners here, text is enough and matches issue #1's "don't add unnecessary chrome" spirit). The actual gap is narrower than "no loading indicators anywhere":

- `FieldPanel.tsx` fetches `latest` entry and `consistency` on mount (`FieldPanel.tsx:21-41`) with **no loading state at all** — the panel just renders empty until both resolve, which on a slow connection looks like a broken/empty field.

**Fix:** add a minimal loading flag to `FieldPanel`, scoped to just the calendar/last-logged area (the `EntryLogger` input should stay interactive immediately, don't block it):
```tsx
const [loading, setLoading] = useState(true);
// set loading(false) once both the latest-entry and consistency calls have settled (Promise.allSettled)
...
{loading ? <p className="field__helper">Loading…</p> : (<>{/* existing hint + calendar */}</>)}
```
Every other mutation (save profile, log entry, add/edit field, create habit) already disables its submit button and swaps the label for `<span className="spinner" />` — that pattern is correct and consistent (`SettingsPage.tsx:128`, `EntryLogger.tsx:65,91`, `HabitFieldFormModal.tsx:128`, `CreateHabitModal.tsx:164`). No change needed there.

---

## LOW

### 9. Habit → HabitField layout itself is correct — no fix needed
**Files:** `src/components/CreateHabitModal.tsx`, `src/pages/HabitDetailPage.tsx:61-73`

Checked against the backend's `User → Habit → HabitField → Entry` model (`FRONTEND_INTEGRATION.md:12-17`): `CreateHabitModal` creates a habit with exactly one field inline (matching "usually just one field per habit"), and `HabitDetailPage` offers "Add another field" for the multi-field case, laid out in a 1-column (phone) / 2-column (desktop, `HabitDetailPage.css:37-47`) grid. This matches the data model correctly — leaving as-is.

---

## Extra items found during review (not in the original list)

### 10. Toast dismiss has no manual close control
**File:** `src/context/ToastContext.tsx:21-27`

Toasts auto-dismiss after a fixed 6s with no close button. A user who wants to dismiss an error toast early (e.g. it's covering the field they're trying to edit) can't. Low cost to add:
```tsx
<button type="button" className="toast__action" onClick={() => dismiss(toast.id)} aria-label="Dismiss">×</button>
```
placed in `ToastContext.tsx:36-50` next to the existing `toast.action` button, always rendered (not conditional on `toast.action`).

### 11. `field-type-grid` buttons have no `aria-pressed` visual focus differentiation beyond color
Not a new rule — flagging for awareness since `better-accessibility` wasn't loaded as a separate pass in this review: `CreateHabitModal.tsx:110-121` and `HabitFieldFormModal.tsx:77-88` already set `aria-pressed` correctly and rely on `:focus-visible` from `global.css:52-56` for keyboard users — no fix needed, confirmed correct.

---

---

## New features (priority) — from `docs/Habits.dc.html` mockup

Two pieces of the mockup are being adopted as real features, both **priority**: (A) the dashboard redesign (Achieved / Partial / Not logged summary + bento tile grid) and (B) the habit-detail Calendar/History recreation. Both are checked below against what `HabitTrackerBE/docs/FRONTEND_INTEGRATION.md` actually exposes today — not against what the mockup's fake `seedData()` can fabricate.

### Feasibility table

| Mockup capability | Backend support today | Verdict |
|---|---|---|
| Today's Achieved/Partial/Not-logged counts | `GET /api/dashboard` already returns `completedToday`, `todayValue`, `goalValue` per field | **Fully feasible now**, derive client-side, zero backend change |
| Bento tile grid (grouped multi-field habits, spans) | Same `/api/dashboard` payload — grouping is a client-side layout decision | **Fully feasible now** |
| Day streak (detail header) | No dedicated endpoint; must be derived from entry history | **Feasible, frontend-only**, but requires fetching enough history (see below) |
| "Last 4 weeks" rate % | `GET /api/habit-fields/{id}/consistency` returns per-day `completed` for roughly the last 4 weeks | **Fully feasible now**, this is exactly what that endpoint is for |
| Month calendar grid showing each day's **value**, not just a dot | `/consistency` only returns `{entryDate, completed}` — **no `entryValue`**, and its window auto-shrinks (see Issue #4 above) — doesn't support arbitrary month navigation either | **Needs a different data source**: `GET /api/habit-fields/{id}/entries?fromDate=&toDate=&size=` (already documented, already supports date range + paging) — feasible now, no backend change, just a different call than the one currently wired up |
| History tab, infinite scroll, newest-first | `GET /entries?page=&size=&sort=` — doc states rows are "sorted newest first" by default | **Fully feasible now**, this is standard Spring paging already supported |
| Arbitrary month back-navigation bound to "first entry ever" | No `firstEntryDate` field anywhere in any response | **Minor gap** — either derive it (fetch `entries?sort=entryDate,asc&size=1` once per field and cache it) or skip the bound and just render an empty grid for months with no data. Recommend the latter for v1, add the bound later if wanted |
| Bottom-sheet modals (new habit / log entry) sliding from the bottom edge | Pure frontend/CSS concern | **Feasible**, but out of scope for this pass — current `<dialog>`-based `Modal.tsx` is a deliberate, working pattern (`Modal.tsx`, `Modal.css`); re-platforming it to a sheet is a separate, lower-priority visual-polish task, not blocked by anything backend-side. Listed under "Out of scope for v1" below. |
| Color system (green "achieved" vs blue "partial" vs gray "empty") | N/A | **Zero new work** — `tokens.css` already has exactly this 3-state semantic: `--color-accent` (done), `--color-ochre`/`--color-ochre-soft` (logged, goal missed), `--color-paper-3`/`--color-rule` (empty). `ConsistencyCalendar.css:19-31` already implements this triad. Reuse it; do not introduce the mockup's hardcoded hex (`#3ddc84`, `#5b9bff`, etc.) |

**Conclusion: both features are buildable with the backend exactly as documented today.** No backend endpoint needs to change. The only non-trivial addition is switching the detail view's calendar/history data source from `/consistency` (boolean-only, auto-shrinking window) to `/entries` (full value, explicit date range, paginated) — `/consistency` stays in use only for the lightweight "last 4 weeks" rate stat and for the existing compact `ConsistencyCalendar` used inline on the field panel (Issue #4 fix above still applies there).

---

### Feature A: Dashboard redesign (Achieved / Partial / Not logged + bento grid)

**Replaces:** `src/pages/DashboardPage.tsx`, `src/pages/DashboardPage.css`, `src/components/HabitCard.tsx`/`.css`, `src/components/StatTile.tsx`/`.css`.

**Data:** no new call — `GET /api/dashboard` already fetched in `DashboardPage.tsx:21-22`.

**Derivation (client-side, replaces current `completedFields`/`totalFields` math at `DashboardPage.tsx:84-85`):**
```ts
type FieldStatus = "achieved" | "partial" | "empty";

function fieldStatus(f: DashboardField): FieldStatus {
  if (f.todayValue == null) return "empty";
  if (f.goalValue == null) return "achieved";          // no goal → any logged value counts as done
  return f.completedToday ? "achieved" : "partial";     // logged but under goal
}
```
Counts for the summary row: tally `fieldStatus` across every field in `data.habits.flatMap(h => h.fields)`.

**Layout:**
- New `SummaryRow` component: 3-column grid (`display:grid; grid-template-columns:repeat(3,1fr)`), top rule via existing `border-top` pattern already used elsewhere (e.g. `SettingsPage.css`), values styled with `var(--text-2xl)` and `var(--font-display)`; label text uses existing `--text-xs` + `letter-spacing` + `uppercase` pattern already established in `Nav.css`/`habits-page`.
- New `HabitTile` component (replaces `HabitCard`): renders one field as a tappable card — name, big value + unit (`var(--text-2xl)`/`var(--font-display)` for the number, matches existing `--text-stat` token already defined for exactly this purpose in `tokens.css:46`), a thin progress bar (`height:4px`, fill width `%`, color `var(--color-accent)` when achieved / `var(--color-ochre)` when partial — reuse, don't invent), and a status line ("✓ Logged", "3 to go", "Not logged") colored via the same two tokens.
- New `HabitGroupTile` for habits with >1 field: outer card with habit name + "N of M done" header, inner 2-column grid of mini `HabitTile`s — mirrors `habit-card`/`habit-field` structure already in `HabitCard.css`, just regrouped.
- Grid: phone = `grid-template-columns: repeat(2, 1fr)` (already the pattern in `DashboardPage.css`/`.dashboard__grid` — confirm and extend, don't replace the breakpoint strategy), group tiles and wide single-field tiles span `grid-column: span 2` (same mechanism as the existing `.habit-card--wide` in `HabitCard.css:11-18`).
- Tap target: whole tile `onClick` navigates to the field detail route (see Feature B) instead of today's flat dashboard-only view.

**Loading state:** keep the existing text-based `"Loading your habits…"` (`DashboardPage.tsx:100`) per Issue #8's guidance — do not add the mockup's pulsing skeleton blocks; it's motion for a case plain text already covers acceptably, and `prefers-reduced-motion` handling would be extra scope for no real gain. If product wants a skeleton later, reuse `animation: pulse` already defined in spirit by `global.css`'s existing `@keyframes spin` pattern — not required for v1.

**Out of scope for v1:** bottom-sheet "New habit" modal (keep the existing `CreateHabitModal` dialog as-is — it's unrelated to the dashboard's data/layout and already reviewed as correct in Issue #9 above).

---

### Feature B: Habit-detail Calendar / History recreation

**New route:** `/habits/:habitSeqId/fields/:habitFieldSeqId` (nested under the existing `HabitDetailPage`, or a new `FieldDetailPage.tsx` — recommend the latter, since the mockup's detail screen is a distinct full-screen view, not an inline panel like today's `FieldPanel`). Add to `App.tsx` alongside the existing protected routes (`App.tsx:44-51` pattern).

**Data calls:**
1. `GET /api/habit-fields/{habitFieldSeqId}` — field metadata (name, type, unit, goal) — already used in `HabitFieldFormModal`, same call.
2. `GET /api/habit-fields/{habitFieldSeqId}/consistency` — feeds the "Last 4 wks" rate stat only (`hit / total-days-in-window`). Keep using the existing response shape as-is.
3. `GET /api/habit-fields/{habitFieldSeqId}/entries?fromDate={monthStart}&toDate={monthEnd}&size=31` — feeds the **month calendar grid** (per-day value + completed state). Re-fetch on month navigation.
4. `GET /api/habit-fields/{habitFieldSeqId}/entries?page={n}&size=10` — feeds the **History tab**, paginated, loaded on scroll-near-bottom (mirrors the mockup's `onDetailScroll` + `loadMoreHistory` threshold logic, which is pure frontend and can be kept almost as-is).

**Streak derivation:** walk backward from today through the entries fetched for the current + previous month (or a dedicated `entries?toDate=today&size=60&sort=entryDate,desc` call made once on mount), stopping at the first day that's either missing or `completed:false`. This matches the mockup's `isDone`-based streak loop (`data-dc-script` lines ~461-464) — portable almost 1:1 into a `useMemo` in the new page, just swapping the seeded `entries` map for real `EntryResponse[]` keyed by `entryDate`.

**Components to add:**
- `DetailStatsRow.tsx` — streak / last-4-week-rate / goal, 3-column layout identical in structure to Feature A's `SummaryRow` (reuse the same CSS class if the shape matches, don't duplicate).
- `SegmentedToggle.tsx` — generic Calendar/History pill toggle with the sliding-background look (`position:relative` track + `position:absolute` thumb that translates via a CSS variable) — this is a genuinely new UI primitive the app doesn't have yet (current toggles are `aria-pressed` buttons laid out in a grid, e.g. `field-type-grid`, not a sliding pill). Build it once, reuse for both this and the mockup's bool Yes/No entry toggle.
- `MonthCalendarGrid.tsx` — 7-column grid (`repeat(7, minmax(0,1fr))`), weekday header row, day cells sized `44px` minimum (touch-target floor already used elsewhere, e.g. `Nav.css:37` `min-height: 44px`), each cell showing day number (circle, filled when achieved, outlined when logged-but-missed, dashed outline for "today") plus a small value label underneath, truncated with `text-overflow: ellipsis` for long TEXT-field notes.
- `EntryHistoryList.tsx` — reverse-chronological row list, date + sub-label ("Goal achieved" / "Below goal of X" / "Logged") + value + status dot, `IntersectionObserver` or scroll-threshold (`scrollTop + clientHeight >= scrollHeight - 120`, same constant as the mockup) to trigger the next page fetch.

**Color/status mapping (reuse, no new tokens — same triad as Feature A):**
- Achieved → `var(--color-accent)` filled circle / dot.
- Logged but under goal → `var(--color-ochre)` outlined circle (matches `ConsistencyCalendar.css:23-26` exactly).
- No entry → `var(--color-paper-3)` / `var(--color-rule)`, as today.
- "Today" marker when not yet logged → dashed outline using `var(--color-rule-2)`, no new color needed.

**Log-entry interaction:** reuse `EntryLogger`'s existing save logic (`EntryLogger.tsx:32-53`, including its optimistic-update-then-rollback-on-error pattern and the retry toast action) rather than rebuilding the mockup's bottom-sheet form from scratch — wire its `onLogged` callback to update whichever month/history data is currently in view. Presenting it in a bottom sheet vs. the current inline form is a visual follow-up (see "Out of scope" below), not a data-flow change.

**Out of scope for v1:**
- Bottom-sheet presentation for the log-entry form (inline `EntryLogger` already works and is accessible; restyling it as a slide-up sheet is pure CSS/animation polish, do after the data views above are correct).
- The mockup's month-navigation bound to "first ever entry" — ship without the bound (just render an empty grid for data-less months) unless product specifically asks for the disabled-arrow behavior later.
- A dedicated backend stats endpoint for streak/rate — not needed for v1 at this data scale; revisit only if per-field entry volume makes the client-side walk noticeably slow.

---

## Suggested fix order for an implementing agent
1. #1 (100dvh) — unblocks #2 for free.
2. #3 (toast position) — isolated CSS change.
3. #4 (consistency calendar) — the most visible "huge issue" per the report; **Feature B subsumes this** if you're doing the feature work in the same pass, since Feature B's `MonthCalendarGrid` replaces the fix's scope anyway — don't do both the patch and the rebuild, pick one per sprint.
4. #6, #7 (copy casing) — trivial, bundle together.
5. #8 (FieldPanel loading state).
6. #5 (IDOR) — file as a backend ticket; add the frontend 403/404 redirect as a quick companion change.
7. **Feature A** (dashboard redesign) — priority, no backend blockers, do after the quick fixes above so the new tiles aren't built on top of the pre-fix toast/viewport bugs.
8. **Feature B** (detail calendar/history recreation) — priority, do alongside or right after Feature A since both introduce the same `SegmentedToggle`/stat-row primitives — build those primitives once, share them.
9. #10 (toast dismiss) — nice-to-have, do last.
