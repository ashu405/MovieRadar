package com.movieradar.app

import com.movieradar.app.domain.Franchises
import com.movieradar.app.domain.Length
import com.movieradar.app.domain.MediaType
import com.movieradar.app.domain.Mood
import com.movieradar.app.domain.OttService
import com.movieradar.app.domain.SurpriseRequest
import com.movieradar.app.domain.SurpriseRules
import com.movieradar.app.domain.TitleKind
import com.movieradar.app.domain.TitleSummary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class SurpriseAndFranchiseTest {
    private fun req(mood: Mood = Mood.THRILLER, length: Length = Length.ANY, services: List<Int> = listOf(8)) =
        SurpriseRequest(mood, TitleKind.EITHER, length, services, "IN")

    @Test fun `steamy mood only offered with 18+ on`() {
        assertFalse(Mood.STEAMY in Mood.available(adult = false))
        assertTrue(Mood.STEAMY in Mood.available(adult = true))
        assertEquals(8, Mood.available(adult = false).size)
    }

    @Test fun `picks are streamable now, well rated, region and service bound`() {
        val p = SurpriseRules.params(req(), MediaType.MOVIE)
        assertEquals("IN", p["watch_region"])
        assertEquals("8", p["with_watch_providers"])
        assertEquals("flatrate|free|ads", p["with_watch_monetization_types"])
        assertEquals("6.5", p["vote_average.gte"])
        assertEquals("false", p["include_adult"])
        assertEquals("53|80", p["with_genres"])
        assertEquals("80|9648", SurpriseRules.params(req(), MediaType.TV)["with_genres"])
    }

    @Test fun `length limits movies only`() {
        assertEquals("100", SurpriseRules.params(req(length = Length.SHORT), MediaType.MOVIE)["with_runtime.lte"])
        assertNull(SurpriseRules.params(req(length = Length.SHORT), MediaType.TV)["with_runtime.lte"])
    }

    @Test fun `steamy turns on adult titles and drops the vote floor`() {
        val p = SurpriseRules.params(req(mood = Mood.STEAMY), MediaType.MOVIE)
        assertEquals("true", p["include_adult"])
        assertNull(p["vote_count.gte"])
    }

    @Test fun `any of mine uses ticked services, or all five when none are ticked`() {
        assertEquals(listOf(8, 119), SurpriseRules.resolveServices(null, setOf(OttService.PRIME, OttService.NETFLIX)))
        assertEquals(OttService.entries.map { it.providerId }.sorted(), SurpriseRules.resolveServices(null, emptySet()))
        assertEquals(listOf(2336), SurpriseRules.resolveServices(OttService.JIOHOTSTAR, setOf(OttService.NETFLIX)))
    }

    private fun t(id: Int, date: String? = "2020-01-01") =
        TitleSummary(id, MediaType.MOVIE, "T$id", date?.take(4), null, null, "", 7.0, emptyList(), releaseDate = date)

    @Test fun `shuffle picks 3 and never repeats shown titles`() {
        val pool = (1..10).map { t(it) }
        val first = SurpriseRules.pick(pool, emptySet(), random = Random(1))
        assertEquals(3, first.size)
        val second = SurpriseRules.pick(pool, first.map { "movie-${it.id}" }.toSet(), random = Random(2))
        assertTrue(second.none { it in first })
        assertEquals(2, SurpriseRules.pick(pool.take(5), pool.take(3).map { "movie-${it.id}" }.toSet()).size)
    }

    @Test fun `franchise parts in release order, unannounced last, duplicates removed`() {
        val ordered = Franchises.releaseOrder(listOf(t(3, "2017-10-20"), t(9, null), t(1, "2006-07-14"), t(1, "2006-07-14"), t(2, "2010-11-05")))
        assertEquals(listOf(1, 2, 3, 9), ordered.map { it.id })
    }

    @Test fun `popular franchise ids are the verified ones`() {
        val byName = Franchises.POPULAR.associate { it.name to it.id }
        assertEquals(483464, byName["Golmaal"])
        assertEquals(485645, byName["Tiger"])
        assertEquals(1241, byName["Harry Potter"])
        assertEquals(20, Franchises.POPULAR.size)
    }
}
