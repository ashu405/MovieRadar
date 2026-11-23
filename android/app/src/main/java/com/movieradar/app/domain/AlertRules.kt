package com.movieradar.app.domain

/** "Now streaming" alert rules. Pure, unit-tested. */
object AlertRules {
    /** Services that count as "streaming": subscription + free (incl. with ads). Rent/buy doesn't. */
    fun streamingProviders(w: WhereToWatch): List<Provider> = (w.stream + w.free).distinctBy { it.id }

    fun encode(ids: Collection<Int>): String = ids.distinct().sorted().joinToString(",")
    fun decode(s: String): Set<Int> = s.split(',').mapNotNull { it.trim().toIntOrNull() }.toSet()

    /** Providers in [now] that weren't known before, in the order TMDB lists them. */
    fun newProviders(known: Set<Int>, now: List<Provider>): List<Provider> = now.filter { it.id !in known }

    /** "Now streaming on Netflix" / "…on Netflix and Prime Video" / "…on Netflix, Prime Video and 1 more". */
    fun headline(newOnes: List<Provider>): String {
        val names = newOnes.map { it.name }
        val list = when (names.size) {
            0 -> ""
            1 -> names[0]
            2 -> "${names[0]} and ${names[1]}"
            else -> "${names[0]}, ${names[1]} and ${names.size - 2} more"
        }
        return "Now streaming on $list"
    }
}
