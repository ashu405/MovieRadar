# Task: Tapping a service opens the app's home screen + "title copied" instead of the title

- **Flow:** BUG_FIX
- **Date:** 2026-11-18
- **Status:** Fixed in v0.3.0 (2026-11-18), awaiting on-phone check

## Done =
On Details, tapping Netflix, Prime Video or another service opens **that app directly on the title's page** (or the website's title page if the app isn't installed). There's no "title copied, paste it in search" step whenever an exact link exists.

## Root cause
`OttLinks` builds **search** URLs (`netflix.com/search?q=…`, `primevideo.com/search?phrase=…`). The OTT apps only register for **title** URLs, not search URLs. `OttOpener` therefore finds no app for the link, falls back to launching the app's home screen, and copies the title to the clipboard. TMDB's API doesn't provide each service's title id, which is why the Phase 1 plan postponed exact links.

## What I found (live, 2026-11-18)
TMDB's public watch page (`themoviedb.org/{movie|tv}/{id}/watch?locale=IN`, the JustWatch-powered page TMDB links to) contains one JustWatch click-out link per service. Each link carries:
- the **exact title URL** (`r=` parameter);
- the **TMDB provider id** (in its base64 `cx` data).

Verified:
- Jawan → Netflix `https://www.netflix.com/title/81728124`
- The Family Man → Prime Video `https://app.primevideo.com/detail?gti=amzn1.dv.gti.2cb6867e-…`
- Jawan → Apple TV and YouTube exact title links
- JioHotstar, SonyLIV and ZEE5 aren't checked yet (one request got an error page).

## Fix
1. **Proxy:** add a `GET /links/{movie|tv}/{id}?region=IN` endpoint (same app-token check). It fetches the TMDB watch page and returns `{ providerId: url }`.
   - Only `https://` URLs on an allow-listed set of OTT domains are returned.
   - Results are cached 12 h.
   - Unit tests cover the parser and the domain allowlist.
   - **This needs one more `npx wrangler deploy`.**
2. **App:** Details loads these links alongside the page. Tapping a service:
   - opens the exact title URL **in that service's app** if it's installed;
   - otherwise opens it in the browser;
   - only falls back to today's search and copy behaviour when there's no exact link.
3. **Netflix:** for movies, use `netflix.com/watch/{id}`, which starts playback straight away. This needs a check on your phone.

## Important limits (please read)
- **Autoplay:** apart from Netflix movies, apps open on the **title's page**; you tap ▶ Play once. Prime Video, JioHotstar and the others don't offer a public "start playing" link to third-party apps.
- **Terms:** this reads TMDB's **website**, not its API. TMDB's terms discourage scraping, and the page layout can change and break the links. When it breaks, the app automatically falls back to today's behaviour. It's acceptable for a personal app, but not something to ship on the Play Store.
- **Rejected alternative:** JustWatch's private API. It's equally unofficial, needs more code, and is a bigger terms risk.

## Result (2026-11-18)
- Proxy `src/links.ts` + `/links/{movie|tv}/{id}?region=` route. Proxy tests: **19 pass** (8 new), `tsc` clean. Parser checked against saved real pages: Jawan → Netflix `title/81728124`, Apple TV, YouTube; The Family Man → Prime `app.primevideo.com/detail?gti=…`.
- App: `OttLinks.exactTarget` (Netflix movie → `/watch/`), `OttOpener` exact-first, Details loads links in the background. Without the redeployed proxy the app silently falls back to the old behaviour.
- **Needs:** `npx wrangler deploy` from `proxy/`, then an on-phone check (especially whether Netflix `/watch/` starts playback, and JioHotstar).
