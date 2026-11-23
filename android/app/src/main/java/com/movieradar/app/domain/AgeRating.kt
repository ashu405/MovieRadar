package com.movieradar.app.domain

/** App-wide age buckets used for badges and (Phase 3) filtering. */
enum class AgeBucket(val label: String) {
    ALL("All"), SEVEN("7+"), THIRTEEN("13+"), SIXTEEN("16+"), EIGHTEEN("18+");
}

object AgeRating {
    private val exact: Map<String, AgeBucket> = mapOf(
        // India (CBFC). Legacy "UA" was 12+ guidance → 13+.
        "U" to AgeBucket.ALL, "U/A 7+" to AgeBucket.SEVEN, "UA" to AgeBucket.THIRTEEN,
        "U/A" to AgeBucket.THIRTEEN, "U/A 13+" to AgeBucket.THIRTEEN, "U/A 16+" to AgeBucket.SIXTEEN,
        "A" to AgeBucket.EIGHTEEN, "S" to AgeBucket.EIGHTEEN,
        // US film + TV
        "G" to AgeBucket.ALL, "TV-Y" to AgeBucket.ALL, "TV-G" to AgeBucket.ALL,
        "PG" to AgeBucket.SEVEN, "TV-Y7" to AgeBucket.SEVEN, "TV-PG" to AgeBucket.SEVEN,
        "PG-13" to AgeBucket.THIRTEEN, "TV-14" to AgeBucket.THIRTEEN,
        "R" to AgeBucket.SIXTEEN, "NC-17" to AgeBucket.EIGHTEEN, "TV-MA" to AgeBucket.EIGHTEEN,
    )

    /** Maps a raw certification ("U/A 13+", "PG-13", "15", "FSK 12") to a bucket, or null if unknown. */
    fun bucketOf(cert: String?): AgeBucket? {
        val c = cert?.trim().orEmpty()
        if (c.isEmpty()) return null
        exact[c.uppercase()]?.let { return it }
        val age = Regex("\\d{1,2}").find(c)?.value?.toIntOrNull() ?: return null
        return when {
            age <= 6 -> AgeBucket.ALL
            age <= 9 -> AgeBucket.SEVEN
            age <= 14 -> AgeBucket.THIRTEEN
            age <= 17 -> AgeBucket.SIXTEEN
            else -> AgeBucket.EIGHTEEN
        }
    }

    /** TMDB `discover` filter for an age bucket: which country's ratings to use and the OR-joined labels. */
    data class CertFilter(val country: String, val certifications: String)

    /**
     * India and the US have explicit mappings (verified against TMDB data). Other regions use US
     * ratings, because TMDB's rating labels differ per country and aren't reliably ordered.
     * US TV has no 16+ label, so 16+ uses TV-MA (17+), the closest one.
     */
    fun discoverFilter(region: String, type: MediaType, bucket: AgeBucket): CertFilter {
        val country = if (region == "IN") "IN" else "US"
        val certs = when (country) {
            "IN" -> when (bucket) {
                AgeBucket.ALL -> "U"
                AgeBucket.SEVEN -> "U/A 7+"
                AgeBucket.THIRTEEN -> "U/A 13+|UA|U/A"
                AgeBucket.SIXTEEN -> "U/A 16+"
                AgeBucket.EIGHTEEN -> "A"
            }
            else -> when (type) {
                MediaType.MOVIE -> when (bucket) {
                    AgeBucket.ALL -> "G"
                    AgeBucket.SEVEN -> "PG"
                    AgeBucket.THIRTEEN -> "PG-13"
                    AgeBucket.SIXTEEN -> "R"
                    AgeBucket.EIGHTEEN -> "NC-17"
                }
                MediaType.TV -> when (bucket) {
                    AgeBucket.ALL -> "TV-Y|TV-G"
                    AgeBucket.SEVEN -> "TV-Y7|TV-PG"
                    AgeBucket.THIRTEEN -> "TV-14"
                    AgeBucket.SIXTEEN, AgeBucket.EIGHTEEN -> "TV-MA"
                }
            }
        }
        return CertFilter(country, certs)
    }

    /**
     * Picks the certification for [region] from (country → certifications) pairs,
     * falling back to US, then to any non-empty value.
     */
    fun pick(region: String, byCountry: List<Pair<String, String>>): String? {
        val nonEmpty = byCountry.filter { it.second.isNotBlank() }
        return nonEmpty.firstOrNull { it.first == region }?.second
            ?: nonEmpty.firstOrNull { it.first == "US" }?.second
            ?: nonEmpty.firstOrNull()?.second
    }
}
