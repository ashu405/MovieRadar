package com.movieradar.app.domain

/**
 * Extra Browse categories built from TMDB genre combinations and keywords
 * (TMDB's own genre list has no rom-com, spy, etc.). Ids verified live, 2026-11-19.
 */
enum class Category(val label: String, val emoji: String) {
    ROM_COM("Rom-com", "💕"),
    CRIME_THRILLER("Crime thriller", "🔪"),
    BASED_ON_BOOK("Based on a book", "📖"),
    SPY("Spy", "🕵️"),
    STEAMY("Steamy", "🔥");

    val title: String get() = "$emoji $label"

    /**
     * `discover` params for this category. [adult] = Settings → "Show 18+ titles": only Steamy changes
     * (adds the 18+ tags and drops the vote floor, because adult titles have very few votes).
     * `include_adult` itself is set by the repository.
     */
    fun params(type: MediaType, adult: Boolean = false): Map<String, String> = when (this) {
        ROM_COM -> if (type == MediaType.MOVIE) mapOf("with_genres" to "10749,35")
        else mapOf("with_genres" to "35", "with_keywords" to "9840")
        CRIME_THRILLER -> mapOf("with_genres" to if (type == MediaType.MOVIE) "80,53" else "80,9648")
        BASED_ON_BOOK -> mapOf("with_keywords" to "818")
        SPY -> mapOf("with_keywords" to "470")
        // erotic thriller | erotic drama | sensuality | steamy | steamy romance; the vote floor drops obscure uploads.
        STEAMY -> if (adult) mapOf("with_keywords" to "$STEAMY_TAGS|${Adult.TAGS}")
        else mapOf("with_keywords" to STEAMY_TAGS, "vote_count.gte" to "20")
    }

    companion object {
        /** erotic thriller | erotic drama | sensuality | steamy | steamy romance */
        const val STEAMY_TAGS = "207767|364719|41260|278555|378681"
    }
}

/**
 * 18+ content, only used when Settings → "Show 18+ titles" is on. Verified live 2026-11-19:
 * TMDB ranks these titles last by popularity, so they need their own queries to be seen at all.
 */
object Adult {
    /** softcore (5,480 movies) | pornography (660) | erotica (32) */
    const val TAGS = "155477|445|325693"

    /**
     * Home rows. TMDB's trending list never includes adult titles, so "Trending" = current popularity
     * and "Most viewed" = most votes, within the 18+ and Steamy tags.
     */
    fun homeRows(): List<Pair<String, Map<String, String>>> = listOf(
        "🔞 Trending 18+" to rowParams("popularity.desc"),
        "🔞 Most viewed 18+" to rowParams("vote_count.desc"),
    )

    private fun rowParams(sort: String) = mapOf(
        "with_keywords" to "$TAGS|${Category.STEAMY_TAGS}",
        "include_adult" to "true",
        "sort_by" to sort,
    )
}
