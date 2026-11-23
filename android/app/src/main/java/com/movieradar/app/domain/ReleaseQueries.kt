package com.movieradar.app.domain

import java.time.YearMonth

/** One TMDB `discover` query and how many result pages (20 each) to read from it. */
data class ReleaseQuery(val type: MediaType, val params: Map<String, String>, val pages: Int)

enum class ReleaseTab { MOVIES, SERIES, OTT }

/**
 * Queries behind Upcoming. Pure so the parameters are unit-tested.
 *
 * "On OTT" can't rely on streaming-provider data: a title only gets it once it's actually on a
 * service, so future days would always be empty. Instead (verified against live TMDB, 2026-11-18):
 *  - movies: premiere date in the month AND a digital release in the month, in the US (global
 *    originals, e.g. Prime's "Perfect Liars") and in the user's region (local OTT releases);
 *  - series: shows premiering in the month on the main streaming networks;
 *  - plus titles already streaming in the region, so recent arrivals aren't lost.
 */
object ReleaseQueries {
    /** TMDB network ids: Netflix, Prime Video, Disney+ Hotstar, SonyLIV, ZEE5, Apple TV, Disney+. */
    const val STREAMING_NETWORKS = "213|1024|3919|2646|2590|2552|2739"

    fun forTab(tab: ReleaseTab, month: YearMonth, region: String): List<ReleaseQuery> {
        val from = month.atDay(1).toString()
        val to = month.atEndOfMonth().toString()
        fun base(type: MediaType) = buildMap {
            val field = if (type == MediaType.MOVIE) "primary_release_date" else "first_air_date"
            put("$field.gte", from); put("$field.lte", to)
            put("sort_by", "popularity.desc"); put("include_adult", "false")
        }
        fun digitalPremieres(country: String, pages: Int) = ReleaseQuery(
            MediaType.MOVIE,
            base(MediaType.MOVIE) + mapOf(
                "release_date.gte" to from, "release_date.lte" to to,
                "region" to country, "with_release_type" to "4",
            ),
            pages,
        )
        fun alreadyStreaming(type: MediaType) = ReleaseQuery(
            type, base(type) + mapOf("watch_region" to region, "with_watch_monetization_types" to "flatrate"), 2,
        )
        return when (tab) {
            ReleaseTab.MOVIES -> listOf(ReleaseQuery(MediaType.MOVIE, base(MediaType.MOVIE), 3))
            ReleaseTab.SERIES -> listOf(ReleaseQuery(MediaType.TV, base(MediaType.TV), 3))
            ReleaseTab.OTT -> buildList {
                add(digitalPremieres("US", 5))
                if (region != "US") add(digitalPremieres(region, 3))
                add(ReleaseQuery(MediaType.TV, base(MediaType.TV) + ("with_networks" to STREAMING_NETWORKS), 3))
                add(alreadyStreaming(MediaType.MOVIE))
                add(alreadyStreaming(MediaType.TV))
            }
        }
    }
}
