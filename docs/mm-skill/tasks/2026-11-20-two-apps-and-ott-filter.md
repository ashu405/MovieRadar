# Task: Build both apps from one project + per-service filter in Upcoming → On OTT

- **Flow:** FEATURE
- **Date:** 2026-11-20
- **Status:** Built v0.6.0 (both apps) 2026-11-20, awaiting on-phone check

## Done =
1. One build produces **two APKs** that install side by side:
   - **MoviesRadar** (`com.movieradar.app`): your normal app, which updates your existing install. **No 18+ switch.**
   - **MovieRadar** (`com.movieradar.plus`): the same app **plus** the 18+ switch and 18+ rows.
2. Both get the **Trakt Home rows** and everything built so far (actor search, Browse categories, exact OTT links…).
3. **Upcoming → On OTT** has a second chip row: **All · Netflix · Prime Video · JioHotstar · SonyLIV · ZEE5** (with logos). Picking one shows only titles arriving on that service this month.
4. Every row in On OTT shows a **small logo** of the service(s) it's coming to.

## Decisions (from brainstorm)
| # | Decision |
|---|---|
| 1 | Two flavours. Trakt rows in **both**; 18+ only in **MovieRadar**; OTT filter in **both**. |
| 2 | Filter chips: the **big five** only: Netflix, Prime Video, JioHotstar, SonyLIV, ZEE5. |
| 3 | Service **badge** on each On OTT row: yes. |

## How each title gets its service (verified live 2026-11-20)
| Source in On OTT | How the service is known |
|---|---|
| **New series** this month | Query per service by TMDB **network**: Netflix 213, Prime Video 1024, Disney+ Hotstar 3919, SonyLIV 2646, ZEE5 2590. (The JioHotstar network id gets re-checked while building; if there's a newer id, both are used.) |
| **OTT movie premieres** this month | The service named in TMDB's **digital-release note**, e.g. "Netflix", "Prime Video". This is read from `movie/{id}?append_to_response=release_dates` (already allowed on the proxy, cached 24 h). 30 of 39 checked premieres had a note. Name variants (NETFLIX, Amazon Prime Video, Disney+ Hotstar, JioHotstar, Sony LIV…) all count. |
| **Already streaming** in your region | TMDB's normal provider data, queried per service (`with_watch_providers` + `watch_region`). |

Titles with no detectable service still appear under **All** without a badge.

## Plan
1. **Gradle flavours** `standard` and `plus`:
   - the app id and name are set per flavour (`app_name` moves out of `strings.xml`);
   - `BuildConfig.ADULT_ENABLED` is false for standard and true for plus. With it false, the Settings switch is hidden and 18+ is always off, whatever was stored.
   - Both flavours are version **0.6.0**.
2. **Domain (pure, unit-tested):**
   - `OttService` enum: TMDB provider id, network ids and note aliases;
   - `OttService.fromNote()`;
   - tagging and merging titles from the three sources into `UpcomingItem(title, services)`;
   - filtering by service.
3. **Repository:** the On OTT tab runs per-service queries, 3–8 at a time, all cached by the proxy. It reads release notes only for OTT movie premieres (about 60–100 small calls, cached 24 h). Any failed source is skipped.
4. **UI:** service chips under the tabs (only when "On OTT" is selected), logo badges on rows, and the empty state "Nothing found on <service> this month".
5. Run all tests and build **both APKs**, checked with `aapt` (id and name).

**No proxy change.**

## Risks
- **Indian service notes:** TMDB's notes for Indian services are rarer, so Indian movies coming straight to JioHotstar, SonyLIV or ZEE5 may not be tagged until they're out. New series on those services are reliable (network-based).
- **Slower first load:** the first load of a new month is slower (up to ~100 cached lookups).

## Result (2026-11-20)
- Flavours `standard` (com.movieradar.app, "MoviesRadar", ADULT_ENABLED=false) and `plus` (com.movieradar.plus, "MovieRadar", ADULT_ENABLED=true). Both are v0.6.0, checked with aapt and the generated BuildConfig.
- JioHotstar network ids verified: **8036 "JioHotstar"** (2025+ originals, e.g. *Criminal Justice: A Family Matter*, *Search: The Naina Murder Case*) and 3919 "Disney+ Hotstar" (older).
- `domain/OttServices.kt` (OttService, fromNote/fromReleaseNotes, OttReleases.merge/filter), `TitleRepository.ottReleases()` + `ottServiceLogos()`, Upcoming VM/UI service chips and badges.
- Tests: `OttServicesTest` (4 new). **56 pass in each flavour.**
- Live November check: new series Netflix 26 · Prime 12 · JioHotstar 5 · SonyLIV 2 · ZEE5 1; already streaming in IN: 18 · 4 · 5 · 2 · 4; Perfect Liars note → "Prime Video".
- APKs: `MoviesRadar-0.6.0.apk`, `MovieRadar-0.6.0.apk`. No proxy change (Trakt still needs `TRAKT_CLIENT_ID` + deploy).
