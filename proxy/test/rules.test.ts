import { describe, expect, it } from "vitest";
import { decide, safeEqual } from "../src/rules";

const u = (p: string) => new URL(`https://proxy.example${p}`);

describe("decide", () => {
  it("allows search and forwards query", () => {
    const d = decide("GET", u("/tmdb/search/multi?query=Dune&language=en-IN"));
    expect(d).toEqual({
      ok: true,
      upstream: "https://api.themoviedb.org/3/search/multi?language=en-IN&query=Dune",
      ttl: 3600,
    });
  });

  it("allows details, watch providers, season, discover, genres", () => {
    for (const p of [
      "/tmdb/movie/438631",
      "/tmdb/tv/1399/watch/providers",
      "/tmdb/tv/1399/season/2",
      "/tmdb/discover/movie?with_genres=28",
      "/tmdb/genre/tv/list",
      "/tmdb/watch/providers/regions",
      "/tmdb/trending/all/week",
    ]) expect(decide("GET", u(p)).ok, p).toBe(true);
  });

  it("allows a person with their credits, but not other person endpoints", () => {
    const d = decide("GET", u("/tmdb/person/35742?append_to_response=combined_credits"));
    expect(d).toEqual({
      ok: true,
      upstream: "https://api.themoviedb.org/3/person/35742?append_to_response=combined_credits",
      ttl: 86400,
    });
    expect(decide("GET", u("/tmdb/person/35742/images")).ok).toBe(false);
    expect(decide("GET", u("/tmdb/person/popular")).ok).toBe(false);
  });

  it("allows franchise (collection) details and search only", () => {
    expect(decide("GET", u("/tmdb/collection/483464")).ok).toBe(true);
    expect(decide("GET", u("/tmdb/search/collection?query=Golmaal"))).toMatchObject({ ok: true, ttl: 3600 });
    expect(decide("GET", u("/tmdb/collection/483464/images")).ok).toBe(false);
  });

  it("strips client-supplied auth params", () => {
    const d = decide("GET", u("/tmdb/movie/1?api_key=stolen&API_KEY=x&language=en"));
    expect(d.ok && d.upstream).toBe("https://api.themoviedb.org/3/movie/1?language=en");
  });

  it("rejects non-GET, unknown paths and write endpoints", () => {
    expect(decide("POST", u("/tmdb/movie/1"))).toMatchObject({ ok: false, status: 405 });
    expect(decide("GET", u("/other"))).toMatchObject({ ok: false, status: 404 });
    for (const p of [
      "/tmdb/movie/1/rating",
      "/tmdb/account/1",
      "/tmdb/authentication/token/new",
      "/tmdb/movie/abc",
      "/tmdb/movie/1/../../account",
      "/tmdb/",
    ]) expect(decide("GET", u(p)), p).toMatchObject({ ok: false, status: 403 });
  });

  it("rejects overly long queries", () => {
    expect(decide("GET", u(`/tmdb/search/multi?query=${"a".repeat(2000)}`))).toMatchObject({ status: 414 });
  });
});

describe("safeEqual", () => {
  it("compares exactly", () => {
    expect(safeEqual("abc", "abc")).toBe(true);
    expect(safeEqual("abc", "abd")).toBe(false);
    expect(safeEqual("abc", "abcd")).toBe(false);
    expect(safeEqual("", "x")).toBe(false);
  });
});
