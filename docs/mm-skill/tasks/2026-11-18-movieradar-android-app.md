# Task: MovieRadar — personal Android "where to watch" app

- **Flow:** NEW_PROJECT
- **Date:** 2026-11-18
- **Status:** Phases 0–4 code complete (2026-11-18). Awaiting on-phone test of v0.2.0
- **App name:** MovieRadar (no existing app found with this exact name, checked 2026-11-18)

## Done =
A debug APK that installs on your Android phone. With it you can:
- search any movie or series and see which OTT services have it in your region, and tap one to open that app or website;
- play legal Internet Archive titles inside the app;
- browse by genre and age rating;
- see what's releasing this month.

It has a dark UI in the style of Netflix or X. It's built and tested in 4 phases.

## Brainstorm (confirmed)
| # | Decision |
|---|---|
| 1 | Android only, personal use → **Kotlin + Jetpack Compose** (no iOS) |
| 2 | **India by default**, with a country picker in Settings; OTT services, age ratings and release dates follow the chosen region |
| 3 | "Open in OTT" = open the OTT **app if installed, otherwise its website**, at that title's search or watch page (no fragile per-service exact deep links in the MVP) |
| 4 | **Cloudflare Worker proxy** in front of TMDB: the API key stays out of the APK, results are cached, free tier |
| 5 | **No login.** Watchlist and "continue watching" are stored on the phone in a Room database |
| 6 | **Personal use, no Play Store, no ads.** TMDB's free non-commercial terms cover this. TMDB and JustWatch attribution go in the About screen. Internet Archive is limited to public-domain or CC-licensed items |
| 7 | **English UI**, all text kept in `strings.xml` so a Hindi UI can be added later |
| — | **Approach 2: build in phases**, with a working APK after each phase |

**Rejected alternatives**
- Flutter or React Native → not needed, since iOS is out of scope; native Compose plus Media3 ExoPlayer is the best fit.
- Calling TMDB directly from the app → rejected per #4; the key could be extracted from the APK and there would be no caching.
- Exact title deep links into Netflix, Prime and the others → postponed; TMDB doesn't provide them, so per-service matching would be fragile.

> Note: `~/StreamHub` has an older plan (2026-11-16) for a similar app. This plan replaces it: it adds the proxy, genre and age browsing, upcoming releases and the region picker, and drops the morning-notification feed.

## Tech stack
**Android app** (`~/MovieRadar/android`)
- Kotlin, Jetpack Compose with Material 3 in a custom dark theme, single activity, Navigation Compose
- MVVM + Kotlin Flows, Hilt for dependency injection
- Retrofit + OkHttp + kotlinx.serialization for networking
- Coil for posters and backdrops
- Media3 ExoPlayer for the Internet Archive player
- Room for the watchlist, continue-watching progress and recent searches
- DataStore for settings (region, language)
- minSdk 26; targetSdk = current stable

**Proxy** (`~/MovieRadar/proxy`)
- Cloudflare Worker (TypeScript), deployed with `wrangler`
- Forwards only an allowlist of TMDB endpoints, adds the API key on the server, and caches responses (popular lists ~6 h, details ~24 h)
- A shared app token in a request header so random people can't use your proxy (good enough for personal use)

*(Library versions and API details below are from my training knowledge. I'll check each against current docs while building.)*

## Data sources (verify while building)
| Feature | Source |
|---|---|
| Search movies + series | TMDB `/search/multi` |
| Details, cast, trailer | TMDB `/movie/{id}`, `/tv/{id}` with `append_to_response=credits,videos,release_dates/content_ratings` |
| Where to watch (by region) | TMDB `/{type}/{id}/watch/providers`, split into **Stream / Free / Rent / Buy** (data from JustWatch) |
| Genres | TMDB `/genre/movie/list`, `/genre/tv/list` |
| Age rating (U, U/A 7+, 13+, 16+, A) | Movies: `release_dates` certification for the region. TV: `content_ratings`. I'm not sure TMDB's India data already uses the new U/A 7+/13+/16+ labels or still has plain "UA"; I'll map both to app chips: **All · 7+ · 13+ · 16+ · 18+** |
| Browse by genre + age | TMDB `/discover/movie` (supports `certification_country` + `certification.lte`) and `/discover/tv`. TV discover may not support filtering by certification; if not, TV results will be filtered in the app from each show's content rating |
| This month's releases | `/discover/movie` with `primary_release_date` from the 1st to the last day of the month + `region`; `/discover/tv` with a `first_air_date` range; also OTT-only releases using `with_watch_providers` + `watch_region` |
| Free in-app playback | Internet Archive Advanced Search + `/metadata/{id}`. Only items in curated film collections (`feature_films`, `silent_films`, `classic_tv`, `classic_cartoons`, …) **or** with a public-domain or CC `licenseurl` are shown |

## Screens and UI (Netflix / X style)
- **Look:** pure black background, a large hero backdrop with a gradient, horizontal poster rows, rounded cards, a bottom navigation bar, shimmer while loading, shared-element transition from poster to details.
- **Bottom navigation:** Home · Search · Browse · Upcoming · Library
1. **Home**: hero banner (trending), then rows for Trending, Popular on Netflix / Prime / JioHotstar (your region), New this week, and Free on Internet Archive.
2. **Search**: search as you type (debounced) with Movies / Series tabs. Each result shows its OTT logos and a "▶ Free" badge when an Internet Archive copy exists.
3. **Details**: backdrop, title, year, runtime, **genre chips**, **age-rating badge**, rating, overview, cast row, trailer (opens YouTube), and a **Where to watch** section (logo tiles grouped as Stream, Free, Rent, Buy; tap opens the app or website). Also **▶ Play free (Internet Archive)** when available, and a watchlist button. Series also get a season and episode list.
4. **Browse**: genre grid (Action, Adventure, Comedy, Thriller…), then a results grid with filter chips for Movie/Series, age (All/7+/13+/16+/18+), OTT service, language and sort.
5. **Upcoming**: "This month", grouped by date (a calendar list), with Movies / Series / OTT-only tabs and arrows to move to the previous or next month.
6. **Library**: watchlist + continue watching (Internet Archive).
7. **Player** (Internet Archive): fullscreen in landscape, quality picker (from the MP4 versions available), subtitles if a `.srt` exists, seeking, resume position, picture-in-picture.
8. **Settings**: region picker (default India 🇮🇳), content language, About with TMDB + JustWatch attribution.

## Opening OTT apps (option a)
For each service there's a small mapping table: TMDB provider id → Android package name + web search URL. Examples: Netflix, Prime Video, JioHotstar, SonyLIV, ZEE5, Apple TV.
- If the app is installed, open it, using its search intent or URL where one works.
- Otherwise open the service's website search for that title in the browser.
- Services not in the table fall back to the TMDB/JustWatch watch link.
- Android 11+ requires `<queries>` entries in the manifest for checking which apps are installed.

## Phases (approach 2): each ends with security check → review → APK → you test on your phone
**Phase 0: Setup** (I'll ask before installing anything)
- Your Mac currently has **no Java and no Android SDK**. Install JDK 17 + Android Studio (recommended) or the command-line tools via Homebrew.
- You: create a free TMDB account and API key, and a free Cloudflare account.
- Project skeleton, git init, and a dark theme that builds a debug APK.

**Phase 1: Search + Details + Where to watch + Home**
- Cloudflare Worker proxy (allowlist, caching, app token) and deployment
- Search, Details and Home screens; region picker; opening OTT apps
- Unit tests for the provider mapping, rating mapping and the proxy allowlist

**Phase 2: Internet Archive + Library**
- Internet Archive client with the legal-content filter (unit-tested), matching TMDB titles by title + year
- "▶ Free" badges, the player screen, resume position, watchlist and continue watching in Room

**Phase 3: Browse by genre + age rating**
- Genre grid, discover with filters, age-rating chips, the TV certification fallback

**Phase 4: Upcoming this month**
- Monthly calendar list, Movies / Series / OTT-only tabs, month navigation, "add to watchlist" from upcoming titles

**Finish:** final security gate, review, release-signed APK for your own phone, and an install guide (USB/ADB or copy the APK).

## What you need to provide
- TMDB API key (themoviedb.org → Settings → API); it's stored only in the Cloudflare Worker secret
- A Cloudflare account (free) for the proxy
- An Android phone with "Install unknown apps" enabled (USB debugging is optional, for faster testing)

## Out of scope (later)
- Exact title deep links into each OTT app (phase 2+ improvement)
- Login and syncing across devices, notifications ("now streaming"), Hindi UI, Android TV, offline downloads

## Risks
- **OTT data accuracy:** TMDB/JustWatch provider data can lag by a few days, especially for Indian services.
- **India age-rating data** in TMDB may be incomplete for some titles; those show an "NR" badge.
- **Matching Internet Archive to TMDB** by title + year can miss or mismatch; only confident matches get the badge.
- **Proxy token in the APK:** it can be extracted, but it only gives access to cached TMDB reads, so the risk is acceptable for personal use.

## Progress log
### 2026-11-18 — Phase 0
- Installed Android Studio (Homebrew). The `temurin@17` cask did not install (it likely needs an admin password); using Android Studio's bundled JDK (OpenJDK 25) as JAVA_HOME instead.
- Version catalog pinned to the latest **stable** versions checked on Maven today: AGP 9.5.1, Kotlin 2.4.21, KSP 2.3.14, Hilt 2.60.1, Compose BOM 2026.09.00, Navigation 2.10.2, Media3 1.11.1, Room 2.8.5, Retrofit 3.0.0, OkHttp 5.5.1, Coil 3.6.3. Gradle 9.9.1 wrapper (checksum verified).
- Android skeleton: Hilt app, edge-to-edge, pure-black theme, 5-tab bottom navigation with placeholder screens, HTTPS-only network config, proxy URL and token read from `local.properties` (git-ignored).
- Proxy (`proxy/`): Cloudflare Worker with an endpoint allowlist, strips client auth params, adds the bearer token on the server, constant-time app-token check, edge caching (no caching of errors). **10/10 tests pass**, `tsc` clean, `wrangler deploy --dry-run` bundles.
  - npm installed with `ignore-scripts`, exact versions, and only versions ≥7 days old.
  - `npm audit`: 1 high (picomatch ReDoS) in **vitest's dev-only** dependency tree. It isn't in the deployed worker; accepted for now and will be re-checked when vitest updates.
- **Blocked on user:** Android SDK setup wizard, TMDB token, Cloudflare login.
- SDK installed by user (platform android-37.0, build-tools 36.0.0). compileSdk/targetSdk set to 37.
- Gradle wrapper download from services.gradle.org timed out from Java (maven repos work fine); seeded `~/.gradle/wrapper/dists` with the checksum-verified 9.9.1 zip.
- `./gradlew assembleDebug` → **BUILD SUCCESSFUL in 3m 11s**, `app-debug.apk` 24 MB. Phase 0 skeleton done.
- Still waiting on user: TMDB read token + `wrangler login` + `wrangler secret put TMDB_TOKEN`.

### 2026-11-18 — Phase 1 (code complete, awaiting device test)
- Verified against live TMDB: India certs are `U, U/A 7+, UA(legacy), U/A 13+, U/A 16+, A, S`; TV discover **does** support `certification_country`/`certification` (no in-app fallback needed); IN provider ids confirmed (Netflix 8, Prime 119, JioHotstar 2336, Zee5 232, Sony Liv 237, …); one details call with `append_to_response=credits,videos,release_dates|content_ratings,watch/providers`.
- Proxy: upstream fetch failures now return 502 (was an unhandled 500). 11/11 tests pass.
- Android: data layer (Retrofit via proxy, retry interceptor), DataStore region (default IN), Home (hero + Trending / New on streaming / Top on <top 4 regional services> / Popular this week), Search (debounced, Movies/Series filter, genres, provider logos for top 15), Details (genres, age badge, trailer, Stream/Free/Rent/Buy tiles → OTT app or website, cast, seasons, JustWatch credit), Settings (139 regions, TMDB attribution).
- `testDebugUnitTest`: **12 tests, 0 failures** (AgeRating 5, OttLinks 4, Mappers 3). `assembleDebug` clean, no warnings.
- Emulator: app launches and renders top bar, skeleton loaders, bottom nav and error state. **Data not verified on the emulator**: its virtual network had no default route ("Active default network: none") even when started outside the sandbox. I'll test on the real phone against the deployed proxy instead.
- Debug-only network config allows cleartext to 10.0.2.2/localhost (for `wrangler dev`); release stays HTTPS-only.
- Waiting on user: `npx wrangler login`.

### 2026-11-18 — Proxy deployed
- User ran `wrangler deploy` → `https://movieradar-proxy.movieradar-apps.workers.dev` (workers.dev subdomain `movieradar-apps`). `TMDB_TOKEN` + `APP_TOKEN` uploaded from `.dev.vars` via `wrangler secret put` (values never printed). Browser check: `/` → `{"error":"Unauthorized"}` (token gate works).
- `local.properties` `movieradar.proxyUrl` now points at the deployed proxy.

### 2026-11-18 — Phases 2–4 (code complete, v0.2.0, awaiting device test)
- **Phase 2 — Internet Archive + Library:** archive.org client on its own OkHttp client (never sends `X-App-Token`). Legal rule (`ArchiveRules.isLegal`): PD/CC `licenseurl`, or a curated PD collection and not in any `*_unsorted` bucket; re-checked on full metadata before playback. TMDB→IA match needs a normalized title match + year ±1 (short titles exact only). "Play free" on Details for movies with a match; Home "Free classics" row (curated collections, 1900–1975). Media3 player: quality picker (one MP4 per resolution, default ≤720p), SRT/VTT subtitles, landscape immersive, keep-screen-on, resume (Room, saved every 5 s + on exit, cleared at ≥95 %). Library tab: Continue watching + Watchlist (Room). Watchlist toggle on Details.
- **Phase 3 — Browse:** Movies/Series, genre grid + "All genres", filters: age (All ages/7+/13+/16+/18+), top 6 regional services, sort (Popular/Top rated/Newest), infinite scroll. Verified live: TMDB `certification` accepts `|`-OR (`U/A 13+|UA` → 5615 vs 945). IN uses CBFC labels; other regions use US labels (shown in UI).
- **Phase 4 — Upcoming:** month view grouped by day, Movies / Series / On streaming tabs, prev/next/Today, bookmark → watchlist. Uses primary release / first-air dates (global premiere), top 60 by popularity per type.
- No proxy change needed (all new TMDB calls are `discover` / `genre`, already allow-listed).
- Verified: `testDebugUnitTest` **30 tests, 0 failures** (new: ArchiveRules 14, Browse/Upcoming 4); `assembleDebug` clean, no compiler warnings. Live archive.org checks: classics query 7658 hits; "Nosferatu" 1922 matches the curated copy; built download URL → HTTP 206 `video/mp4` over HTTPS.
- **Not verified:** UI on a real device (emulator has no network). Not done: picture-in-picture, "▶ Free" badges in Search results, release-signed APK.
