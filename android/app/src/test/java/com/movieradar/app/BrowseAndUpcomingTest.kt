package com.movieradar.app

import com.movieradar.app.domain.AgeBucket
import com.movieradar.app.domain.AgeRating
import com.movieradar.app.domain.MediaType
import com.movieradar.app.domain.TitleSummary
import com.movieradar.app.domain.UpcomingRules
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class BrowseAndUpcomingTest {

    @Test fun `india age filter uses CBFC labels including legacy UA`() {
        val f = AgeRating.discoverFilter("IN", MediaType.MOVIE, AgeBucket.THIRTEEN)
        assertEquals("IN", f.country)
        assertEquals("U/A 13+|UA|U/A", f.certifications)
        assertEquals("A", AgeRating.discoverFilter("IN", MediaType.TV, AgeBucket.EIGHTEEN).certifications)
    }

    @Test fun `other regions fall back to US labels per media type`() {
        assertEquals(AgeRating.CertFilter("US", "PG-13"), AgeRating.discoverFilter("GB", MediaType.MOVIE, AgeBucket.THIRTEEN))
        assertEquals(AgeRating.CertFilter("US", "TV-14"), AgeRating.discoverFilter("US", MediaType.TV, AgeBucket.THIRTEEN))
        assertEquals("TV-MA", AgeRating.discoverFilter("US", MediaType.TV, AgeBucket.SIXTEEN).certifications)
    }

    @Test fun `every bucket maps back to itself`() {
        AgeBucket.entries.forEach { b ->
            AgeRating.discoverFilter("IN", MediaType.MOVIE, b).certifications.split('|').forEach { c ->
                assertEquals("IN $c", b, AgeRating.bucketOf(c))
            }
            AgeRating.discoverFilter("US", MediaType.MOVIE, b).certifications.split('|').forEach { c ->
                assertEquals("US $c", b, AgeRating.bucketOf(c))
            }
        }
    }

    private fun t(id: Int, date: String?, type: MediaType = MediaType.MOVIE) =
        TitleSummary(id, type, "T$id", date?.take(4), null, null, "", 0.0, emptyList(), releaseDate = date)

    @Test fun `upcoming groups by day, sorted, inside the month only, without duplicates`() {
        val month = YearMonth.of(2026, 11)
        val days = UpcomingRules.groupByDay(
            month,
            listOf(t(1, "2026-11-20"), t(2, "2026-11-03"), t(3, "2026-12-01"), t(1, "2026-11-20"), t(4, null), t(5, "2026-11-20", MediaType.TV), t(6, "bad")),
        )
        assertEquals(listOf(LocalDate.of(2026, 11, 3), LocalDate.of(2026, 11, 20)), days.map { it.date })
        assertEquals(listOf(1, 5), days[1].titles.map { it.id })
    }
}
