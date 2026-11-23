package com.movieradar.app.domain

/**
 * The "big five" services for the Upcoming → On OTT filter. Ids verified live on TMDB (2026-11-20):
 * provider ids for India, TV network ids (JioHotstar originals moved from 3919 "Disney+ Hotstar" to 8036
 * "JioHotstar" in 2025), and the names used in TMDB's digital-release notes.
 */
enum class OttService(
    val label: String,
    val providerId: Int,
    val networkIds: List<Int>,
    private val noteAliases: List<String>,
) {
    NETFLIX("Netflix", 8, listOf(213), listOf("netflix")),
    PRIME("Prime Video", 119, listOf(1024), listOf("prime video", "amazon prime", "primevideo", "amazon video")),
    JIOHOTSTAR("JioHotstar", 2336, listOf(8036, 3919), listOf("jiohotstar", "jio hotstar", "hotstar", "jiocinema")),
    SONYLIV("SonyLIV", 237, listOf(2646), listOf("sonyliv", "sony liv")),
    ZEE5("ZEE5", 232, listOf(2590), listOf("zee5", "zee 5"));

    val networkFilter: String get() = networkIds.joinToString("|")

    companion object {
        /** Services named in any digital (type 4) release note, across countries. */
        fun fromReleaseNotes(notes: List<Pair<Int, String>>): Set<OttService> =
            notes.filter { it.first == 4 }.flatMap { fromNote(it.second) }.toSet()

        /** Services named in a release note, e.g. "Apple TV, Prime Video, Google Play" → {PRIME}. */
        fun fromNote(note: String?): Set<OttService> {
            val n = note?.lowercase()?.trim().orEmpty()
            if (n.isEmpty()) return emptySet()
            return entries.filter { s -> s.noteAliases.any { it in n } }.toSet()
        }
    }
}

/** On OTT results: titles plus which of the big five each one is coming to (by title key). */
data class OttReleases(val titles: List<TitleSummary>, val services: Map<String, Set<OttService>>) {
    fun servicesOf(t: TitleSummary): Set<OttService> = services[t.key].orEmpty()

    /** All titles, or only those tagged with [service]. */
    fun filter(service: OttService?): List<TitleSummary> =
        if (service == null) titles else titles.filter { service in servicesOf(it) }

    companion object {
        val EMPTY = OttReleases(emptyList(), emptyMap())

        /** Merge untagged titles with per-service tagged batches; tags from every source are combined. */
        fun merge(untagged: List<TitleSummary>, tagged: List<Pair<OttService, List<TitleSummary>>>, noteTags: Map<String, Set<OttService>>): OttReleases {
            val all = (untagged + tagged.flatMap { it.second }).distinctBy { it.key }
            val tags = HashMap<String, MutableSet<OttService>>()
            tagged.forEach { (s, list) -> list.forEach { tags.getOrPut(it.key) { mutableSetOf() }.add(s) } }
            noteTags.forEach { (k, v) -> if (v.isNotEmpty()) tags.getOrPut(k) { mutableSetOf() }.addAll(v) }
            return OttReleases(all, tags)
        }
    }
}
