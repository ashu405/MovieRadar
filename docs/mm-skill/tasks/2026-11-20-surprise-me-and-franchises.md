# Task: 🎲 Surprise me (mood picker) + 🎬 Franchises / watch order (both apps)

- **Flow:** FEATURE
- **Date:** 2026-11-20
- **Status:** Built v0.8.0 (both apps) 2026-11-20. Waiting on: wrangler deploy (collection routes), then on-phone check

## Done =
1. **🎲 Surprise me** (a Home top-bar button) opens a sheet:
   - choose **Mood · Type · Length · Service** and get **3 picks you can stream right now** in your region, rated ⭐ 6.5+;
   - **🔀 Shuffle** gives 3 new ones; tap a pick to open Details.
2. **Settings → My services:** tick the services you subscribe to (Netflix, Prime Video, JioHotstar, SonyLIV, ZEE5). "Any of mine" in Surprise me uses only those (or all five if none is ticked).
3. **Franchise strip on Details:** when a movie belongs to a franchise, **"Part of the Golmaal Collection · 5 films"** shows every part in **release order**, with the current film marked and each part's streaming logo.
4. **Browse → 🎬 Franchises:** a grid of popular franchises plus a **franchise search**. Each opens a franchise page listing the parts in release order with where to watch.
5. **MovieRadar only:** a **🔥 Steamy** mood, shown only when the 18+ switch is on.

## Decisions (from brainstorm)
| # | Decision |
|---|---|
| 1 | Franchises on **Details and in Browse** (with search). |
| 2 | Surprise me as recommended: 8 moods, Type, Length, Service, 3 picks, Shuffle. |
| 3 | **"My services"** setting: yes. |
| 4 | **🔥 Steamy** mood in MovieRadar when 18+ is on: yes. |

## Data (verified live 2026-11-20)
**Franchises:** a movie's `belongs_to_collection`, then `collection/{id}` with its parts and release dates.

Popular grid (ids checked):

| Franchise | Id | Franchise | Id |
|---|---|---|---|
| Golmaal | 483464 | Avengers | 86311 |
| Dhoom | 44976 | Harry Potter | 1241 |
| Housefull | 142015 | Fast & Furious | 9485 |
| Tiger | 485645 | Mission: Impossible | 87359 |
| Stree | 1639816 | James Bond | 645 |
| Hera Pheri | 142022 | Jurassic Park | 328 |
| Singham | 1793488 | The Lord of the Rings | 119 |
| Dabangg | 256433 | | |
| Krrish | 246091 | | |
| Baahubali | 350309 | | |
| K.G.F | 657153 | | |
| Pushpa | 921781 | | |
| Bhool Bhulaiyaa | 977824 | | |

TMDB limitations: franchises are single series (Avengers ≠ the whole MCU; Tiger, War and Pathaan are separate), **release order only** (no story-chronological order), and **movies only** (TMDB has no series franchises).

**Mood → TMDB `discover` (pure, unit-tested):**

| Mood | Query |
|---|---|
| 😄 Feel-good | Comedy, Family or Romance; no Horror, Thriller or War |
| 😂 Laugh out loud | Comedy |
| 🔪 Thriller night | Thriller or Crime |
| 💕 Romantic | Romance |
| 👨‍👩‍👧 Family | Family or Animation |
| 🤯 Mind-bending | Sci-Fi or Mystery |
| 👻 Scary | Horror |
| 💥 Action-packed | Action or Adventure |
| 🔥 Steamy (MovieRadar + 18+) | Steamy tags, `include_adult=true` |

Every mood also applies: `watch_region` = your region, `with_watch_providers` = chosen service(s) (subscription only), `vote_average ≥ 6.5`, `vote_count ≥ 100`, and for movies `with_runtime.lte` 100/120 when a length is picked. Series use the TV genre ids. Picks are taken at random from up to the first 5 result pages, and Shuffle never repeats a pick in the same session.

## Plan
1. **Proxy:** allow `collection/{id}` (24 h) and `search/collection` (1 h), with unit tests. **Needs one `npx wrangler deploy`.**
2. **Domain (pure, tested):**
   - `Mood` with discover params per type;
   - `SurprisePicker` (random 3 without repeats, given a seed);
   - `Franchises.POPULAR` list;
   - `Franchise.ordered()` (release order, undated last).
3. **Data:**
   - `TitleRepository.surprise(...)`, `collection(id)` and `searchCollections(q)`;
   - `DetailsDto.belongsToCollection`;
   - `SettingsRepository.myServices` (DataStore).
4. **UI:**
   - the Home 🎲 button and a bottom sheet;
   - the Details franchise strip (lazy-loaded);
   - Browse "🎬 Franchises" (tile + search) and a new `FranchiseRoute` screen;
   - Settings "My services" chips.
5. Build both apps as **v0.8.0** and run all tests.

## Risks
- **Thin lists:** some moods × service × length combinations have few results; the sheet then says so and suggests "Any length" or "Any service".
- **Incomplete franchises:** some regional franchises aren't grouped as collections on TMDB, so their films won't show a franchise strip.

## Result (2026-11-20)
- Proxy: `collection/{id}` (24 h) and `search/collection` (1 h) allowed. **26 proxy tests pass**, `tsc` clean. **Not deployed yet.**
- App: `domain/Surprise.kt` (Mood, SurpriseRules, Franchises), `SurpriseSheet` (+VM with pool cache, no-repeat Shuffle), Settings "My services", Details franchise strip (with each part's streaming logos), `FranchiseScreen`, `FranchiseListScreen` (popular + search), Browse "🎬 Franchises" tile, Home 🎲 button.
- TV "Scary" keywords verified: 315058 = horror, 6152 = supernatural.
- `SurpriseAndFranchiseTest` (8 new). **69 pass in each flavour.** Both APKs v0.8.0.
- Live: Thriller night (movies, any of 5, IN) → 796 titles; Feel-good series on Netflix → 308; Golmaal → 2006, 2008, 2010, 2017, 2027 (announced).
- Until the proxy is deployed, franchise strips, pages and search quietly fail; Surprise me works (it only uses `discover`).
