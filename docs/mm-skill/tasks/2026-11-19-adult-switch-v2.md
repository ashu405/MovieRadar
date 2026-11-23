# Task: Bring back the 18+ switch, make it actually show titles, add two 18+ Home rows

- **Flow:** BUG_FIX + small FEATURE
- **Date:** 2026-11-19
- **Status:** Built in v0.4.5 (2026-11-19), awaiting on-phone check

## Done =
1. **Settings** has a "Show 18+ titles" switch again: **off by default**, remembered.
2. **When it's on**, 18+ titles really appear: in **Search**, in **Browse → 🔥 Steamy**, and in two new **Home rows**, "🔞 Trending 18+" and "🔞 Most viewed 18+".
3. **When it's off**, the app behaves exactly as v0.4.4: no 18+ rows, adult filter off everywhere.

## Root cause of "titles didn't come even when enabled" (checked live, 2026-11-19)
The switch did send `include_adult=true`, and TMDB returns more results with it (Steamy: 108 → 303; search "erotic": 524 → 1,375), but:
- adult-leaning titles have tiny popularity and vote counts, so with `sort_by=popularity.desc` they rank last (TMDB's adult-flagged titles sat at **position 262+** of 283 in Steamy);
- Steamy also has `vote_count.gte=20`, which removes almost all of them.
So nothing new ever reached the first screen of results.

## Fix
- **18+ tag set (verified live):** `softcore` 155477 (5,480 movies), `pornography` 445 (660), `erotica` 325693 (32), plus Steamy's existing tags.
- **Steamy, switch on:** add the 18+ tags, drop the vote floor, `include_adult=true`. **Switch off:** unchanged.
- **Home, switch on only:** two rows from `discover/movie` with the 18+ tags and `include_adult=true`:
  - "🔞 Trending 18+" = `sort_by=popularity.desc` (TMDB's real trending list never includes adult titles, so current popularity is the closest substitute);
  - "🔞 Most viewed 18+" = `sort_by=vote_count.desc` (votes are the closest public substitute for views).
  These rows sit after "Popular this week". Their posters are shown as TMDB provides them.
- **Search, switch on:** `include_adult=true`, as before.
- **Settings:** restore the v0.4.2 switch row above Region, then rebuild as v0.4.5.
- **Tests:** the Steamy params for switch on/off, the 18+ Home row queries, and that "off" never sends `include_adult=true`.
- **No proxy change** (`discover` and `search` are already allowed).

## Limits
- Most results are **erotic or softcore feature films**. TMDB's formally adult-flagged titles have very few views or votes, so they show up rarely, even in "Most viewed".
- These titles usually have **no streaming service** in India, so "Where to watch" will often be empty.

## Result (2026-11-19)
- `Adult` + `Category.params(type, adult)`, `SettingsRepository.showAdult`, the switch is back in Settings, Home/Search/Browse read it.
- `CategoriesTest` +3; Android total **48 pass**; `versionName=0.4.5`, label MoviesRadar.
