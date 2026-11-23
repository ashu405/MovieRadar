package com.movieradar.app

import com.movieradar.app.data.TitleRef
import com.movieradar.app.data.TraktList
import com.movieradar.app.data.TraktRows
import com.movieradar.app.data.remote.DetailsDto
import com.movieradar.app.data.remote.TraktIdsDto
import com.movieradar.app.data.remote.TraktItemDto
import com.movieradar.app.data.remote.TraktMediaDto
import com.movieradar.app.data.toSummary
import com.movieradar.app.domain.MediaType
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TraktRowsTest {
    private fun movie(id: Int?) = TraktItemDto(movie = TraktMediaDto("M$id", 2026, TraktIdsDto(id)))
    private fun show(id: Int?) = TraktItemDto(show = TraktMediaDto("S$id", 2026, TraktIdsDto(id)))

    @Test fun `movies and series alternate, order kept, duplicates and missing tmdb ids dropped`() {
        val refs = TraktRows.merge(
            movies = listOf(movie(1368337), movie(null), movie(1263337), movie(1368337)),
            shows = listOf(show(84773)),
        )
        assertEquals(
            listOf(TitleRef(MediaType.MOVIE, 1368337), TitleRef(MediaType.TV, 84773), TitleRef(MediaType.MOVIE, 1263337)),
            refs,
        )
    }

    @Test fun `real trakt json parses, with extra fields ignored`() {
        val json = Json { ignoreUnknownKeys = true; explicitNulls = false; coerceInputValues = true }
        val body = """[{"watchers":1908,"movie":{"title":"The Odyssey","year":2026,"ids":{"trakt":1,"slug":"x","imdb":"tt1","tmdb":1368337}}}]"""
        val items = json.decodeFromString(ListSerializer(TraktItemDto.serializer()), body)
        assertEquals(listOf(TitleRef(MediaType.MOVIE, 1368337)), TraktRows.merge(items, emptyList()))
    }

    @Test fun `row titles`() {
        assertEquals("🔥 Trending now", TraktList.TRENDING.title)
        assertEquals("watched/weekly", TraktList.MOST_WATCHED.path)
    }

    @Test fun `details to card`() {
        val c = DetailsDto(id = 84773, name = "The Rings of Power", firstAirDate = "2022-09-01", posterPath = "/p.jpg")
            .toSummary(MediaType.TV)!!
        assertEquals("2022", c.year)
        assertEquals(MediaType.TV, c.type)
        assertNull(DetailsDto(id = 1).toSummary(MediaType.MOVIE))
    }
}
