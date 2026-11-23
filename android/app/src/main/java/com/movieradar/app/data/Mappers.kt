package com.movieradar.app.data

import com.movieradar.app.data.remote.CreditDto
import com.movieradar.app.data.remote.DetailsDto
import com.movieradar.app.data.remote.PersonDto
import com.movieradar.app.domain.Credit
import com.movieradar.app.domain.Filmography
import com.movieradar.app.domain.FranchiseRef
import com.movieradar.app.domain.PersonDetails
import com.movieradar.app.domain.PersonSummary
import com.movieradar.app.data.remote.MediaDto
import com.movieradar.app.data.remote.ProviderDto
import com.movieradar.app.data.remote.RegionProvidersDto
import com.movieradar.app.domain.AgeRating
import com.movieradar.app.domain.CastMember
import com.movieradar.app.domain.MediaType
import com.movieradar.app.domain.Provider
import com.movieradar.app.domain.Season
import com.movieradar.app.domain.TitleDetails
import com.movieradar.app.domain.TitleSummary
import com.movieradar.app.domain.WhereToWatch

fun MediaDto.toSummary(fallbackType: MediaType? = null): TitleSummary? {
    val type = MediaType.fromPath(mediaType) ?: fallbackType ?: return null
    val t = title ?: name ?: return null
    return TitleSummary(
        id = id, type = type, title = t,
        year = (releaseDate ?: firstAirDate)?.take(4)?.takeIf { it.length == 4 },
        posterPath = posterPath, backdropPath = backdropPath,
        overview = overview.orEmpty(), rating = voteAverage, genreIds = genreIds,
        releaseDate = (releaseDate ?: firstAirDate)?.takeIf { it.length == 10 },
    )
}

private fun List<ProviderDto>.toProviders() =
    sortedBy { it.displayPriority }.distinctBy { it.id }.map { Provider(it.id, it.name, it.logoPath) }

fun RegionProvidersDto?.toWhereToWatch(): WhereToWatch {
    if (this == null) return WhereToWatch.EMPTY
    return WhereToWatch(
        stream = flatrate.toProviders(),
        free = (free + ads).toProviders(),
        rent = rent.toProviders(),
        buy = buy.toProviders(),
        tmdbLink = link,
    )
}

fun DetailsDto.toDetails(type: MediaType, region: String): TitleDetails {
    val certPairs = when (type) {
        MediaType.MOVIE -> releaseDates?.results.orEmpty().flatMap { c ->
            // Prefer theatrical (3) / digital (4) certifications, then any.
            c.releaseDates.sortedBy { if (it.type == 3 || it.type == 4) 0 else 1 }.map { c.country to it.certification }
        }
        MediaType.TV -> contentRatings?.results.orEmpty().map { it.country to it.rating }
    }
    val cert = AgeRating.pick(region, certPairs)
    val trailer = videos?.results.orEmpty()
        .filter { it.site == "YouTube" && (it.type == "Trailer" || it.type == "Teaser") }
        .sortedWith(compareBy({ it.type != "Trailer" }, { !it.official }))
        .firstOrNull()?.key
    return TitleDetails(
        summary = TitleSummary(
            id = id, type = type, title = title ?: name.orEmpty(),
            year = (releaseDate ?: firstAirDate)?.take(4)?.takeIf { it.length == 4 },
            posterPath = posterPath, backdropPath = backdropPath,
            overview = overview.orEmpty(), rating = voteAverage, genreIds = genres.map { it.id },
        ),
        tagline = tagline?.takeIf { it.isNotBlank() },
        genres = genres.map { it.name },
        runtimeMinutes = runtime ?: episodeRunTime.firstOrNull(),
        seasonCount = numberOfSeasons,
        seasons = seasons.filter { it.seasonNumber > 0 }.map { Season(it.seasonNumber, it.name, it.episodeCount, it.airDate) },
        certification = cert,
        ageBucket = AgeRating.bucketOf(cert),
        cast = credits?.cast.orEmpty().take(20).map { CastMember(it.name, it.character, it.profilePath, it.id) },
        trailerYoutubeKey = trailer,
        whereToWatch = watchProviders?.results?.get(region).toWhereToWatch(),
        franchise = belongsToCollection?.let { FranchiseRef(it.id, it.name) },
    )
}

fun MediaDto.toPerson(): PersonSummary? {
    if (mediaType != "person") return null
    val n = name ?: return null
    return PersonSummary(id, n, profilePath, knownForDepartment, knownFor.mapNotNull { it.title ?: it.name }.take(3))
}

fun CreditDto.toCredit(): Credit? {
    val type = MediaType.fromPath(mediaType) ?: return null
    val t = title ?: name ?: return null
    val date = (releaseDate ?: firstAirDate)?.takeIf { it.length == 10 }
    return Credit(
        TitleSummary(
            id = id, type = type, title = t, year = date?.take(4),
            posterPath = posterPath, backdropPath = backdropPath, overview = overview.orEmpty(),
            rating = voteAverage, genreIds = genreIds, releaseDate = date,
        ),
        character,
    )
}

fun PersonDto.toDetails(): PersonDetails {
    val split = Filmography.build(combinedCredits?.cast.orEmpty().mapNotNull { it.toCredit() }, name)
    return PersonDetails(
        id = id, name = name, biography = biography?.takeIf { it.isNotBlank() },
        birthday = birthday, placeOfBirth = placeOfBirth, profilePath = profilePath,
        department = knownForDepartment, movies = split.movies, series = split.series,
    )
}

/** Lightweight card from a plain details call (used for Trakt rows). */
fun DetailsDto.toSummary(type: MediaType): TitleSummary? {
    val t = title ?: name ?: return null
    val date = (releaseDate ?: firstAirDate)?.takeIf { it.length == 10 }
    return TitleSummary(
        id = id, type = type, title = t, year = date?.take(4),
        posterPath = posterPath, backdropPath = backdropPath, overview = overview.orEmpty(),
        rating = voteAverage, genreIds = genres.map { it.id }, releaseDate = date,
    )
}
