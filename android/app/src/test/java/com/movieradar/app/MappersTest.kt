package com.movieradar.app

import com.movieradar.app.data.remote.DetailsDto
import com.movieradar.app.data.toDetails
import com.movieradar.app.data.toSummary
import com.movieradar.app.domain.AgeBucket
import com.movieradar.app.domain.MediaType
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MappersTest {
    private val json = Json { ignoreUnknownKeys = true; explicitNulls = false; coerceInputValues = true }

    // Trimmed real TMDB response shape (movie 872906 "Jawan", fetched 2026-11-18).
    private val movie = """
        {"id":872906,"title":"Jawan","overview":"o","poster_path":"/p.jpg","backdrop_path":"/b.jpg",
         "release_date":"2023-09-07","vote_average":7.1,"runtime":169,"genres":[{"id":28,"name":"Action"},{"id":53,"name":"Thriller"}],
         "credits":{"cast":[{"name":"Shah Rukh Khan","character":"Azad","profile_path":"/s.jpg"}]},
         "videos":{"results":[{"key":"tz","site":"YouTube","type":"Teaser","official":true},{"key":"tr","site":"YouTube","type":"Trailer","official":true}]},
         "release_dates":{"results":[{"iso_3166_1":"IN","release_dates":[{"certification":"","type":1},{"certification":"UA","type":3}]},
                                      {"iso_3166_1":"US","release_dates":[{"certification":"NR","type":3}]}]},
         "watch/providers":{"results":{"IN":{"link":"https://www.themoviedb.org/movie/872906-jvan/watch?locale=IN",
            "flatrate":[{"provider_id":8,"provider_name":"Netflix","logo_path":"/n.jpg","display_priority":1}],
            "rent":[{"provider_id":2,"provider_name":"Apple TV Store","display_priority":3},{"provider_id":192,"provider_name":"YouTube","display_priority":2}]}}}}
    """.trimIndent()

    @Test fun `movie details map genres, certification, trailer and providers`() {
        val d = json.decodeFromString<DetailsDto>(movie).toDetails(MediaType.MOVIE, "IN")
        assertEquals("Jawan", d.summary.title)
        assertEquals("2023", d.summary.year)
        assertEquals(listOf("Action", "Thriller"), d.genres)
        assertEquals("UA", d.certification)
        assertEquals(AgeBucket.THIRTEEN, d.ageBucket)
        assertEquals("tr", d.trailerYoutubeKey)
        assertEquals(listOf("Netflix"), d.whereToWatch.stream.map { it.name })
        assertEquals(listOf("YouTube", "Apple TV Store"), d.whereToWatch.rent.map { it.name })
        assertEquals(169, d.runtimeMinutes)
    }

    @Test fun `other region has no providers but still gets a fallback certification`() {
        val d = json.decodeFromString<DetailsDto>(movie).toDetails(MediaType.MOVIE, "GB")
        assertEquals(true, d.whereToWatch.isEmpty)
        assertEquals("NR", d.certification)
        assertNull(d.ageBucket)
    }

    @Test fun `search results without media type or title are dropped`() {
        val dto = json.decodeFromString<com.movieradar.app.data.remote.MediaDto>("""{"id":1,"media_type":"person","name":"X"}""")
        assertNull(dto.toSummary())
        val tv = json.decodeFromString<com.movieradar.app.data.remote.MediaDto>("""{"id":2,"media_type":"tv","name":"Panchayat","first_air_date":"2020-04-03"}""")
        assertEquals(MediaType.TV, tv.toSummary()!!.type)
        assertEquals("2020", tv.toSummary()!!.year)
    }
}
