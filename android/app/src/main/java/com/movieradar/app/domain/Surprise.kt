package com.movieradar.app.domain

import kotlin.random.Random

/** 🎲 Surprise me moods → TMDB discover params. Genre ids are TMDB's (movie / TV differ). */
enum class Mood(val label: String, val emoji: String, val adultOnly: Boolean = false) {
    FEEL_GOOD("Feel-good", "😄"),
    LAUGH("Laugh out loud", "😂"),
    THRILLER("Thriller night", "🔪"),
    ROMANTIC("Romantic", "💕"),
    FAMILY("Family", "👨‍👩‍👧"),
    MIND_BENDING("Mind-bending", "🤯"),
    SCARY("Scary", "👻"),
    ACTION("Action-packed", "💥"),
    STEAMY("Steamy", "🔥", adultOnly = true);

    val title: String get() = "$emoji $label"

    /** Genre / keyword params for this mood ("|" = any of, "," = all of). */
    fun params(type: MediaType): Map<String, String> {
        val movie = type == MediaType.MOVIE
        return when (this) {
            FEEL_GOOD -> mapOf(
                "with_genres" to if (movie) "35|10751|10749" else "35|10751",
                "without_genres" to if (movie) "27|53|10752" else "80|10768",
            )
            LAUGH -> mapOf("with_genres" to "35")
            THRILLER -> mapOf("with_genres" to if (movie) "53|80" else "80|9648")
            ROMANTIC -> if (movie) mapOf("with_genres" to "10749") else mapOf("with_genres" to "18", "with_keywords" to "9840")
            FAMILY -> mapOf("with_genres" to if (movie) "10751|16" else "10751|16|10762")
            MIND_BENDING -> mapOf("with_genres" to if (movie) "878|9648" else "10765|9648")
            SCARY -> if (movie) mapOf("with_genres" to "27") else mapOf("with_genres" to "9648", "with_keywords" to "315058|6152")
            ACTION -> mapOf("with_genres" to if (movie) "28|12" else "10759")
            STEAMY -> mapOf("with_keywords" to "${Category.STEAMY_TAGS}|${Adult.TAGS}", "include_adult" to "true")
        }
    }

    companion object {
        /** Moods offered: 🔥 Steamy only in MovieRadar with the 18+ switch on. */
        fun available(adult: Boolean): List<Mood> = entries.filter { !it.adultOnly || adult }
    }
}

enum class TitleKind(val label: String) { MOVIE("Movie"), SERIES("Series"), EITHER("Either") }
enum class Length(val label: String, val maxMinutes: Int?) { ANY("Any length", null), SHORT("Under 100 min", 100), MEDIUM("Under 2 h", 120) }

/** What the 🎲 sheet asks for. [services] = provider ids to search (already resolved from "Any of mine"). */
data class SurpriseRequest(
    val mood: Mood,
    val kind: TitleKind,
    val length: Length,
    val services: List<Int>,
    val region: String,
)

object SurpriseRules {
    const val MIN_RATING = "6.5"
    const val MIN_VOTES = "100"

    /** Full discover params for one media type (a page number is added by the caller). */
    fun params(r: SurpriseRequest, type: MediaType): Map<String, String> = buildMap {
        put("watch_region", r.region)
        put("with_watch_monetization_types", "flatrate|free|ads")
        if (r.services.isNotEmpty()) put("with_watch_providers", r.services.joinToString("|"))
        put("vote_average.gte", MIN_RATING)
        put("vote_count.gte", MIN_VOTES)
        put("sort_by", "popularity.desc")
        put("include_adult", "false")
        if (type == MediaType.MOVIE) r.length.maxMinutes?.let { put("with_runtime.lte", it.toString()) }
        putAll(r.mood.params(type)) // may switch include_adult on (Steamy)
        if (r.mood == Mood.STEAMY) remove("vote_count.gte") // 18+ titles have few votes
    }

    fun types(kind: TitleKind): List<MediaType> = when (kind) {
        TitleKind.MOVIE -> listOf(MediaType.MOVIE)
        TitleKind.SERIES -> listOf(MediaType.TV)
        TitleKind.EITHER -> listOf(MediaType.MOVIE, MediaType.TV)
    }

    /** "Any of mine": the services you ticked, or all five if you ticked none. */
    fun resolveServices(chosen: OttService?, mine: Set<OttService>): List<Int> =
        (chosen?.let { setOf(it) } ?: mine.ifEmpty { OttService.entries.toSet() }).map { it.providerId }.sorted()

    /** Up to [count] random picks from [pool], skipping keys already [shown] this session. */
    fun pick(pool: List<TitleSummary>, shown: Set<String>, count: Int = 3, random: Random = Random.Default): List<TitleSummary> {
        val fresh = pool.distinctBy { it.key }.filter { it.key !in shown }
        return fresh.shuffled(random).take(count)
    }
}

/** 🎬 Franchises (TMDB "collections"). Ids verified live 2026-11-20. */
data class FranchiseRef(val id: Int, val name: String)

data class Franchise(val id: Int, val name: String, val overview: String, val posterPath: String?, val backdropPath: String?, val parts: List<TitleSummary>) {
    /** Release order; announced parts without a date go last. */
    val ordered: List<TitleSummary> get() = Franchises.releaseOrder(parts)
}

object Franchises {
    val POPULAR: List<FranchiseRef> = listOf(
        FranchiseRef(483464, "Golmaal"), FranchiseRef(44976, "Dhoom"), FranchiseRef(142015, "Housefull"),
        FranchiseRef(485645, "Tiger"), FranchiseRef(1639816, "Stree"), FranchiseRef(142022, "Hera Pheri"),
        FranchiseRef(1793488, "Singham"), FranchiseRef(256433, "Dabangg"), FranchiseRef(246091, "Krrish"),
        FranchiseRef(350309, "Baahubali"), FranchiseRef(657153, "K.G.F"), FranchiseRef(921781, "Pushpa"),
        FranchiseRef(977824, "Bhool Bhulaiyaa"), FranchiseRef(86311, "Avengers"), FranchiseRef(1241, "Harry Potter"),
        FranchiseRef(9485, "Fast & Furious"), FranchiseRef(87359, "Mission: Impossible"), FranchiseRef(645, "James Bond"),
        FranchiseRef(328, "Jurassic Park"), FranchiseRef(119, "The Lord of the Rings"),
    )

    fun releaseOrder(parts: List<TitleSummary>): List<TitleSummary> =
        parts.distinctBy { it.key }.sortedWith(compareBy<TitleSummary> { it.releaseDate == null }.thenBy { it.releaseDate })
}
