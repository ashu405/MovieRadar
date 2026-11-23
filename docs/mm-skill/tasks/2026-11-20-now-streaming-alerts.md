# Task: "Now streaming" alerts (both apps)

- **Flow:** FEATURE
- **Date:** 2026-11-20
- **Status:** Built v0.7.0 (both apps) 2026-11-20, awaiting on-phone check

## Done =
On any title you can tap **🔔 Notify me**. Once a day (around 9 AM) the app checks those titles. When one **newly** becomes available on a subscription or free service in your region, you get a notification like "**Now streaming on Netflix:** Avengers: Doomsday". Tapping it opens that title's page. This works in **both** MoviesRadar and MovieRadar.

## Decisions (from brainstorm)
| # | Decision |
|---|---|
| 1 | Alerts only for titles with **🔔 Notify me** turned on. The Watchlist stays separate. |
| 2 | "Streaming" = **subscription or free (incl. with ads)** in your region (TMDB `flatrate` + `free` + `ads`). Rent/buy doesn't count. |
| 3 | Check **once a day, ~9 AM**, with Android WorkManager; it needs internet and respects battery saver, so the time can drift a little. |
| 4 | **Only new arrivals alert.** When you tap 🔔, the services it's already on are recorded and won't alert. Later, each newly added service alerts once. |

## Plan
1. **Dependency:** `androidx.work:work-runtime-ktx:2.12.0` (stable, released 2026-09-23).
2. **Storage (Room):** a new `alerts` table (title key, id, type, title, poster, known service ids, created-at).
   - The database goes from **v1 to v2 with a proper migration**, so your **watchlist and Continue watching are kept**.
3. **Pure rules (unit-tested):**
   - `AlertRules.streamingIds(where)` = flatrate + free + ads provider ids;
   - `AlertRules.newServices(known, now)`;
   - the notification text ("Now streaming on Netflix" / "…on Netflix and Prime Video").
4. **🔔 Notify me** button:
   - on **Details**, next to Watchlist and Trailer;
   - as a bell icon on **Upcoming** rows, next to the bookmark.
   - The first tap asks for **notification permission** (Android 13+).
   - Turning it on records the current services as already known.
5. **Library:** a new **"🔔 Alerts"** section lists titles with alerts on; tap to open, long-press to turn off.
6. **Daily worker:**
   - It runs for every alerted title: it fetches where-to-watch in your region, compares with the known services, then notifies and updates them. Calls go through the existing proxy (cached).
   - A failed title is retried the next day.
7. **Notifications:** a "Streaming alerts" channel. The title's poster is the large icon when it loads, and tapping opens Details through a deep link handled in `MainActivity`.
8. **Settings:** a **"Streaming alerts"** switch, on by default. Off = the daily check is cancelled and no notifications are sent; your 🔔 list is kept.
9. Build **both apps** as **v0.7.0** and run all tests.

**No proxy change** (watch providers are already allowed).

## Risks
- **Data lag:** TMDB/JustWatch availability can lag a day or two, so an alert may arrive a little after the title actually lands.
- **Phone battery restrictions:** some phones (Xiaomi, Oppo, Vivo, Realme) kill background jobs aggressively. If alerts don't arrive, set the app's **Battery → No restrictions** in Android settings.
- **Service changes:** a title disappearing from a service doesn't notify (only arrivals do).

## Result (2026-11-20)
- WorkManager 2.12.0; Room v1→v2 with `MIGRATION_1_2` (its SQL matches Room's generated `alerts` schema exactly, checked against `LibraryDb_Impl`).
- `domain/AlertRules`, `alerts/AlertRepository` (enable = record known services, check = new arrivals once each), `alerts/AlertWorker` (daily, next ~9 AM, needs network, retries on failure), `alerts/AlertNotifier` (channel, poster large icon, tap → Details via `MainActivity` singleTop deep link).
- UI: 🔔 Notify me on Details and Upcoming rows (asks POST_NOTIFICATIONS on Android 13+), Library "🔔 Alerts" (tap opens, long-press stops), Settings "Streaming alerts" switch (on by default; off cancels the job).
- `AlertRulesTest` (5 new). **61 tests pass in each flavour.** Both APKs v0.7.0, with POST_NOTIFICATIONS declared.
- **Not verified on a device:** an actual notification firing (it needs a title to newly gain a service, or a manual test run).
