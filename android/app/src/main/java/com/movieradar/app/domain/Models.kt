package com.movieradar.app.domain

enum class MediaType(val path: String) {
    MOVIE("movie"), TV("tv");

    companion object {
        fun fromPath(p: String?): MediaType? = entries.firstOrNull { it.path == p }
    }
}

data class TitleSummary(
    val id: Int,
    val type: MediaType,
    val title: String,
    val year: String?,
    val posterPath: String?,
    val backdropPath: String?,
    val overview: String,
    val rating: Double,
    val genreIds: List<Int>,
    /** Full yyyy-MM-dd release / first-air date when known (used by Upcoming). */
    val releaseDate: String? = null,
)

val TitleSummary.key: String get() = "${type.path}-$id"


data class Provider(val id: Int, val name: String, val logoPath: String?)

data class WhereToWatch(
    val stream: List<Provider>,
    val free: List<Provider>,
    val rent: List<Provider>,
    val buy: List<Provider>,
    val tmdbLink: String?,
) {
    val isEmpty get() = stream.isEmpty() && free.isEmpty() && rent.isEmpty() && buy.isEmpty()
    /** Logos worth showing on a card: subscription first, then free. */
    val headline get() = (stream + free).distinctBy { it.id }

    companion object {
        val EMPTY = WhereToWatch(emptyList(), emptyList(), emptyList(), emptyList(), null)
    }
}

data class CastMember(val name: String, val character: String?, val profilePath: String?, val id: Int = 0)
data class Season(val number: Int, val name: String, val episodeCount: Int, val airDate: String?)

data class TitleDetails(
    val summary: TitleSummary,
    val tagline: String?,
    val genres: List<String>,
    val runtimeMinutes: Int?,
    val seasonCount: Int?,
    val seasons: List<Season>,
    val certification: String?,
    val ageBucket: AgeBucket?,
    val cast: List<CastMember>,
    val trailerYoutubeKey: String?,
    val whereToWatch: WhereToWatch,
    /** Franchise this movie belongs to (TMDB collection), if any. */
    val franchise: FranchiseRef? = null,
)

data class Region(val code: String, val name: String)

/** A home-screen row. */
data class Shelf(val title: String, val items: List<TitleSummary>)
