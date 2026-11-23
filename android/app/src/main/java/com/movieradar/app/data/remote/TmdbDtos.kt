package com.movieradar.app.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PageDto<T>(
    val page: Int = 1,
    val results: List<T> = emptyList(),
    @SerialName("total_pages") val totalPages: Int = 1,
)

@Serializable
data class MediaDto(
    val id: Int,
    @SerialName("media_type") val mediaType: String? = null,
    val title: String? = null,
    val name: String? = null,
    val overview: String? = null,
    @SerialName("poster_path") val posterPath: String? = null,
    @SerialName("backdrop_path") val backdropPath: String? = null,
    @SerialName("release_date") val releaseDate: String? = null,
    @SerialName("first_air_date") val firstAirDate: String? = null,
    @SerialName("vote_average") val voteAverage: Double = 0.0,
    @SerialName("genre_ids") val genreIds: List<Int> = emptyList(),
    @SerialName("original_language") val originalLanguage: String? = null,
    // Present when media_type == "person" (search/multi).
    @SerialName("profile_path") val profilePath: String? = null,
    @SerialName("known_for_department") val knownForDepartment: String? = null,
    @SerialName("known_for") val knownFor: List<KnownForDto> = emptyList(),
)

@Serializable data class KnownForDto(val title: String? = null, val name: String? = null)

@Serializable data class GenreDto(val id: Int, val name: String)
@Serializable data class GenreListDto(val genres: List<GenreDto> = emptyList())

@Serializable
data class DetailsDto(
    val id: Int,
    val title: String? = null,
    val name: String? = null,
    val tagline: String? = null,
    val overview: String? = null,
    @SerialName("poster_path") val posterPath: String? = null,
    @SerialName("backdrop_path") val backdropPath: String? = null,
    @SerialName("release_date") val releaseDate: String? = null,
    @SerialName("first_air_date") val firstAirDate: String? = null,
    @SerialName("vote_average") val voteAverage: Double = 0.0,
    val runtime: Int? = null,
    @SerialName("episode_run_time") val episodeRunTime: List<Int> = emptyList(),
    @SerialName("number_of_seasons") val numberOfSeasons: Int? = null,
    val genres: List<GenreDto> = emptyList(),
    val seasons: List<SeasonDto> = emptyList(),
    val credits: CreditsDto? = null,
    val videos: VideosDto? = null,
    @SerialName("release_dates") val releaseDates: ReleaseDatesDto? = null,
    @SerialName("content_ratings") val contentRatings: ContentRatingsDto? = null,
    @SerialName("watch/providers") val watchProviders: WatchProvidersDto? = null,
    @SerialName("belongs_to_collection") val belongsToCollection: CollectionRefDto? = null,
)

@Serializable
data class CollectionRefDto(
    val id: Int,
    val name: String = "",
    @SerialName("poster_path") val posterPath: String? = null,
)

@Serializable
data class CollectionDto(
    val id: Int,
    val name: String = "",
    val overview: String? = null,
    @SerialName("poster_path") val posterPath: String? = null,
    @SerialName("backdrop_path") val backdropPath: String? = null,
    val parts: List<MediaDto> = emptyList(),
)

@Serializable
data class SeasonDto(
    @SerialName("season_number") val seasonNumber: Int,
    val name: String = "",
    @SerialName("episode_count") val episodeCount: Int = 0,
    @SerialName("air_date") val airDate: String? = null,
    @SerialName("poster_path") val posterPath: String? = null,
)

@Serializable data class CreditsDto(val cast: List<CastDto> = emptyList())
@Serializable
data class CastDto(
    val id: Int = 0,
    val name: String,
    val character: String? = null,
    @SerialName("profile_path") val profilePath: String? = null,
)

@Serializable data class VideosDto(val results: List<VideoDto> = emptyList())
@Serializable
data class VideoDto(val key: String, val site: String, val type: String, val official: Boolean = false)

@Serializable data class ReleaseDatesDto(val results: List<CountryReleasesDto> = emptyList())
@Serializable
data class CountryReleasesDto(
    @SerialName("iso_3166_1") val country: String,
    @SerialName("release_dates") val releaseDates: List<ReleaseDateDto> = emptyList(),
)
@Serializable data class ReleaseDateDto(val certification: String = "", val type: Int = 0, val note: String = "")

@Serializable data class ContentRatingsDto(val results: List<ContentRatingDto> = emptyList())
@Serializable
data class ContentRatingDto(@SerialName("iso_3166_1") val country: String, val rating: String = "")

@Serializable data class WatchProvidersDto(val results: Map<String, RegionProvidersDto> = emptyMap())
@Serializable
data class RegionProvidersDto(
    val link: String? = null,
    val flatrate: List<ProviderDto> = emptyList(),
    val free: List<ProviderDto> = emptyList(),
    val ads: List<ProviderDto> = emptyList(),
    val rent: List<ProviderDto> = emptyList(),
    val buy: List<ProviderDto> = emptyList(),
)

@Serializable
data class ProviderDto(
    @SerialName("provider_id") val id: Int,
    @SerialName("provider_name") val name: String,
    @SerialName("logo_path") val logoPath: String? = null,
    @SerialName("display_priority") val displayPriority: Int = 999,
    @SerialName("display_priorities") val displayPriorities: Map<String, Int> = emptyMap(),
)

@Serializable data class RegionDto(
    @SerialName("iso_3166_1") val code: String,
    @SerialName("english_name") val englishName: String,
)

@Serializable data class TitleLinksDto(val links: Map<String, String> = emptyMap())

@Serializable
data class PersonDto(
    val id: Int,
    val name: String = "",
    val biography: String? = null,
    val birthday: String? = null,
    @SerialName("place_of_birth") val placeOfBirth: String? = null,
    @SerialName("profile_path") val profilePath: String? = null,
    @SerialName("known_for_department") val knownForDepartment: String? = null,
    @SerialName("combined_credits") val combinedCredits: CombinedCreditsDto? = null,
)

@Serializable data class CombinedCreditsDto(val cast: List<CreditDto> = emptyList())

@Serializable
data class CreditDto(
    val id: Int,
    @SerialName("media_type") val mediaType: String? = null,
    val title: String? = null,
    val name: String? = null,
    val character: String? = null,
    val overview: String? = null,
    @SerialName("poster_path") val posterPath: String? = null,
    @SerialName("backdrop_path") val backdropPath: String? = null,
    @SerialName("release_date") val releaseDate: String? = null,
    @SerialName("first_air_date") val firstAirDate: String? = null,
    @SerialName("vote_average") val voteAverage: Double = 0.0,
    @SerialName("genre_ids") val genreIds: List<Int> = emptyList(),
)

// --- Trakt (via proxy /trakt/...) ---
@Serializable data class TraktIdsDto(val tmdb: Int? = null)
@Serializable data class TraktMediaDto(val title: String? = null, val year: Int? = null, val ids: TraktIdsDto = TraktIdsDto())
@Serializable data class TraktItemDto(val movie: TraktMediaDto? = null, val show: TraktMediaDto? = null)
