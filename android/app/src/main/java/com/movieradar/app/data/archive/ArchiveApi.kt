package com.movieradar.app.data.archive

import kotlinx.serialization.json.JsonObject
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/** Internet Archive public APIs (no key). Responses are loosely typed, so they're parsed by hand. */
interface ArchiveApi {
    @GET("advancedsearch.php")
    suspend fun search(
        @Query("q") query: String,
        @Query("fl[]") fields: List<String> = listOf("identifier", "title", "year", "collection", "licenseurl"),
        @Query("sort[]") sort: String = "downloads desc",
        @Query("rows") rows: Int = 20,
        @Query("output") output: String = "json",
    ): JsonObject

    @GET("metadata/{id}")
    suspend fun metadata(@Path("id", encoded = false) identifier: String): JsonObject
}
