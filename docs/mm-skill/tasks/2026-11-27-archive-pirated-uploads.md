# Task: "Play free" offered a pirated upload (Wallbanger)

- **Flow:** BUG_FIX
- **Date:** 2026-11-27
- **Status:** Fixed in v0.9.1 (both apps) 2026-11-27; pushed

## Done =
Wallbanger (2024) and other recent commercial films **no longer show a "Play free · Internet Archive" button**. Real public-domain classics (Night of the Living Dead, Nosferatu, the Free classics Home row) still play.

## Root cause (verified live 2026-11-27)
- For Wallbanger, the app's Archive search returns `wallbanger-2024-67208`, titled "Wallbanger 2024[ 67208]".
- It was uploaded by an ordinary user into `opensource_movies`, Internet Archive's open community upload bucket. The uploader **labelled it CC0 "public domain" themselves**.
- It is a 480p copy of a 2024 commercial film, so it is **pirated, not public domain**.
- `ArchiveRules.isLegal()` trusts any self-declared `licenseurl` containing `creativecommons.org/publicdomain`, so it passed the legal filter.
- `matches()` then accepted it:
  - "wallbanger 2024 67208" starts with "wallbanger ";
  - the item has no year, but its title contains "2024".
- The file is reachable (HTTP 200, video/mp4, 248 MB). Playback most likely failed on the phone for an unrelated reason, but the real bug is that the app offered it at all.

## Plan
1. **Legal filter (`ArchiveRules.isLegal`):**
   - Trust **only Internet Archive's moderated public-domain collections** (feature_films, silent_films, film_noir, classic_tv, cartoons…).
   - **Stop trusting self-applied licenses**, so community uploads in `opensource_movies` no longer count even if marked CC0.
   - The player's re-check (`playable()`) uses the same rule, so an already-cached match can't play either.
2. **Search query (`LEGAL_CLAUSE`):** drop the `licenseurl:*publicdomain*` / `*creativecommons*` clauses to match.
3. **Tests (RED first):**
   - Wallbanger's real metadata (opensource_movies + CC0) is **not legal**;
   - a feature_films item is legal;
   - a CC-licensed item that is only in `opensource_movies` is not legal;
   - update the existing ArchiveRules tests that assumed license trust.
4. **Live check:**
   - The search for "Wallbanger" returns 0 legal matches.
   - Night of the Living Dead (1968) still matches and has MP4 sources.
5. Build both apps as **v0.9.1**, run all tests, then commit and push.

## Rejected alternatives
- **Making Wallbanger play.** Not done: it's a pirated copy, and the app only plays legal public-domain films.
- **Keeping license trust but blocking recent years only.** Rejected: uploaders mislabel old copyrighted films too. The moderated collections are the only reliable signal.

## Trade-off
- A few genuinely Creative-Commons indie films that exist only in community uploads will lose their in-app Play button. Their where-to-watch info stays.

## Result (2026-11-27)
- **RED:** the new Wallbanger test failed on the old rule (15 tests, 1 failed).
- **GREEN:** `isLegal(collections)` now trusts only the moderated collections; the license parameter is removed. The search query and `playable()` re-check use the same rule. **73 tests pass in each flavour.**
- **Live, with the new query:**
  - wallbanger → **0**
  - Night of the Living Dead → 23 (top match: feature_films, with MP4s)
  - Nosferatu → 10
  - His Girl Friday → 12
- **Security:** this narrows what can play. There are no new inputs, network calls or permissions.
