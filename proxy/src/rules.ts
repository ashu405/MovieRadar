/** Pure request rules for the proxy (no Worker globals, so they are unit-testable). */

export const TMDB_BASE = "https://api.themoviedb.org/3";

const HOUR = 3600;

/** Allowed TMDB read endpoints and how long each response may be cached (seconds). */
const RULES: ReadonlyArray<{ pattern: RegExp; ttl: number }> = [
  { pattern: /^search\/(multi|movie|tv)$/, ttl: 1 * HOUR },
  { pattern: /^trending\/(all|movie|tv)\/(day|week)$/, ttl: 3 * HOUR },
  { pattern: /^discover\/(movie|tv)$/, ttl: 6 * HOUR },
  { pattern: /^movie\/(popular|upcoming|now_playing|top_rated)$/, ttl: 6 * HOUR },
  { pattern: /^tv\/(popular|on_the_air|top_rated|airing_today)$/, ttl: 6 * HOUR },
  { pattern: /^(movie|tv)\/\d{1,10}$/, ttl: 24 * HOUR },
  { pattern: /^(movie|tv)\/\d{1,10}\/(watch\/providers|recommendations|similar)$/, ttl: 12 * HOUR },
  { pattern: /^tv\/\d{1,10}\/season\/\d{1,4}$/, ttl: 24 * HOUR },
  { pattern: /^person\/\d{1,10}$/, ttl: 24 * HOUR },
  { pattern: /^collection\/\d{1,10}$/, ttl: 24 * HOUR },
  { pattern: /^search\/collection$/, ttl: 1 * HOUR },
  { pattern: /^genre\/(movie|tv)\/list$/, ttl: 7 * 24 * HOUR },
  { pattern: /^watch\/providers\/(movie|tv|regions)$/, ttl: 7 * 24 * HOUR },
  { pattern: /^configuration\/countries$/, ttl: 7 * 24 * HOUR },
];

/** Query params the client may never set (auth is added by the proxy only). */
const BLOCKED_PARAMS = new Set(["api_key", "session_id", "guest_session_id"]);
const MAX_QUERY_LENGTH = 1024;

export type Decision =
  | { ok: true; upstream: string; ttl: number }
  | { ok: false; status: number; message: string };

export function decide(method: string, url: URL): Decision {
  if (method !== "GET") return { ok: false, status: 405, message: "Only GET is allowed" };
  if (!url.pathname.startsWith("/tmdb/")) return { ok: false, status: 404, message: "Not found" };

  const path = url.pathname.slice("/tmdb/".length);
  const rule = RULES.find((r) => r.pattern.test(path));
  if (!rule) return { ok: false, status: 403, message: "Endpoint not allowed" };
  if (url.search.length > MAX_QUERY_LENGTH) return { ok: false, status: 414, message: "Query too long" };

  const params = new URLSearchParams();
  for (const [k, v] of url.searchParams) {
    if (!BLOCKED_PARAMS.has(k.toLowerCase())) params.append(k, v);
  }
  params.sort(); // stable cache key regardless of param order
  const qs = params.toString();
  return { ok: true, upstream: `${TMDB_BASE}/${path}${qs ? `?${qs}` : ""}`, ttl: rule.ttl };
}

/** Constant-time string comparison so the token can't be guessed by timing. */
export function safeEqual(a: string, b: string): boolean {
  const enc = new TextEncoder();
  const x = enc.encode(a);
  const y = enc.encode(b);
  let diff = x.length ^ y.length;
  for (let i = 0; i < Math.max(x.length, y.length); i++) diff |= (x[i] ?? 0) ^ (y[i] ?? 0);
  return diff === 0;
}
