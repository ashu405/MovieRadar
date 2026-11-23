package com.movieradar.app

import com.movieradar.app.domain.MediaType
import com.movieradar.app.domain.OttReleases
import com.movieradar.app.domain.OttService
import com.movieradar.app.domain.TitleSummary
import com.movieradar.app.domain.key
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OttServicesTest {

    @Test fun `release notes map to the big five, case and spelling variants included`() {
        assertEquals(setOf(OttService.NETFLIX), OttService.fromNote("NETFLIX"))
        assertEquals(setOf(OttService.PRIME), OttService.fromNote("Apple TV, Prime Video, Google Play, Fandango at Home"))
        assertEquals(setOf(OttService.JIOHOTSTAR), OttService.fromNote("Disney+ Hotstar"))
        assertEquals(setOf(OttService.JIOHOTSTAR), OttService.fromNote("JioHotstar"))
        assertEquals(setOf(OttService.SONYLIV), OttService.fromNote("Sony LIV"))
        assertEquals(setOf(OttService.ZEE5), OttService.fromNote("ZEE5"))
        assertTrue(OttService.fromNote("Disney+ / Hulu").isEmpty())
        assertTrue(OttService.fromNote("").isEmpty())
        assertTrue(OttService.fromNote(null).isEmpty())
    }

    @Test fun `only digital (type 4) release notes count`() {
        val notes = listOf(3 to "Netflix premiere screening", 4 to "Prime Video", 4 to "", 6 to "ZEE5")
        assertEquals(setOf(OttService.PRIME), OttService.fromReleaseNotes(notes))
    }

    @Test fun `network ids include both JioHotstar generations`() {
        assertEquals("8036|3919", OttService.JIOHOTSTAR.networkFilter)
        assertEquals("213", OttService.NETFLIX.networkFilter)
    }

    private fun t(id: Int, type: MediaType = MediaType.MOVIE) =
        TitleSummary(id, type, "T$id", "2026", null, null, "", 0.0, emptyList(), releaseDate = "2026-11-20")

    @Test fun `merge keeps every title, combines tags from all sources, and filters by service`() {
        val perfectLiars = t(1568060)
        val stranger = t(66732, MediaType.TV)
        val plain = t(5)
        val r = OttReleases.merge(
            untagged = listOf(perfectLiars, plain),
            tagged = listOf(
                OttService.NETFLIX to listOf(stranger),
                OttService.PRIME to listOf(perfectLiars),
            ),
            noteTags = mapOf(perfectLiars.key to setOf(OttService.PRIME, OttService.NETFLIX), plain.key to emptySet()),
        )
        assertEquals(listOf(1568060, 5, 66732), r.titles.map { it.id })
        assertEquals(setOf(OttService.PRIME, OttService.NETFLIX), r.servicesOf(perfectLiars))
        assertTrue(r.servicesOf(plain).isEmpty())
        assertEquals(listOf(1568060), r.filter(OttService.PRIME).map { it.id })
        assertEquals(listOf(1568060, 66732), r.filter(OttService.NETFLIX).map { it.id })
        assertEquals(3, r.filter(null).size)
    }
}
