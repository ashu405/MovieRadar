package com.movieradar.app

import com.movieradar.app.domain.ArchiveFile
import com.movieradar.app.domain.ArchiveItem
import com.movieradar.app.domain.ArchiveRules
import com.movieradar.app.domain.PlayableItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ArchiveRulesTest {

    // --- Legal filter (real values seen on archive.org) ---

    @Test fun `curated public-domain collection is playable without a license url`() {
        assertTrue(ArchiveRules.isLegal(listOf("silent_films", "SciFi_Horror", "feature_films", "moviesandfilms")))
    }

    @Test fun `community uploads are not playable even when self-labelled public domain (Wallbanger 2024)`() {
        // archive.org/details/wallbanger-2024-67208: a pirated 2024 film in opensource_movies, marked CC0 by the uploader.
        assertFalse(ArchiveRules.isLegal(listOf("opensource_movies")))
        assertFalse(ArchiveRules.isLegal(listOf("opensource_movies", "fav-supermanga")))
        assertFalse(ArchiveRules.isLegal(emptyList()))
    }

    @Test fun `moderated collections stay playable`() {
        assertTrue(ArchiveRules.isLegal(listOf("feature_films")))
        assertTrue(ArchiveRules.isLegal(listOf("film_noir", "opensource_movies")))
    }

    @Test fun `unsorted uploads are not playable even if tagged feature_films`() {
        assertFalse(ArchiveRules.isLegal(listOf("feature_films_unsorted", "feature_films")))
    }

    // --- Title matching ---

    @Test fun `normalize strips accents punctuation year and leading article`() {
        assertEquals("cabinet of dr caligari", ArchiveRules.normalize("The Cabinet of Dr. Caligari (1920)"))
        assertEquals("amelie", ArchiveRules.normalize("Amélie"))
        assertEquals("romeo and juliet", ArchiveRules.normalize("Romeo & Juliet"))
    }

    @Test fun `same title and year matches`() {
        assertTrue(ArchiveRules.matches("Nosferatu", 1922, "Nosferatu (1922)", 1922))
        assertTrue(ArchiveRules.matches("Night of the Living Dead", 1968, "Night of the Living Dead", 1968))
    }

    @Test fun `year within one is accepted, further apart is rejected`() {
        assertTrue(ArchiveRules.matches("His Girl Friday", 1940, "His Girl Friday", 1939))
        assertFalse(ArchiveRules.matches("Nosferatu", 2024, "Nosferatu", 1922))
    }

    @Test fun `archive title may extend a long tmdb title`() {
        assertTrue(
            ArchiveRules.matches("Nosferatu", 1922, "Nosferatu, eine Symphonie des Grauens (A Symphony of Horror)", 1922),
        )
    }

    @Test fun `short titles must match exactly`() {
        assertFalse(ArchiveRules.matches("It", 1927, "It Happened One Night", 1927))
        assertTrue(ArchiveRules.matches("It", 1927, "It", 1927))
    }

    @Test fun `missing archive year needs the year in its title`() {
        assertTrue(ArchiveRules.matches("Nosferatu", 1922, "Nosferatu 1922 restored", null))
        assertFalse(ArchiveRules.matches("Nosferatu", 1922, "Nosferatu", null))
    }

    @Test fun `no tmdb year never matches`() {
        assertFalse(ArchiveRules.matches("Nosferatu", null, "Nosferatu", 1922))
    }

    // --- Files → player sources ---

    private val files = listOf(
        ArchiveFile("Film_512kb.mp4", "512Kb MPEG4", 240, 395_416_378),
        ArchiveFile("Film.ogv", "Ogg Video", 304, 401_195_646),
        ArchiveFile("Film HD.mp4", "h.264 HD", 720, 1_500_000_000),
        ArchiveFile("Film.mp4", "h.264", 480, 800_000_000),
        ArchiveFile("Film_dup.mp4", "MPEG4", 480, 700_000_000),
        ArchiveFile("subs/Film.en.srt", "SubRip", null, 50_000),
        ArchiveFile("Film.thumbs/x.jpg", "Thumbnail", null, 800),
    )

    @Test fun `only mp4 sources, best first, one per resolution`() {
        val s = ArchiveRules.videoSources("Film_1922", files)
        assertEquals(listOf("720p", "480p", "240p"), s.map { it.label })
        assertTrue(s[1].url.endsWith("/Film.mp4")) // larger of the two 480p files
    }

    @Test fun `download url encodes spaces and keeps folders`() {
        val s = ArchiveRules.videoSources("Film_1922", files)
        assertEquals("https://archive.org/download/Film_1922/Film%20HD.mp4", s[0].url)
        val subs = ArchiveRules.subtitles("Film_1922", files)
        assertEquals("https://archive.org/download/Film_1922/subs/Film.en.srt", subs.single().url)
        assertFalse(subs.single().isVtt)
    }

    @Test fun `default quality is the best up to 720p`() {
        val item = ArchiveItem("Film_1922", "Film", 1922)
        val p = PlayableItem(item, ArchiveRules.videoSources("Film_1922", files), emptyList())
        assertEquals("720p", p.defaultSource?.label)
        val only1080 = PlayableItem(item, ArchiveRules.videoSources("x", listOf(ArchiveFile("a.mp4", null, 1080, 1))), emptyList())
        assertEquals("1080p", only1080.defaultSource?.label)
    }
}
