# Task: Upcoming → "On streaming" shows only part of the month

- **Flow:** BUG_FIX
- **Date:** 2026-11-18
- **Status:** Fixed in v0.3.0 (2026-11-18), awaiting on-phone check

## Done =
On **Upcoming → On streaming**, the current month lists streaming arrivals across the **whole month**, including days that haven't happened yet, each under its correct date. The Movies and Series tabs stay unchanged.

## Root cause (checked against live TMDB, 2026-11-18)
The tab asks TMDB for titles that **premiered this month _and_ are already on a subscription service** (`with_watch_monetization_types=flatrate`). A title only gets streaming data once it's actually on a service, so:
- **Future days are always empty.** Live check: movies stop at Nov 14 (31 total) and series at Nov 13 (26 total), although today is Nov 18.
- **It misses films that come to OTT this month** after an earlier cinema release, because the query uses the cinema premiere date.

## Fix (revised after the "Perfect Liars" example)
**User's example:** *Perfect Liars* (Prime Video, Nov 20). On TMDB it's a **movie** (Spanish, Amazon MGM). It has a Prime Video digital release on Nov 20 in the US, BR, DE, FR, SG and other countries, but **no India entry**, and no streaming data yet because it isn't out. So the current query can't show it, and neither could my first plan (India digital releases only), which I checked: it's not in any of the 80 results.

1. **Movies = OTT premieres this month.** These are films whose **premiere date is in the month _and_ that have a digital release in that same month**. Two queries are merged:
   - **US** (catches global originals from Netflix, Prime and Apple): 246 titles live, *Perfect Liars* at position 26, 35 dated after today, and no cinema films (e.g. *Focker-in-Law* is excluded). The first 5 pages are used, i.e. the top 100 by popularity.
   - **India** (catches Indian OTT releases): 50 titles live. All pages are used.
   Every title's date is its premiere day, so it lands on the right day.
2. **Series = shows premiering this month on streaming networks**: Netflix 213, Prime Video 1024, Disney+ Hotstar 3919, SonyLIV 2646, ZEE5 2590, Apple TV 2552, Disney+ 2739 (ids verified live). This gave 46 live results, including Nov 19 and 25.
3. **The current query** (titles already on a subscription service in your region) stays as a third source, so recent arrivals aren't lost. Duplicates are removed.
4. **Tab renamed "On OTT".**
5. **Unit test:** checks the query parameters for each source.

**No proxy change:** everything still goes through `discover`, which is already allowed.

**Rejected alternatives:**
- Digital releases in all countries: about 1,100 results, and TMDB returns their original premiere dates (some from 2019), so they can't be grouped by day.
- Fetching each title's details: 60+ calls per month.

## Risks
- TMDB's Indian digital-release data can be incomplete or arrive late, so some OTT arrivals may still be missing.
- Network-based TV only covers the listed services and **new shows only**. A new **season** of an existing show (e.g. season 2) isn't caught, because TMDB dates a show by its first-ever episode.
- Very obscure OTT films beyond the top 100 US results may not appear.

## Result (2026-11-18)
- `domain/ReleaseQueries.kt` (pure) + `TitleRepository.releases` running every page in parallel (failed pages skipped). Tab renamed **On OTT**.
- Unit tests: `OttFixesTest` (3 query tests). Android total **37 pass**. Live TMDB: the US digital-premiere query has Perfect Liars at position 26 (page 2 of 5 read).
