package com.movieradar.app

import com.movieradar.app.domain.MediaType
import com.movieradar.app.domain.OttLinks
import com.movieradar.app.domain.ReleaseQueries
import com.movieradar.app.domain.ReleaseTab
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.YearMonth

class OttFixesTest {
    private val nov = YearMonth.of(2026, 11)

    // --- Upcoming → On OTT ---

    @Test fun `ott tab never depends only on streaming data, so future days can appear`() {
        val qs = ReleaseQueries.forTab(ReleaseTab.OTT, nov, "IN")
        val usDigital = qs.first { it.params["region"] == "US" }
        assertEquals(MediaType.MOVIE, usDigital.type)
        assertEquals("4", usDigital.params["with_release_type"])
        assertEquals("2026-11-01", usDigital.params["primary_release_date.gte"])
        assertEquals("2026-11-30", usDigital.params["release_date.lte"])
        assertFalse(usDigital.params.containsKey("with_watch_monetization_types"))
        assertEquals(5, usDigital.pages)

        assertTrue(qs.any { it.params["region"] == "IN" && it.params["with_release_type"] == "4" })
        val tv = qs.first { it.params.containsKey("with_networks") }
        assertEquals(MediaType.TV, tv.type)
        assertEquals(ReleaseQueries.STREAMING_NETWORKS, tv.params["with_networks"])
        assertEquals("2026-11-30", tv.params["first_air_date.lte"])
        // Recent arrivals already on a service in the region are still included.
        assertEquals(2, qs.count { it.params["with_watch_monetization_types"] == "flatrate" && it.params["watch_region"] == "IN" })
    }

    @Test fun `us region does not query the US twice`() {
        assertEquals(1, ReleaseQueries.forTab(ReleaseTab.OTT, nov, "US").count { it.params["region"] == "US" })
    }

    @Test fun `movies and series tabs are unchanged`() {
        val m = ReleaseQueries.forTab(ReleaseTab.MOVIES, nov, "IN").single()
        assertEquals(MediaType.MOVIE, m.type)
        assertNull(m.params["region"])
        val s = ReleaseQueries.forTab(ReleaseTab.SERIES, nov, "IN").single()
        assertEquals("2026-11-01", s.params["first_air_date.gte"])
    }

    // --- Exact "open in app" links ---

    @Test fun `netflix movies open the watch page so playback starts`() {
        val t = OttLinks.exactTarget(8, "https://www.netflix.com/title/81728124", isMovie = true)!!
        assertEquals("https://www.netflix.com/watch/81728124", t.searchUrl)
        assertEquals("com.netflix.mediaclient", t.packageName)
        assertTrue(t.isExact)
    }

    @Test fun `netflix series keep the title page`() {
        assertEquals(
            "https://www.netflix.com/title/80057281",
            OttLinks.exactTarget(8, "https://www.netflix.com/title/80057281", isMovie = false)!!.searchUrl,
        )
    }

    @Test fun `prime link opens in the prime app, unknown provider id falls back to host`() {
        val url = "https://app.primevideo.com/detail?gti=amzn1.dv.gti.2cb6867e"
        assertEquals("com.amazon.avod.thirdpartyclient", OttLinks.exactTarget(119, url, true)!!.packageName)
        assertEquals("com.amazon.avod.thirdpartyclient", OttLinks.exactTarget(2100, url, true)!!.packageName)
        assertNull(OttLinks.exactTarget(2, "https://tv.apple.com/in/movie/jawan/umc.cmc.x", true)!!.packageName)
    }

    @Test fun `non https links are refused`() {
        assertNull(OttLinks.exactTarget(8, "http://www.netflix.com/title/1", true))
        assertNull(OttLinks.exactTarget(8, "intent://evil", true))
    }
}
