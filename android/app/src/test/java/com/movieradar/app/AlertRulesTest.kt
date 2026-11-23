package com.movieradar.app

import com.movieradar.app.alerts.AlertWorker
import com.movieradar.app.domain.AlertRules
import com.movieradar.app.domain.Provider
import com.movieradar.app.domain.WhereToWatch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime

class AlertRulesTest {
    private val netflix = Provider(8, "Netflix", null)
    private val prime = Provider(119, "Prime Video", null)
    private val mx = Provider(515, "MX Player", null)
    private val apple = Provider(2, "Apple TV", null)

    @Test fun `streaming means subscription or free, never rent or buy`() {
        val w = WhereToWatch(stream = listOf(netflix), free = listOf(mx), rent = listOf(apple), buy = listOf(apple), tmdbLink = null)
        assertEquals(listOf(8, 515), AlertRules.streamingProviders(w).map { it.id })
        assertTrue(AlertRules.streamingProviders(WhereToWatch.EMPTY).isEmpty())
    }

    @Test fun `only services not already known are new`() {
        assertEquals(listOf(prime), AlertRules.newProviders(setOf(8), listOf(netflix, prime)))
        assertTrue(AlertRules.newProviders(setOf(8, 119), listOf(netflix, prime)).isEmpty())
        assertEquals(listOf(netflix), AlertRules.newProviders(emptySet(), listOf(netflix)))
    }

    @Test fun `known ids round-trip through storage`() {
        assertEquals("8,119,515", AlertRules.encode(listOf(515, 8, 119, 8)))
        assertEquals(setOf(8, 119, 515), AlertRules.decode("8,119,515"))
        assertTrue(AlertRules.decode("").isEmpty())
        assertEquals(setOf(8), AlertRules.decode("8, x,"))
    }

    @Test fun `notification headline`() {
        assertEquals("Now streaming on Netflix", AlertRules.headline(listOf(netflix)))
        assertEquals("Now streaming on Netflix and Prime Video", AlertRules.headline(listOf(netflix, prime)))
        assertEquals("Now streaming on Netflix, Prime Video and 1 more", AlertRules.headline(listOf(netflix, prime, mx)))
    }

    @Test fun `daily check is scheduled for the next 9 AM`() {
        val nine = LocalTime.of(9, 0)
        assertEquals(Duration.ofHours(2), AlertWorker.delayUntilNext(nine, LocalDateTime.of(2026, 11, 20, 7, 0)))
        assertEquals(Duration.ofHours(23), AlertWorker.delayUntilNext(nine, LocalDateTime.of(2026, 11, 20, 10, 0)))
        assertEquals(Duration.ofHours(24), AlertWorker.delayUntilNext(nine, LocalDateTime.of(2026, 11, 20, 9, 0)))
    }
}
