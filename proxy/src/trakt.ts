/**
 * Trakt lists (real audience data). The app calls /trakt/...; the proxy adds the Trakt
 * client id on the server so it never ships in the APK. Pure routing here; fetch in index.ts.
 */

const HOUR = 3600;
export const TRAKT_BASE = "https://api.trakt.tv";

export type TraktRoute =
  | { ok: true; upstream: string; ttl: number }
  | { ok: false; status: number; message: string };

const LISTS: Record<string, number> = {
  trending: 1 * HOUR,
  "watched/weekly": 6 * HOUR,
  anticipated: 6 * HOUR,
};

export function traktRoute(url: URL): TraktRoute | null {
  if (!url.pathname.startsWith("/trakt/")) return null;
  const m = /^\/trakt\/(movies|shows)\/(trending|watched\/weekly|anticipated)$/.exec(url.pathname);
  if (!m) return { ok: false, status: 403, message: "Endpoint not allowed" };
  const limit = Number(url.searchParams.get("limit") ?? "20");
  if (!Number.isInteger(limit) || limit < 1 || limit > 30) return { ok: false, status: 400, message: "limit must be 1-30" };
  // Only `limit` is forwarded; everything else the client sends is dropped.
  return { ok: true, upstream: `${TRAKT_BASE}/${m[1]}/${m[2]}?limit=${limit}`, ttl: LISTS[m[2]] };
}
