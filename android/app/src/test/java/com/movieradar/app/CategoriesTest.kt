package com.movieradar.app

import com.movieradar.app.data.BrowseFilter
import com.movieradar.app.domain.Adult
import com.movieradar.app.domain.Category
import com.movieradar.app.domain.MediaType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CategoriesTest {
    @Test fun `categories never switch on adult titles`() {
        Category.entries.forEach { c ->
            MediaType.entries.forEach { t -> assertFalse("$c $t", c.params(t).containsKey("include_adult")) }
        }
    }

    @Test fun `category queries match the verified TMDB ids`() {
        assertEquals("10749,35", Category.ROM_COM.params(MediaType.MOVIE)["with_genres"])
        assertEquals("9840", Category.ROM_COM.params(MediaType.TV)["with_keywords"])
        assertEquals("80,53", Category.CRIME_THRILLER.params(MediaType.MOVIE)["with_genres"])
        assertEquals("80,9648", Category.CRIME_THRILLER.params(MediaType.TV)["with_genres"])
        assertEquals("818", Category.BASED_ON_BOOK.params(MediaType.TV)["with_keywords"])
        assertEquals("470", Category.SPY.params(MediaType.MOVIE)["with_keywords"])
        val steamy = Category.STEAMY.params(MediaType.MOVIE)
        assertEquals("207767|364719|41260|278555|378681", steamy["with_keywords"])
        assertEquals("20", steamy["vote_count.gte"])
    }

    @Test fun `titles show the emoji`() {
        assertEquals("🔥 Steamy", Category.STEAMY.title)
    }

    @Test fun `steamy with the 18+ switch adds 18+ tags and drops the vote floor, other categories unchanged`() {
        val on = Category.STEAMY.params(MediaType.MOVIE, adult = true)
        assertEquals("${Category.STEAMY_TAGS}|155477|445|325693", on["with_keywords"])
        assertFalse(on.containsKey("vote_count.gte"))
        assertEquals("20", Category.STEAMY.params(MediaType.MOVIE, adult = false)["vote_count.gte"])
        assertEquals(Category.SPY.params(MediaType.MOVIE), Category.SPY.params(MediaType.MOVIE, adult = true))
    }

    @Test fun `18+ home rows are trending by popularity and most viewed by votes`() {
        val rows = Adult.homeRows()
        assertEquals(listOf("🔞 Trending 18+", "🔞 Most viewed 18+"), rows.map { it.first })
        assertEquals("popularity.desc", rows[0].second["sort_by"])
        assertEquals("vote_count.desc", rows[1].second["sort_by"])
        rows.forEach {
            assertEquals("true", it.second["include_adult"])
            assertTrue(it.second.getValue("with_keywords").startsWith(Adult.TAGS))
        }
    }

    @Test fun `browse is off by default`() {
        assertFalse(BrowseFilter().includeAdult)
    }
}
