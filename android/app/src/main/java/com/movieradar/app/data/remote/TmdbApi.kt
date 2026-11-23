package com.movieradar.app.data.remote

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.QueryMap

/** TMDB v3 endpoints, reached through the MovieRadar proxy (`/tmdb/...`). */
interface TmdbApi {
    @GET("tmdb/search/multi")
    suspend fun searchMulti(
        @Query("query") query: String,
        @Query("page") page: Int = 1,
        @Query("include_adult") includeAdult: Boolean = false,
        @Query("language") language: String = LANG,
    ): PageDto<MediaDto>

    @GET("tmdb/trending/{type}/{window}")
    suspend fun trending(
        @Path("type") type: String,
        @Path("window") window: String,
        @Query("language") language: String = LANG,
    ): PageDto<MediaDto>

    @GET("tmdb/discover/{type}")
    suspend fun discover(
        @Path("type") type: String,
        @QueryMap params: Map<String, String>,
    ): PageDto<MediaDto>

    @GET("tmdb/{type}/{id}")
    suspend fun details(
        @Path("type") type: String,
        @Path("id") id: Int,
        @Query("append_to_response") append: String,
        @Query("language") language: String = LANG,
    ): DetailsDto

    @GET("tmdb/{type}/{id}/watch/providers")
    suspend fun watchProviders(@Path("type") type: String, @Path("id") id: Int): WatchProvidersDto

    @GET("tmdb/genre/{type}/list")
    suspend fun genres(@Path("type") type: String, @Query("language") language: String = LANG): GenreListDto

    @GET("tmdb/watch/providers/{type}")
    suspend fun regionProviders(
        @Path("type") type: String,
        @Query("watch_region") region: String,
    ): PageDto<ProviderDto>

    /** Proxy-only endpoint: exact per-service title URLs keyed by TMDB provider id. */
    @GET("links/{type}/{id}")
    suspend fun titleLinks(@Path("type") type: String, @Path("id") id: Int, @Query("region") region: String): TitleLinksDto

    @GET("tmdb/person/{id}")
    suspend fun person(
        @Path("id") id: Int,
        @Query("append_to_response") append: String = "combined_credits",
        @Query("language") language: String = LANG,
    ): PersonDto

    /** Same as details, without extras: used to turn Trakt ids into poster cards. */
    @GET("tmdb/{type}/{id}")
    suspend fun basic(
        @Path("type") type: String,
        @Path("id") id: Int,
        @Query("language") language: String = LANG,
    ): DetailsDto

    /** Proxy-only: Trakt lists. [type] = movies|shows, [list] = trending | watched/weekly | anticipated. */
    @GET("trakt/{type}/{list}")
    suspend fun trakt(
        @Path("type") type: String,
        @Path("list", encoded = true) list: String,
        @Query("limit") limit: Int = 10,
    ): List<TraktItemDto>

    @GET("tmdb/collection/{id}")
    suspend fun collection(@Path("id") id: Int, @Query("language") language: String = LANG): CollectionDto

    @GET("tmdb/search/collection")
    suspend fun searchCollections(@Query("query") query: String, @Query("language") language: String = LANG): PageDto<CollectionRefDto>

    @GET("tmdb/watch/providers/regions")
    suspend fun regions(): PageDto<RegionDto>

    companion object {
        const val LANG = "en-US"
    }
}
