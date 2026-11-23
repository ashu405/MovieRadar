package com.movieradar.app.domain

/** A person in search results. */
data class PersonSummary(
    val id: Int,
    val name: String,
    val profilePath: String?,
    val department: String?,
    val knownFor: List<String>,
)

/** One acting credit, before filtering. */
data class Credit(val title: TitleSummary, val character: String?)

data class PersonDetails(
    val id: Int,
    val name: String,
    val biography: String?,
    val birthday: String?,
    val placeOfBirth: String?,
    val profilePath: String?,
    val department: String?,
    val movies: List<TitleSummary>,
    val series: List<TitleSummary>,
)

data class FilmographySplit(val movies: List<TitleSummary>, val series: List<TitleSummary>)

/** Turns raw TMDB credits into the actor page lists. Pure, unit-tested. */
object Filmography {
    /** TMDB TV genres that are appearances, not acting: News, Reality, Talk. */
    private val NON_ACTING_TV_GENRES = setOf(10763, 10764, 10767)
    private val SELF = Regex("""\b(self|himself|herself|themselves)\b""", RegexOption.IGNORE_CASE)

    private val TV_NON_ROLES = setOf("host", "co-host", "presenter", "guest", "judge", "contestant")

    /**
     * Appearances that aren't acting: playing themselves (incl. the role being their own name),
     * and on TV also hosting/judging, an empty role, or a News / Reality / Talk show.
     */
    fun isSelfAppearance(c: Credit, personName: String = ""): Boolean {
        val ch = c.character?.trim().orEmpty()
        if (ch.isNotEmpty() && SELF.containsMatchIn(ch)) return true
        if (personName.isNotBlank() && ch.equals(personName.trim(), ignoreCase = true)) return true
        if (c.title.type != MediaType.TV) return false
        return ch.isEmpty() || ch.lowercase() in TV_NON_ROLES || c.title.genreIds.any { it in NON_ACTING_TV_GENRES }
    }

    /**
     * Acting roles only, one entry per title, split into movies / series.
     * Undated titles (unreleased or unannounced) come first, then newest first.
     */
    fun build(credits: List<Credit>, personName: String = ""): FilmographySplit {
        val sorted = credits
            .filterNot { isSelfAppearance(it, personName) }
            .map { it.title }
            .distinctBy { it.key }
            .sortedWith(compareBy<TitleSummary> { it.releaseDate != null }.thenByDescending { it.releaseDate })
        return FilmographySplit(sorted.filter { it.type == MediaType.MOVIE }, sorted.filter { it.type == MediaType.TV })
    }
}
