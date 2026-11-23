/**
 * Exact "open this title" links per streaming service.
 *
 * TMDB's API has no per-service title URLs, but its public watch page
 * (themoviedb.org/{movie|tv}/{id}/watch?locale=XX, powered by JustWatch) contains one
 * click.justwatch.com link per offer. Each link carries:
 *   - r  = the service's own title URL (e.g. https://www.netflix.com/title/81728124)
 *   - cx = base64url JSON with the TMDB provider id
 * Pure functions here so they're unit-testable; the fetch lives in index.ts.
 */

const HOUR = 3600;
export const LINKS_TTL = 12 * HOUR;

/** Only links to these OTT domains (or their subdomains) are ever returned to the app. */
const ALLOWED_HOSTS = [
  "netflix.com", "primevideo.com", "amazon.com", "amazon.in", "hotstar.com", "jiocinema.com",
  "sonyliv.com", "zee5.com", "sunnxt.com", "mxplayer.in", "youtube.com", "apple.com",
  "play.google.com", "disneyplus.com", "max.com", "crunchyroll.com", "mubi.com", "tubitv.com",
  "aha.video", "hoichoi.tv", "erosnow.com", "airtelxstream.in", "lionsgateplay.com", "jio.com",
  "hulu.com", "paramountplus.com", "peacocktv.com", "starz.com", "plex.tv", "pluto.tv",
];

/** Preference when one service has several offers for the same title. */
const RANK: Record<string, number> = { flatrate: 0, free: 1, ads: 2, rent: 3, buy: 4 };

export type LinkRoute = { ok: true; upstream: string } | { ok: false; status: number; message: string };

export function linksRoute(url: URL): LinkRoute | null {
  const m = /^\/links\/(movie|tv)\/(\d{1,10})$/.exec(url.pathname);
  if (!m) return null;
  const region = (url.searchParams.get("region") ?? "IN").toUpperCase();
  if (!/^[A-Z]{2}$/.test(region)) return { ok: false, status: 400, message: "Invalid region" };
  return { ok: true, upstream: `https://www.themoviedb.org/${m[1]}/${m[2]}/watch?locale=${region}` };
}

export function isAllowedTarget(raw: string): boolean {
  try {
    const u = new URL(raw);
    if (u.protocol !== "https:") return false;
    const host = u.hostname.toLowerCase();
    return ALLOWED_HOSTS.some((h) => host === h || host.endsWith(`.${h}`));
  } catch {
    return false;
  }
}

function decodeCx(cx: string): { providerId?: number; monetizationType?: string } {
  try {
    const b64 = cx.replace(/-/g, "+").replace(/_/g, "/");
    const padded = b64 + "=".repeat((4 - (b64.length % 4)) % 4);
    const parsed = JSON.parse(atob(padded));
    const data = parsed?.data?.find?.((d: { data?: { providerId?: unknown } }) => d?.data?.providerId != null)?.data;
    return { providerId: Number(data?.providerId), monetizationType: String(data?.monetizationType ?? "") };
  } catch {
    return {};
  }
}

/** { tmdbProviderId: titleUrl } from the watch page HTML. */
export function parseLinks(html: string): Record<number, string> {
  const best = new Map<number, { url: string; rank: number }>();
  const re = /href="(https:\/\/click\.justwatch\.com\/a\?[^"]+)"/g;
  for (let m = re.exec(html); m; m = re.exec(html)) {
    let link: URL;
    try {
      link = new URL(m[1].replace(/&amp;/g, "&"));
    } catch {
      continue;
    }
    const target = link.searchParams.get("r");
    const { providerId, monetizationType } = decodeCx(link.searchParams.get("cx") ?? "");
    if (!target || !providerId || !Number.isInteger(providerId) || !isAllowedTarget(target)) continue;
    const rank = RANK[monetizationType ?? ""] ?? 9;
    const cur = best.get(providerId);
    if (!cur || rank < cur.rank) best.set(providerId, { url: target, rank });
  }
  return Object.fromEntries([...best].map(([id, v]) => [id, v.url]));
}
