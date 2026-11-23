package com.movieradar.app.domain

import java.net.URLEncoder

/**
 * How to open a streaming service for a title (plan option "a"): the service's app if installed,
 * otherwise its website search. Package names and search URLs are best-effort and verified on device;
 * unknown services fall back to TMDB's "watch" page (powered by JustWatch).
 */
data class OttTarget(val packageName: String?, val searchUrl: String, val isExact: Boolean = false)

object OttLinks {
    private val table: Map<Int, Pair<String?, String>> = mapOf(
        8 to ("com.netflix.mediaclient" to "https://www.netflix.com/search?q=%s"),
        175 to ("com.netflix.mediaclient" to "https://www.netflix.com/search?q=%s"),
        119 to ("com.amazon.avod.thirdpartyclient" to "https://www.primevideo.com/search?phrase=%s"),
        9 to ("com.amazon.avod.thirdpartyclient" to "https://www.primevideo.com/search?phrase=%s"),
        2336 to ("in.startv.hotstar" to "https://www.hotstar.com/in/explore?search_query=%s"),
        122 to ("in.startv.hotstar" to "https://www.hotstar.com/in/explore?search_query=%s"),
        232 to ("com.graymatrix.did" to "https://www.zee5.com/search?q=%s"),
        237 to ("com.sonyliv" to "https://www.sonyliv.com/search?searchTerm=%s"),
        309 to ("com.suntv.sunnxt" to "https://www.sunnxt.com/search?q=%s"),
        515 to ("com.mxtech.videoplayer.ad" to "https://www.mxplayer.in/search?q=%s"),
        192 to ("com.google.android.youtube" to "https://www.youtube.com/results?search_query=%s"),
        283 to ("com.crunchyroll.crunchyroid" to "https://www.crunchyroll.com/search?q=%s"),
        11 to ("com.mubi" to "https://mubi.com/en/search/films?query=%s"),
        73 to ("com.tubitv" to "https://tubitv.com/search/%s"),
        350 to (null to "https://tv.apple.com/search?term=%s"),
        2 to (null to "https://tv.apple.com/search?term=%s"),
        3 to ("com.google.android.videos" to "https://play.google.com/store/search?q=%s&c=movies"),
        337 to ("com.disney.disneyplus" to "https://www.disneyplus.com/search?q=%s"),
        1899 to ("com.wbd.stream" to "https://play.max.com/search?q=%s"),
    )

    val knownPackages: Set<String> get() = table.values.mapNotNull { it.first }.toSet()

    /**
     * Exact title link from the proxy, opened in the service's own app when known.
     * Netflix movies use /watch/ so playback starts straight away; series keep /title/ (episode picker).
     */
    fun exactTarget(providerId: Int, exactUrl: String, isMovie: Boolean): OttTarget? {
        if (!exactUrl.startsWith("https://")) return null
        val url = if (isMovie) NETFLIX_TITLE.replace(exactUrl) { "https://www.netflix.com/watch/${it.groupValues[1]}" } else exactUrl
        return OttTarget(table[providerId]?.first ?: packageForUrl(url), url, isExact = true)
    }

    private val NETFLIX_TITLE = Regex("^https://www\\.netflix\\.com/(?:[a-z]{2}(?:-[a-z]{2})?/)?title/(\\d+)$")

    private fun packageForUrl(url: String): String? {
        val host = runCatching { java.net.URI(url).host.orEmpty().lowercase() }.getOrDefault("")
        return HOST_PACKAGES.entries.firstOrNull { (h, _) -> host == h || host.endsWith(".$h") }?.value
    }

    private val HOST_PACKAGES = mapOf(
        "netflix.com" to "com.netflix.mediaclient",
        "primevideo.com" to "com.amazon.avod.thirdpartyclient",
        "hotstar.com" to "in.startv.hotstar",
        "zee5.com" to "com.graymatrix.did",
        "sonyliv.com" to "com.sonyliv",
        "youtube.com" to "com.google.android.youtube",
        "disneyplus.com" to "com.disney.disneyplus",
    )

    fun targetFor(providerId: Int, title: String, fallbackLink: String?): OttTarget? {
        val q = URLEncoder.encode(title, Charsets.UTF_8.name())
        table[providerId]?.let { (pkg, url) -> return OttTarget(pkg, url.format(q)) }
        return fallbackLink?.takeIf { it.startsWith("https://") }?.let { OttTarget(null, it) }
    }
}
