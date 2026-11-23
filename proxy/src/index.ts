import { LINKS_TTL, linksRoute, parseLinks } from "./links";
import { decide, safeEqual } from "./rules";
import { traktRoute } from "./trakt";

export interface Env {
  TMDB_TOKEN: string;
  APP_TOKEN: string;
  TRAKT_CLIENT_ID?: string;
}

const json = (status: number, body: unknown) =>
  new Response(JSON.stringify(body), {
    status,
    headers: { "content-type": "application/json; charset=utf-8" },
  });

export async function handle(
  request: Request,
  env: Env,
  cache: Cache,
  ctx: Pick<ExecutionContext, "waitUntil">,
): Promise<Response> {
  if (!env.APP_TOKEN || !env.TMDB_TOKEN) return json(500, { error: "Proxy not configured" });

  const token = request.headers.get("x-app-token") ?? "";
  if (!safeEqual(token, env.APP_TOKEN)) return json(401, { error: "Unauthorized" });

  const url = new URL(request.url);
  const lr = request.method === "GET" ? linksRoute(url) : null;
  if (lr) return lr.ok ? links(lr.upstream, cache, ctx) : json(lr.status, { error: lr.message });

  const tr = request.method === "GET" ? traktRoute(url) : null;
  if (tr) {
    if (!tr.ok) return json(tr.status, { error: tr.message });
    if (!env.TRAKT_CLIENT_ID) return json(503, { error: "Trakt not configured" });
    return trakt(tr.upstream, tr.ttl, env.TRAKT_CLIENT_ID, cache, ctx);
  }

  const d = decide(request.method, url);
  if (!d.ok) return json(d.status, { error: d.message });

  // Cache key = the upstream URL (never contains secrets).
  const cacheKey = new Request(d.upstream, { method: "GET" });
  const hit = await cache.match(cacheKey);
  if (hit) return hit;

  let upstream: Response;
  try {
    upstream = await fetch(d.upstream, {
      headers: { authorization: `Bearer ${env.TMDB_TOKEN}`, accept: "application/json" },
    });
  } catch {
    return json(502, { error: "Upstream unavailable, try again" });
  }

  const res = new Response(upstream.body, {
    status: upstream.status,
    headers: {
      "content-type": upstream.headers.get("content-type") ?? "application/json",
      "cache-control": upstream.ok ? `public, max-age=${d.ttl}` : "no-store",
    },
  });
  if (upstream.ok) ctx.waitUntil(cache.put(cacheKey, res.clone()));
  return res;
}

/** Trakt list with the client id added server-side; only successful responses are cached. */
async function trakt(
  upstreamUrl: string,
  ttl: number,
  clientId: string,
  cache: Cache,
  ctx: Pick<ExecutionContext, "waitUntil">,
): Promise<Response> {
  const cacheKey = new Request(upstreamUrl, { method: "GET" });
  const hit = await cache.match(cacheKey);
  if (hit) return hit;
  let up: Response;
  try {
    up = await fetch(upstreamUrl, {
      headers: { "content-type": "application/json", "trakt-api-version": "2", "trakt-api-key": clientId },
    });
  } catch {
    return json(502, { error: "Upstream unavailable, try again" });
  }
  if (!up.ok) return json(502, { error: `Trakt returned ${up.status}` });
  const res = new Response(up.body, {
    status: 200,
    headers: { "content-type": "application/json; charset=utf-8", "cache-control": `public, max-age=${ttl}` },
  });
  ctx.waitUntil(cache.put(cacheKey, res.clone()));
  return res;
}

/** Exact per-service title links scraped from TMDB's public watch page (see links.ts). */
async function links(upstreamUrl: string, cache: Cache, ctx: Pick<ExecutionContext, "waitUntil">): Promise<Response> {
  const cacheKey = new Request(`${upstreamUrl}&mr=links-v1`, { method: "GET" });
  const hit = await cache.match(cacheKey);
  if (hit) return hit;
  let page: Response;
  try {
    page = await fetch(upstreamUrl, { headers: { accept: "text/html", "user-agent": "Mozilla/5.0 (MovieRadar personal proxy)" } });
  } catch {
    return json(502, { error: "Upstream unavailable, try again" });
  }
  if (page.status === 404) return json(200, { links: {} });
  if (!page.ok) return json(502, { error: `Watch page returned ${page.status}` });
  const res = new Response(JSON.stringify({ links: parseLinks(await page.text()) }), {
    status: 200,
    headers: { "content-type": "application/json; charset=utf-8", "cache-control": `public, max-age=${LINKS_TTL}` },
  });
  ctx.waitUntil(cache.put(cacheKey, res.clone()));
  return res;
}

export default {
  fetch(request: Request, env: Env, ctx: ExecutionContext): Promise<Response> {
    return handle(request, env, caches.default, ctx);
  },
} satisfies ExportedHandler<Env>;
