# Task: Settings switch "Show 18+ (adult) titles"

- **Flow:** SMALL_TASK (the user approved the described design: "ok do that")
- **Date:** 2026-11-19
- **Status:** Removed in v0.4.3 at the user's request — adult titles always off, no switch

## Done =
A Settings switch, **off by default** and stored in DataStore (`show_adult`). When on, `include_adult=true` is sent for **Search** (`search/multi`) and **Browse** (genres + the 5 categories incl. Steamy). Home, Upcoming and actor pages keep `include_adult=false`.

## Result
- `SettingsRepository.showAdult`, `BrowseFilter.includeAdult`, `TitleRepository.search(q, includeAdult)`. `Category.params` no longer sets the flag; the repository decides.
- `CategoriesTest` updated (categories don't set the flag; default off). Android total **45 pass**; `versionName=0.4.2`.
- No proxy change.
