import { afterEach, describe, expect, it, vi } from "vitest";
import { handle, type Env } from "../src/index";
import { traktRoute } from "../src/trakt";

describe("traktRoute", () => {
  it("allows only the three list endpoints and forwards just the limit", () => {
    expect(traktRoute(new URL("https://p/trakt/movies/trending?limit=10&extended=full"))).toEqual({
      ok: true, upstream: "https://api.trakt.tv/movies/trending?limit=10", ttl: 3600,
    });
    expect(traktRoute(new URL("https://p/trakt/shows/watched/weekly"))).toMatchObject({ ok: true, ttl: 21600 });
    expect(traktRoute(new URL("https://p/trakt/movies/anticipated?limit=30"))).toMatchObject({ ok: true });
  });

  it("rejects other Trakt paths, bad limits, and ignores non-trakt paths", () => {
    expect(traktRoute(new URL("https://p/trakt/users/me/history"))).toMatchObject({ ok: false, status: 403 });
    expect(traktRoute(new URL("https://p/trakt/movies/trending?limit=500"))).toMatchObject({ ok: false, status: 400 });
    expect(traktRoute(new URL("https://p/trakt/movies/trending?limit=abc"))).toMatchObject({ ok: false, status: 400 });
    expect(traktRoute(new URL("https://p/tmdb/movie/1"))).toBeNull();
  });
});

describe("handle /trakt", () => {
  const ctx = { waitUntil: (p: Promise<unknown>) => void p };
  const store = new Map<string, Response>();
  const cache = {
    match: async (r: Request) => store.get(r.url)?.clone(),
    put: async (r: Request, res: Response) => void store.set(r.url, res),
  } as unknown as Cache;
  const req = (token = "app-secret") =>
    new Request("https://p/trakt/movies/trending?limit=2", { headers: { "x-app-token": token } });
  afterEach(() => vi.unstubAllGlobals());

  it("requires the app token", async () => {
    const env: Env = { TMDB_TOKEN: "t", APP_TOKEN: "app-secret", TRAKT_CLIENT_ID: "trakt-id" };
    expect((await handle(req("nope"), env, cache, ctx)).status).toBe(401);
  });

  it("returns 503 when the Trakt id isn't configured", async () => {
    const env: Env = { TMDB_TOKEN: "t", APP_TOKEN: "app-secret" };
    expect((await handle(req(), env, cache, ctx)).status).toBe(503);
  });

  it("adds the Trakt key server-side, never the TMDB token, and caches", async () => {
    const fetchMock = vi.fn(async () => new Response('[{"watchers":5,"movie":{"ids":{"tmdb":1}}}]'));
    vi.stubGlobal("fetch", fetchMock);
    const env: Env = { TMDB_TOKEN: "tmdb-secret", APP_TOKEN: "app-secret", TRAKT_CLIENT_ID: "trakt-id" };
    const r = await handle(req(), env, cache, ctx);
    expect(r.status).toBe(200);
    const headers = JSON.stringify((fetchMock.mock.calls[0] as unknown as [string, RequestInit])[1]?.headers);
    expect(headers).toContain("trakt-id");
    expect(headers).not.toContain("tmdb-secret");
    await handle(req(), env, cache, ctx);
    expect(fetchMock).toHaveBeenCalledTimes(1);
  });
});
