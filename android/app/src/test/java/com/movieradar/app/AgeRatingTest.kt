package com.movieradar.app

import com.movieradar.app.domain.AgeBucket
import com.movieradar.app.domain.AgeRating
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AgeRatingTest {
    @Test fun `india CBFC ratings map to app buckets`() {
        assertEquals(AgeBucket.ALL, AgeRating.bucketOf("U"))
        assertEquals(AgeBucket.SEVEN, AgeRating.bucketOf("U/A 7+"))
        assertEquals(AgeBucket.THIRTEEN, AgeRating.bucketOf("UA"))
        assertEquals(AgeBucket.THIRTEEN, AgeRating.bucketOf("U/A 13+"))
        assertEquals(AgeBucket.SIXTEEN, AgeRating.bucketOf("U/A 16+"))
        assertEquals(AgeBucket.EIGHTEEN, AgeRating.bucketOf("A"))
    }

    @Test fun `us ratings map to app buckets`() {
        assertEquals(AgeBucket.ALL, AgeRating.bucketOf("G"))
        assertEquals(AgeBucket.SEVEN, AgeRating.bucketOf("PG"))
        assertEquals(AgeBucket.THIRTEEN, AgeRating.bucketOf("PG-13"))
        assertEquals(AgeBucket.THIRTEEN, AgeRating.bucketOf("TV-14"))
        assertEquals(AgeBucket.EIGHTEEN, AgeRating.bucketOf("TV-MA"))
    }

    @Test fun `numeric ratings from other countries`() {
        assertEquals(AgeBucket.ALL, AgeRating.bucketOf("0"))
        assertEquals(AgeBucket.THIRTEEN, AgeRating.bucketOf("12"))
        assertEquals(AgeBucket.SIXTEEN, AgeRating.bucketOf("FSK 16"))
        assertEquals(AgeBucket.EIGHTEEN, AgeRating.bucketOf("18"))
    }

    @Test fun `blank or unknown is null`() {
        assertNull(AgeRating.bucketOf(""))
        assertNull(AgeRating.bucketOf(null))
        assertNull(AgeRating.bucketOf("NR"))
    }

    @Test fun `pick prefers region then US then anything`() {
        val certs = listOf("US" to "PG-13", "IN" to "", "IN" to "UA", "GB" to "12A")
        assertEquals("UA", AgeRating.pick("IN", certs))
        assertEquals("PG-13", AgeRating.pick("FR", certs))
        assertEquals("12A", AgeRating.pick("FR", listOf("GB" to "12A")))
        assertNull(AgeRating.pick("IN", listOf("IN" to "")))
    }
}
