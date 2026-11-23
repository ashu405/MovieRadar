package com.movieradar.app

import com.movieradar.app.data.remote.CreditDto
import com.movieradar.app.data.remote.KnownForDto
import com.movieradar.app.data.remote.MediaDto
import com.movieradar.app.data.toCredit
import com.movieradar.app.data.toPerson
import com.movieradar.app.data.toSummary
import com.movieradar.app.domain.Credit
import com.movieradar.app.domain.Filmography
import com.movieradar.app.domain.MediaType
import com.movieradar.app.domain.TitleSummary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PeopleTest {

    private fun credit(id: Int, type: MediaType, date: String?, character: String? = "Rahul", genres: List<Int> = emptyList()) =
        Credit(TitleSummary(id, type, "T$id", date?.take(4), null, null, "", 0.0, genres, releaseDate = date), character)

    @Test fun `self appearances and talk or reality shows are hidden`() {
        assertTrue(Filmography.isSelfAppearance(credit(1, MediaType.TV, "2016-04-23", "Self - Guest")))
        assertTrue(Filmography.isSelfAppearance(credit(2, MediaType.MOVIE, "2010-01-01", "Himself")))
        assertTrue(Filmography.isSelfAppearance(credit(3, MediaType.TV, "2004-11-19", null, genres = listOf(10767))))
        assertTrue(Filmography.isSelfAppearance(credit(4, MediaType.TV, "2000-07-03", "", genres = listOf(10764))))
        assertFalse(Filmography.isSelfAppearance(credit(5, MediaType.MOVIE, "1995-10-20", "Raj Malhotra")))
        assertFalse(Filmography.isSelfAppearance(credit(6, MediaType.TV, "2020-01-01", "Selfie Guy")))
    }

    @Test fun `tv hosting, empty roles and playing your own name are hidden, real tv roles kept`() {
        val srk = "Shah Rukh Khan"
        assertTrue(Filmography.isSelfAppearance(credit(7, MediaType.TV, "1998-01-01", "Host"), srk))       // Zee Cine Awards
        assertTrue(Filmography.isSelfAppearance(credit(8, MediaType.TV, "2022-01-01", ""), srk))           // Umang
        assertTrue(Filmography.isSelfAppearance(credit(9, MediaType.TV, "2025-01-01", "Shah Rukh Khan"), srk))
        assertFalse(Filmography.isSelfAppearance(credit(10, MediaType.TV, "1989-01-01", "Lt. Abhimanyu Rai", listOf(10759, 18)), srk)) // Fauji
        assertFalse(Filmography.isSelfAppearance(credit(11, MediaType.MOVIE, "2023-01-01", ""), srk))      // movies with missing role data stay
    }

    @Test fun `filmography is split, de-duplicated, undated first then newest first`() {
        val f = Filmography.build(
            listOf(
                credit(10, MediaType.MOVIE, "1995-10-20"),
                credit(11, MediaType.MOVIE, "2023-09-07"),
                credit(11, MediaType.MOVIE, "2023-09-07", "Vikram"), // double role, same film
                credit(12, MediaType.MOVIE, null),
                credit(13, MediaType.MOVIE, "2010-02-12", "Himself"),
                credit(20, MediaType.TV, "2016-04-23", "Self", genres = listOf(10767)),
                credit(21, MediaType.TV, "1989-01-01", "Abhimanyu"),
            ),
        )
        assertEquals(listOf(12, 11, 10), f.movies.map { it.id })
        assertEquals(listOf(21), f.series.map { it.id })
    }

    @Test fun `search multi people are parsed and not treated as titles`() {
        val dto = MediaDto(
            id = 35742, mediaType = "person", name = "Shah Rukh Khan", profilePath = "/p.jpg",
            knownForDepartment = "Acting",
            knownFor = listOf(KnownForDto(title = "Dilwale Dulhania Le Jayenge"), KnownForDto(name = "Circus"), KnownForDto()),
        )
        val p = dto.toPerson()!!
        assertEquals("Shah Rukh Khan", p.name)
        assertEquals(listOf("Dilwale Dulhania Le Jayenge", "Circus"), p.knownFor)
        assertNull(dto.toSummary())
        assertNull(MediaDto(id = 1, mediaType = "movie", title = "Jawan").toPerson())
    }

    @Test fun `credit keeps the full date for sorting`() {
        val c = CreditDto(id = 872906, mediaType = "movie", title = "Jawan", character = "Azad", releaseDate = "2023-09-07")
            .toCredit()!!
        assertEquals("2023-09-07", c.title.releaseDate)
        assertEquals("2023", c.title.year)
        assertNull(CreditDto(id = 1, mediaType = "person", name = "x").toCredit())
    }
}
