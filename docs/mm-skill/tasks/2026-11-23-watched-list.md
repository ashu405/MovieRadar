# Task: ✅ Watched list (both apps)

- **Flow:** FEATURE
- **Date:** 2026-11-23
- **Status:** Built v0.9.0 (both apps) 2026-11-23; pushed. Waiting on: on-phone check

## Done =
You can mark any movie or series as watched. It then **moves out of Watchlist into a new "✅ Watched" section** in Library (newest first), and a **✓ badge** shows on its poster everywhere in the app. Unmarking undoes it.

## Decisions (from brainstorm)
| # | Decision |
|---|---|
| 1 | A new **"✅ Watched"** section in Library, under Watchlist. |
| 2 | Marking watched **moves** the title out of Watchlist. |
| 3 | Mark it from a **"✓ Mark as watched"** button on Details, or by **long-press on a Watchlist poster**. |
| 4 | **Any title** can be marked, not only Watchlist ones. |
| 5 | **✓ Watched badge** on posters everywhere (Home, Search, Browse, Upcoming, Franchises, Actor pages, Library). No date display and no rating. |

## Plan
1. **Database (Room v2 → v3):**
   - New `watched` table: key, tmdbId, type, title, posterPath, year, watchedAt.
   - A `MIGRATION_2_3` that only creates the table, so existing watchlist, progress and alerts data is untouched.
   - DAO: `watched()` (newest first), `watchedKeys()`, `isWatched(key)`, `markWatched` and `unmarkWatched`. Marking watched also removes the title from the Watchlist, in **one transaction**.
2. **LibraryRepository:**
   - `watched: Flow<List<TitleSummary>>`, `watchedKeys: Flow<Set<String>>`, `isWatched(t)`, `setWatched(t, Boolean)`.
   - Unmarking doesn't put it back in the Watchlist; you can re-add it with the bookmark.
3. **Details:** a **"✓ Mark as watched"** button next to the Watchlist button. When marked it reads **"✓ Watched"** (filled), and tapping again unmarks it. The Watchlist button shows as not saved once a title has moved.
4. **Library:**
   - A **"✅ Watched (N)"** section under Watchlist.
   - **Long-press a Watchlist poster** → mark watched. A snackbar "Moved to Watched · Undo" undoes it.
   - Long-press a Watched poster → unmark, also with Undo.
5. **Badge everywhere:**
   - The root provides the set of watched keys through a `CompositionLocal`.
   - `PosterCard` draws a small **✓** corner badge when the title is in that set, so every screen gets it without per-screen changes.
6. **Tests:**
   - A pure test for the move logic (watched removes from watchlist; unmark keeps it out).
   - A migration SQL check matching Room's generated schema, as done for v2.
   - Run all tests in both flavours.
7. Build both apps as **v0.9.0** and commit and push to ashu405/MovieRadar.

## Rejected alternatives
- **A `watched` flag on the watchlist table.** Rejected because decision 4 lets non-watchlist titles be watched, and a separate table keeps the move and undo simple.
- **Passing the watched set to every screen.** Rejected in favour of a `CompositionLocal`: one source, and no changes to about 10 screens.

## Risks
- **DB migration.** It is an additive `CREATE TABLE` only, and the SQL is verified against Room's schema. A wrong migration would crash on start, so it gets tested before the build.
- **Badge cost.** It is a set lookup per poster, which is negligible.

## Result (2026-11-23)
- **Room v3:** `watched` table + `MIGRATION_2_3`. Its SQL is identical to Room's generated `CREATE TABLE`. `markWatched` runs inside `performInTransactionSuspending`, as seen in the generated `LibraryDao_Impl`.
- **Details:** "✓ Mark as watched" / green "✓ Watched" button. The action row now wraps (FlowRow).
- **Library:**
  - "✅ Watched (N)" section.
  - Long-press a Watchlist poster to move it there; long-press a Watched poster to unmark. Both show a snackbar with Undo.
  - Undo after marking puts the title back on the Watchlist.
- **Badge:** ✓ badge on every `PosterCard` via `LocalWatchedKeys`, provided in `MainActivity`.
- **Tests:** `WatchedTest` (4 new) uses a fake DAO that runs the real interface `markWatched`. **73 pass in each flavour.**
- **Security:** local-only data, with no new network calls, permissions or secrets.
- **Deviation:** undoing an unmark re-dates the title to "now", so it moves to the top of Watched.
