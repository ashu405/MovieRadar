package com.movieradar.app

import com.movieradar.app.domain.OttLinks
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OttLinksTest {
    @Test fun `netflix opens netflix search with encoded title`() {
        val t = OttLinks.targetFor(8, "Jawan & Co", null)!!
        assertEquals("com.netflix.mediaclient", t.packageName)
        assertEquals("https://www.netflix.com/search?q=Jawan+%26+Co", t.searchUrl)
    }

    @Test fun `percent signs in titles cannot break the template`() {
        val t = OttLinks.targetFor(119, "100% Love", null)!!
        assertEquals("https://www.primevideo.com/search?phrase=100%25+Love", t.searchUrl)
    }

    @Test fun `unknown provider falls back to https tmdb link only`() {
        assertEquals("https://www.themoviedb.org/movie/1/watch", OttLinks.targetFor(99999, "X", "https://www.themoviedb.org/movie/1/watch")!!.searchUrl)
        assertNull(OttLinks.targetFor(99999, "X", "javascript:alert(1)"))
        assertNull(OttLinks.targetFor(99999, "X", null))
    }

    @Test fun `all search urls are https`() {
        listOf(8, 119, 2336, 232, 237, 309, 515, 192, 283, 11, 73, 350, 2, 3, 337, 1899).forEach {
            assertTrue(OttLinks.targetFor(it, "x", null)!!.searchUrl.startsWith("https://"))
        }
    }
}
