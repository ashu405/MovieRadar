package com.movieradar.app.domain

import java.text.Normalizer
import kotlin.math.abs

/** An Internet Archive item that passed the legal-content filter. */
data class ArchiveItem(
    val identifier: String,
    val title: String,
    val year: Int?,
) {
    val thumbnailUrl: String get() = "https://archive.org/services/img/$identifier"
}

data class VideoSource(val label: String, val url: String, val height: Int?, val sizeBytes: Long?)
data class SubtitleTrack(val label: String, val url: String, val isVtt: Boolean)

data class PlayableItem(
    val item: ArchiveItem,
    val sources: List<VideoSource>,
    val subtitles: List<SubtitleTrack>,
) {
    /** Sensible default: best quality up to 720p, else the smallest available. */
    val defaultSource: VideoSource?
        get() = sources.filter { (it.height ?: 0) in 1..720 }.maxByOrNull { it.height ?: 0 } ?: sources.lastOrNull()
}

/** One file entry from `archive.org/metadata/{id}`. */
data class ArchiveFile(val name: String, val format: String?, val height: Int?, val sizeBytes: Long?)

/**
 * Rules for which Internet Archive content the app may play, and how TMDB titles are matched to it.
 * Pure functions so they are unit-tested.
 */
object ArchiveRules {
    /** Curated Internet Archive film collections of public-domain works. */
    val CURATED = setOf(
        "feature_films", "silent_films", "film_noir", "classic_tv", "classic_cartoons",
        "SciFi_Horror", "comedy_films", "Comedy_Films", "cartoons",
    )

    /**
     * Playable only if filed in one of Internet Archive's moderated public-domain collections
     * (and not still sitting in an "unsorted" bucket).
     *
     * Self-applied licenses are deliberately ignored: anyone can upload a commercial film to the
     * community bucket and label it CC0 (e.g. "Wallbanger 2024", a pirated copy marked public domain).
     */
    fun isLegal(collections: List<String>): Boolean {
        if (collections.any { it.contains("unsorted", ignoreCase = true) }) return false
        return collections.any { it in CURATED }
    }

    fun normalize(title: String): String {
        val noAccents = Normalizer.normalize(title, Normalizer.Form.NFD).replace(Regex("\\p{Mn}+"), "")
        return noAccents.lowercase()
            .replace("&", " and ")
            .replace(Regex("\\(\\s*\\d{4}\\s*\\)"), " ")
            .replace(Regex("[^a-z0-9 ]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
            .removePrefix("the ").removePrefix("a ").removePrefix("an ")
    }

    /**
     * True when an archive item is confidently the same film:
     * titles equal after normalizing (or the archive title extends a multi-word / long TMDB title,
     * e.g. "Nosferatu, eine Symphonie des Grauens"), and release years within one year.
     * If the archive has no year, its title must contain the TMDB year.
     */
    fun matches(tmdbTitle: String, tmdbYear: Int?, iaTitle: String, iaYear: Int?): Boolean {
        val a = normalize(tmdbTitle)
        val b = normalize(iaTitle)
        if (a.isEmpty() || b.isEmpty()) return false
        val longEnough = a.contains(' ') || a.length >= 6
        val titleOk = a == b || (longEnough && b.startsWith("$a "))
        if (!titleOk || tmdbYear == null) return false
        return if (iaYear != null) abs(iaYear - tmdbYear) <= 1 else iaTitle.contains(tmdbYear.toString())
    }

    private val VIDEO_EXT = listOf(".mp4", ".m4v")
    private val SUB_EXT = listOf(".srt", ".vtt")

    fun downloadUrl(identifier: String, fileName: String): String {
        val path = fileName.split('/').joinToString("/") { seg ->
            java.net.URLEncoder.encode(seg, "UTF-8").replace("+", "%20")
        }
        return "https://archive.org/download/${java.net.URLEncoder.encode(identifier, "UTF-8")}/$path"
    }

    /** Playable MP4 sources, best quality first, one per resolution. */
    fun videoSources(identifier: String, files: List<ArchiveFile>): List<VideoSource> =
        files.filter { f -> VIDEO_EXT.any { f.name.lowercase().endsWith(it) } }
            .sortedWith(compareByDescending<ArchiveFile> { it.height ?: 0 }.thenByDescending { it.sizeBytes ?: 0 })
            .distinctBy { it.height ?: it.name.hashCode() }
            .map { f ->
                VideoSource(
                    label = f.height?.let { "${it}p" } ?: f.format ?: "MP4",
                    url = downloadUrl(identifier, f.name),
                    height = f.height,
                    sizeBytes = f.sizeBytes,
                )
            }

    fun subtitles(identifier: String, files: List<ArchiveFile>): List<SubtitleTrack> =
        files.filter { f -> SUB_EXT.any { f.name.lowercase().endsWith(it) } }
            .map { f ->
                SubtitleTrack(
                    label = f.name.substringAfterLast('/').substringBeforeLast('.'),
                    url = downloadUrl(identifier, f.name),
                    isVtt = f.name.lowercase().endsWith(".vtt"),
                )
            }
}
