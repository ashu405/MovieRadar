# Task: Extra Browse categories — Rom-com, Crime thriller, Based on a book, Spy, Steamy

- **Flow:** FEATURE (small; the user chose the categories)
- **Date:** 2026-11-19
- **Status:** Built in v0.4.1 (2026-11-19), awaiting on-phone check

## Done =
Browse shows a **"More categories"** row of 5 tiles under the "All genres" tile: 💕 Rom-com, 🔪 Crime thriller, 📖 Based on a book, 🕵️ Spy and 🔥 Steamy. Each opens a results grid for Movies or Series that works with the existing age, service and sort filters and infinite scroll.

## Definitions (verified live on TMDB, 2026-11-19)
| Category | Movies query | Series query | Live count (movies / series) |
|---|---|---|---|
| Rom-com | `with_genres=10749,35` (Romance AND Comedy) | `with_genres=35` + `with_keywords=9840` (romance) | 2,964 / 2,247 |
| Crime thriller | `with_genres=80,53` | `with_genres=80,9648` (Crime + Mystery; TV has no Thriller genre) | 2,455 / not checked |
| Based on a book | `with_keywords=818` | same | 8,552 / 3,569 |
| Spy | `with_keywords=470` | same | 852 / 212 |
| Steamy | `with_keywords=207767\|364719\|41260\|278555\|378681` (erotic thriller, erotic drama, sensuality, steamy, steamy romance), `include_adult=false`, `vote_count.gte=20` | same | 107 / 9 |

- **Steamy** is limited to mainstream films and series. TMDB's adult flag stays off (explicit titles are excluded), and the vote floor removes obscure low-quality uploads. Examples: *The Handmaiden*, *Lust, Caution*, *Unfaithful*, *Verity*, *P-Valley*.
- **Limits:** Steamy series are few (9) because TMDB tags fewer shows that way. Crime thriller for series uses Crime + Mystery as the closest match.

## Plan
1. `domain/Categories.kt` (pure): the 5 categories with per-type discover params. Unit test for the params and for "Steamy keeps include_adult=false".
2. `BrowseFilter` gets `category`. `TitleRepository.browse` merges the category params, and a category replaces the genre.
3. Browse UI: a "More categories" header plus 5 gradient tiles in the grid. Opening a tile shows the title (e.g. "🔥 Steamy") with the usual filters.
4. Build **v0.4.1** and run all tests.

**No proxy change** (`discover` is already allowed).

## Result (2026-11-19)
- `domain/Categories.kt`, `BrowseFilter.category`, a "More categories" row in Browse. `CategoriesTest` (3 new); Android total **45 pass**; v0.4.1 builds clean (`versionName=0.4.1`).
