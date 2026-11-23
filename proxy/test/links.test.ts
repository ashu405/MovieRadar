import { afterEach, describe, expect, it, vi } from "vitest";
import { handle, type Env } from "../src/index";
import { isAllowedTarget, linksRoute, parseLinks } from "../src/links";

/** Builds a click-out link shaped like the ones on TMDB's watch page. */
function clickout(providerId: number, monetizationType: string, target: string): string {
  const cx = btoa(
    JSON.stringify({ schema: "x", data: [{ schema: "click", data: { provider: "P", providerId, monetizationType } }] }),
  ).replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/, "");
  return `<a href="https://click.justwatch.com/a?cx=${cx}&amp;r=${encodeURIComponent(target)}&amp;uct_country=in">x</a>`;
}

describe("parseLinks", () => {
  it("maps TMDB provider ids to exact title urls", () => {
    const html =
      clickout(8, "flatrate", "https://www.netflix.com/title/81728124") +
      clickout(119, "flatrate", "https://app.primevideo.com/detail?gti=amzn1.dv.gti.abc");
    expect(parseLinks(html)).toEqual({
      8: "https://www.netflix.com/title/81728124",
      119: "https://app.primevideo.com/detail?gti=amzn1.dv.gti.abc",
    });
  });

  it("prefers the subscription offer over rent/buy for the same service", () => {
    const html =
      clickout(2, "buy", "https://tv.apple.com/in/movie/x?buy=1") +
      clickout(2, "flatrate", "https://tv.apple.com/in/movie/x");
    expect(parseLinks(html)[2]).toBe("https://tv.apple.com/in/movie/x");
  });

  it("drops non-OTT domains, plain http and broken entries", () => {
    const html =
      clickout(8, "flatrate", "https://evil.example/phish") +
      clickout(9, "flatrate", "http://www.primevideo.com/detail") +
      `<a href="https://click.justwatch.com/a?cx=not-base64&amp;r=https%3A%2F%2Fwww.netflix.com%2Ftitle%2F1">x</a>`;
    expect(parseLinks(html)).toEqual({});
  });

  it("returns nothing for a page without offers", () => {
    expect(parseLinks("<html><body>No offers</body></html>")).toEqual({});
  });
});

describe("isAllowedTarget", () => {
  it("accepts subdomains of allowed hosts only", () => {
    expect(isAllowedTarget("https://www.hotstar.com/in/shows/x/123")).toBe(true);
    expect(isAllowedTarget("https://netflix.com.evil.example/title/1")).toBe(false);
    expect(isAllowedTarget("javascript:alert(1)")).toBe(false);
  });
});

describe("linksRoute", () => {
  it("builds the watch page url and validates input", () => {
    expect(linksRoute(new URL("https://p/links/movie/872906?region=in"))).toEqual({
      ok: true,
      upstream: "https://www.themoviedb.org/movie/872906/watch?locale=IN",
    });
    expect(linksRoute(new URL("https://p/links/tv/1?region=IND"))).toMatchObject({ ok: false, status: 400 });
    expect(linksRoute(new URL("https://p/links/person/1"))).toBeNull();
  });
});

describe("handle /links", () => {
  const env: Env = { TMDB_TOKEN: "tmdb-secret", APP_TOKEN: "app-secret" };
  const ctx = { waitUntil: (p: Promise<unknown>) => void p };
  const store = new Map<string, Response>();
  const cache = {
    match: async (r: Request) => store.get(r.url)?.clone(),
    put: async (r: Request, res: Response) => void store.set(r.url, res),
  } as unknown as Cache;
  afterEach(() => vi.unstubAllGlobals());

  it("requires the app token", async () => {
    const r = await handle(new Request("https://p/links/movie/1"), env, cache, ctx);
    expect(r.status).toBe(401);
  });

  it("fetches the page without the TMDB token, returns links, and caches", async () => {
    const fetchMock = vi.fn(async () => new Response(clickout(8, "flatrate", "https://www.netflix.com/title/42")));
    vi.stubGlobal("fetch", fetchMock);
    const req = () => new Request("https://p/links/movie/7?region=IN", { headers: { "x-app-token": "app-secret" } });

    const r1 = await handle(req(), env, cache, ctx);
    expect(await r1.json()).toEqual({ links: { 8: "https://www.netflix.com/title/42" } });
    const init = fetchMock.mock.calls[0] as unknown as [string, RequestInit];
    expect(JSON.stringify(init[1]?.headers)).not.toContain("tmdb-secret");

    await handle(req(), env, cache, ctx);
    expect(fetchMock).toHaveBeenCalledTimes(1);
  });
});
