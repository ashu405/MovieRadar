package com.movieradar.app.data

import com.movieradar.app.data.remote.TraktItemDto
import com.movieradar.app.domain.MediaType

/** A Trakt list item reduced to what we need: which TMDB title to show. */
data class TitleRef(val type: MediaType, val tmdbId: Int)

enum class TraktList(val path: String, val title: String) {
    TRENDING("trending", "🔥 Trending now"),
    MOST_WATCHED("watched/weekly", "👀 Most watched this week"),
    ANTICIPATED("anticipated", "⏳ Most anticipated"),
}

object TraktRows {
    /** Movies and series alternated (movie, series, movie…), Trakt order kept, no duplicates, items without a TMDB id dropped. */
    fun merge(movies: List<TraktItemDto>, shows: List<TraktItemDto>): List<TitleRef> {
        val m = movies.mapNotNull { it.movie?.ids?.tmdb?.let { id -> TitleRef(MediaType.MOVIE, id) } }
        val s = shows.mapNotNull { it.show?.ids?.tmdb?.let { id -> TitleRef(MediaType.TV, id) } }
        return buildList {
            for (i in 0 until maxOf(m.size, s.size)) {
                m.getOrNull(i)?.let(::add); s.getOrNull(i)?.let(::add)
            }
        }.distinct()
    }
}
