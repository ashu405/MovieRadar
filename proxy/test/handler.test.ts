import { afterEach, describe, expect, it, vi } from "vitest";
import { handle, type Env } from "../src/index";

const env: Env = { TMDB_TOKEN: "tmdb-secret", APP_TOKEN: "app-secret" };

function memoryCache(): Cache {
  const store = new Map<string, Response>();
  return {
    match: async (r: RequestInfo) => store.get(typeof r === "string" ? r : r.url)?.clone(),
    put: async (r: RequestInfo, res: Response) => void store.set(typeof r === "string" ? r : r.url, res),
  } as unknown as Cache;
}
const ctx = { waitUntil: (p: Promise<unknown>) => void p };
const req = (path: string, token = "app-secret") =>
  new Request(`https://proxy.example${path}`, { headers: { "x-app-token": token } });

afterEach(() => vi.unstubAllGlobals());

describe("handle", () => {
  it("rejects a missing or wrong app token", async () => {
    expect((await handle(req("/tmdb/movie/1", "nope"), env, memoryCache(), ctx)).status).toBe(401);
  });

  it("adds the bearer token upstream, never exposes it, and caches", async () => {
    const fetchMock = vi.fn(async () => new Response('{"id":1}', { status: 200 }));
    vi.stubGlobal("fetch", fetchMock);
    const cache = memoryCache();

    const r1 = await handle(req("/tmdb/movie/1"), env, cache, ctx);
    expect(r1.status).toBe(200);
    expect(await r1.text()).toBe('{"id":1}');
    const [url, init] = fetchMock.mock.calls[0] as unknown as [string, RequestInit];
    expect(url).toBe("https://api.themoviedb.org/3/movie/1");
    expect(url).not.toContain("tmdb-secret");
    expect((init.headers as Record<string, string>).authorization).toBe("Bearer tmdb-secret");

    await new Promise((r) => setTimeout(r, 0));
    const r2 = await handle(req("/tmdb/movie/1"), env, cache, ctx);
    expect(await r2.text()).toBe('{"id":1}');
    expect(fetchMock).toHaveBeenCalledTimes(1);
  });

  it("does not cache upstream errors", async () => {
    const fetchMock = vi.fn(async () => new Response("{}", { status: 503 }));
    vi.stubGlobal("fetch", fetchMock);
    const cache = memoryCache();
    await handle(req("/tmdb/movie/2"), env, cache, ctx);
    await handle(req("/tmdb/movie/2"), env, cache, ctx);
    expect(fetchMock).toHaveBeenCalledTimes(2);
  });

  it("returns 502 when the upstream fetch throws", async () => {
    vi.stubGlobal("fetch", vi.fn(async () => { throw new Error("network down"); }));
    const r = await handle(req("/tmdb/movie/3"), env, memoryCache(), ctx);
    expect(r.status).toBe(502);
  });

  it("fails closed when secrets are missing", async () => {
    const r = await handle(req("/tmdb/movie/1"), { TMDB_TOKEN: "", APP_TOKEN: "" }, memoryCache(), ctx);
    expect(r.status).toBe(500);
  });
});
