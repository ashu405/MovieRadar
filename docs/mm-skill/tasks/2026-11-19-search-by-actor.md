# Task: Search by actor / actress

- **Flow:** FEATURE
- **Date:** 2026-11-19
- **Status:** Built in v0.4.0 (2026-11-19), awaiting proxy deploy + on-phone check

## Done =
Typing an actor's or actress's name in Search and choosing the new **People** chip lists matching people. Tapping a person opens their page with their acting filmography (Movies / Series), newest first. Tapping any title there opens its Details. Tapping a cast member on Details opens the same page.

## Decisions (from brainstorm)
| # | Decision |
|---|---|
| 1 | A fourth filter chip: **All · Movies · Series · People**. "All" stays titles only, as today. |
| 2 | Actor page sorted **newest first**. Titles with no date (unreleased or unannounced) are listed first as "Upcoming", then the rest by date. |
| 3 | **Hide self-appearances**: credits where the character is "Self", "Himself" or "Herself", and TV in the Talk, Reality, News and Awards genres. |

## Data (checked live against TMDB, 2026-11-19)
- **People search:** the app's existing `search/multi` already returns people (`media_type: "person"`, with photo, department and `known_for`). For example, "Shah Rukh Khan" → person 35742, known for *DDLJ*, *My Name Is Khan* and *Kal Ho Naa Ho*. Today the mapper drops these.
- **Filmography:** `person/{id}?append_to_response=combined_credits` returns the bio and photo plus all credits in one call (Shah Rukh Khan: 154 cast credits, 117 movies and 37 TV). Sorted by popularity, his list opens with talk shows (*Koffee with Karan*, *The Kapil Sharma Show*), which is why decision 3 matters.

## Plan
1. **Proxy:** allow `person/{id}` (TTL 24 h) in `rules.ts`, with a unit test. **Needs `npx wrangler deploy`** (one deploy also covers the pending exact-links change).
2. **Data:** a `PersonDto` with combined_credits, people parsed from `search/multi`, and a pure `Filmography.build()` that filters self-appearances, dedupes and sorts newest first. Unit-tested.
3. **Search:** a People chip. The results show round photo, name, "Acting" and the top 3 known-for titles. Debounce and loading work as today.
4. **Person screen:** a new route with photo, name, birthday and birthplace, an expandable bio, Movies / Series chips with counts, and a poster grid. Tap → Details.
5. **Details:** cast circles become tappable → Person screen.
6. Build **v0.4.0** and run all tests.

**Rejected alternative:** `discover?with_cast=` for filmography. It covers movies only (TV discover has no cast filter) and needs one call per page.

## Risks
- Some Indian actors have incomplete TMDB credits, especially regional TV.
- The bio may only be available in English.

## Result (2026-11-19)
- Proxy: `person/{id}` allowed (24 h). **20 proxy tests pass**, `tsc` clean. **Not deployed yet.**
- App: People chip, `PersonScreen` (photo, department, born, bio, Movies/Series chips with counts, poster grid, "Upcoming" for undated), tappable cast on Details. **42 Android tests pass** (5 new in `PeopleTest`); v0.4.0 builds clean.
- Filter tightened after live data: on TV, an empty role, Host/Presenter/Judge/Guest/Contestant, or the role being the actor's own name also count as non-acting. Live check on Shah Rukh Khan before tightening: 154 credits → 103 movies, 9 series; newest first: King: Part 2 (Upcoming), King (2026), Dunki, Tiger 3, Jawan.
- Without the deploy, People search works (it uses `search/multi`), but opening an actor shows "That request isn't allowed by the proxy".
