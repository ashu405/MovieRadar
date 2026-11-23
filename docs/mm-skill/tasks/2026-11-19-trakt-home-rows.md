# Task: Real "Trending / Most watched / Most anticipated" on Home via Trakt

- **Flow:** FEATURE
- **Date:** 2026-11-19
- **Status:** Built in v0.5.0 (2026-11-19). Waiting on: TRAKT_CLIENT_ID secret + wrangler deploy, then on-phone check

## Done =
Home's "Trending today" and "Popular this week" rows are replaced with real audience lists from Trakt (**worldwide**), plus one new row:
- **🔥 Trending now**: what people are watching right now (movies + series)
- **👀 Most watched this week**: the most viewers this week (movies + series)
- **⏳ Most anticipated**: upcoming titles the most people have saved (movies + series)

Every card opens the normal Details page (posters, where-to-watch and exact links all still come from TMDB). The hero banner uses the top "Trending now" title. If Trakt is down, Home falls back to the current TMDB rows automatically.

## Decisions (from brainstorm)
| # | Decision |
|---|---|
| 1 | Trakt **replaces** "Trending today" and "Popular this week"; **adds** "Most anticipated". **Worldwide** lists. |
| 2–3 | **No YouTube integration.** |
| — | The 18+ rows stay on TMDB. Trakt has no keyword or adult filter, so it can't build them. |

## Verified live (2026-11-19, with your Client ID)
- `movies/trending`, `movies/watched/weekly`, `movies/anticipated`, `shows/trending`: all OK. Each item carries `ids.tmdb`. Top results right now: *The Odyssey*, *Heart of the Beast*, *Avengers: Doomsday*, *The Rings of Power*.
- Trakt returns **no images**, so each card's poster comes from TMDB `movie/{id}` / `tv/{id}` (already allowed and cached 24 h on the proxy).

## Plan
1. **Proxy:** a new route `GET /trakt/{movies|shows}/{trending|watched/weekly|anticipated}?limit=1..30`.
   - The same app-token check as other routes.
   - It adds the `trakt-api-key` and `trakt-api-version: 2` headers on the server; the key never goes into the APK.
   - Caching: 1 h for trending, 6 h for weekly and anticipated.
   - Strict allowlist, with unit tests.
   - **You:** run `npx wrangler secret put TRAKT_CLIENT_ID` (paste the ID), then `npx wrangler deploy`.
2. **App data:** a `TraktApi` on the existing proxy client.
   - 10 movies + 10 series per row, interleaved, giving 3 rows and 60 items.
   - Each item is turned into a TMDB card via `movie/{id}` / `tv/{id}`, 8 at a time. The proxy caches these, so after the first load Home is fast.
   - Items without a TMDB id are skipped.
   - A pure `TraktRows` merge/interleave/dedupe is unit-tested.
3. **Home:** the new rows replace the two TMDB rows. If Trakt fails, it falls back to the TMDB rows. Hero = first Trakt trending item with a backdrop.
4. Run tests and build **v0.5.0**.

## Risks
- **First load is slower:** the first Home load after a cache expiry makes up to 60 small TMDB calls. They run in parallel and are cached for 24 h.
- **Hollywood-heavy:** Trakt users are mostly Western, so worldwide lists lean Hollywood. Indian titles still appear in TMDB-based rows ("Top on Netflix/Prime…").

## Result (2026-11-19)
- Proxy: `src/trakt.ts` + `/trakt/...` route (allowlist, limit 1-30, key added server-side, cached 1 h / 6 h). **25 proxy tests pass** (5 new), `tsc` clean. **Not deployed yet.**
- App: `TraktApi` methods, `TraktRows.merge`, `traktShelf()` (8 TMDB card lookups at a time), Home uses Trakt rows with TMDB fallback. `TraktRowsTest` (4 new); Android total **52 pass**; `versionName=0.5.0`.
- Until the proxy is deployed with `TRAKT_CLIENT_ID`, Home silently shows the old TMDB rows.
