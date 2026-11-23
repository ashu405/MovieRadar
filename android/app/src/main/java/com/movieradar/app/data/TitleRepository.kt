package com.movieradar.app.data

import com.movieradar.app.data.remote.TmdbApi
import com.movieradar.app.domain.Adult
import com.movieradar.app.domain.AgeBucket
import com.movieradar.app.domain.AgeRating
import com.movieradar.app.domain.Category
import com.movieradar.app.domain.ReleaseQueries
import com.movieradar.app.domain.ReleaseTab
import java.time.YearMonth
import com.movieradar.app.domain.Franchise
import com.movieradar.app.domain.FranchiseRef
import com.movieradar.app.domain.MediaType
import com.movieradar.app.domain.SurpriseRequest
import com.movieradar.app.domain.SurpriseRules
import com.movieradar.app.domain.OttReleases
import com.movieradar.app.domain.OttService
import com.movieradar.app.domain.key
import com.movieradar.app.domain.PersonDetails
import com.movieradar.app.domain.PersonSummary
import com.movieradar.app.domain.Provider
import com.movieradar.app.domain.Region
import com.movieradar.app.domain.Shelf
import com.movieradar.app.domain.TitleDetails
import com.movieradar.app.domain.TitleSummary
import com.movieradar.app.domain.WhereToWatch
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.sync.withLock
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TitleRepository @Inject constructor(private val api: TmdbApi) {

    private val genreMutex = Mutex()
    private var genreNames: Map<Int, String>? = null

    /** One search call returns titles and people (actors / actresses). */
    suspend fun search(query: String, includeAdult: Boolean = false): SearchResults {
        val results = api.searchMulti(query, includeAdult = includeAdult).results
        return SearchResults(
            titles = results.mapNotNull { it.toSummary() },
            people = results.mapNotNull { it.toPerson() },
        )
    }

    suspend fun person(id: Int): PersonDetails = api.person(id).toDetails()

    suspend fun details(type: MediaType, id: Int, region: String): TitleDetails {
        val ratings = if (type == MediaType.MOVIE) "release_dates" else "content_ratings"
        return api.details(type.path, id, "credits,videos,$ratings,watch/providers").toDetails(type, region)
    }

    suspend fun whereToWatch(type: MediaType, id: Int, region: String): WhereToWatch =
        api.watchProviders(type.path, id).results[region].toWhereToWatch()

    /** Exact "open this title" URLs per provider id (empty if the proxy can't find any). */
    suspend fun titleLinks(type: MediaType, id: Int, region: String): Map<Int, String> =
        api.titleLinks(type.path, id, region).links.mapNotNull { (k, v) -> k.toIntOrNull()?.let { it to v } }.toMap()

    suspend fun genreNames(): Map<Int, String> = genreMutex.withLock {
        genreNames ?: coroutineScope {
            val m = async { api.genres("movie").genres }
            val t = async { api.genres("tv").genres }
            (m.await() + t.await()).associate { it.id to it.name }
        }.also { genreNames = it }
    }

    suspend fun regions(): List<Region> =
        api.regions().results.map { Region(it.code, it.englishName) }.sortedBy { it.name }

    /** Main subscription services for a region, most relevant first (no stores/kids duplicates). */
    suspend fun topServices(region: String, limit: Int = 4): List<Provider> =
        api.regionProviders("movie", region).results
            .filterNot { p -> EXCLUDE.any { p.name.contains(it, ignoreCase = true) } }
            .sortedBy { it.displayPriorities[region] ?: it.displayPriority }
            .take(limit)
            .map { Provider(it.id, it.name, it.logoPath) }

    /**
     * A Trakt row turned into TMDB poster cards (Trakt has no images). Null if Trakt fails or is empty,
     * so Home can fall back to TMDB. Card lookups run 8 at a time and are cached by the proxy.
     */
    suspend fun traktShelf(list: TraktList, perType: Int = 10): Shelf? = runCatching {
        coroutineScope {
            val movies = async { api.trakt("movies", list.path, perType) }
            val shows = async { api.trakt("shows", list.path, perType) }
            val refs = TraktRows.merge(movies.await(), shows.await())
            val gate = Semaphore(8)
            val cards = refs.map { ref ->
                async {
                    gate.withPermit {
                        runCatching { api.basic(ref.type.path, ref.tmdbId).toSummary(ref.type) }.getOrNull()
                    }
                }
            }.mapNotNull { it.await() }
            cards.takeIf { it.isNotEmpty() }?.let { Shelf(list.title, it) }
        }
    }.getOrNull()

    suspend fun homeShelves(region: String, includeAdult: Boolean = false): HomeData = coroutineScope {
        // Real audience lists from Trakt (worldwide); each falls back to the TMDB row if Trakt fails.
        val traktTrending = async { traktShelf(TraktList.TRENDING) }
        val traktWatched = async { traktShelf(TraktList.MOST_WATCHED) }
        val traktAnticipated = async { traktShelf(TraktList.ANTICIPATED) }
        // 18+ rows only when the switch is on; a failure there must not break Home.
        val adultShelves = if (!includeAdult) emptyList() else Adult.homeRows().map { (title, params) ->
            async { Shelf(title, runCatching { api.discover("movie", params).results.mapNotNull { it.toSummary(MediaType.MOVIE) } }.getOrDefault(emptyList())) }
        }
        val trending = async { api.trending("all", "day").results.mapNotNull { it.toSummary() } }
        val trendingWeek = async { api.trending("all", "week").results.mapNotNull { it.toSummary() } }
        val since = LocalDate.now().minusDays(60).toString()
        val newOnStreaming = async {
            api.discover(
                "movie",
                mapOf(
                    "watch_region" to region,
                    "with_watch_monetization_types" to "flatrate",
                    "primary_release_date.gte" to since,
                    "sort_by" to "popularity.desc",
                ),
            ).results.mapNotNull { it.toSummary(MediaType.MOVIE) }
        }
        val services = topServices(region)
        val serviceShelves = services.map { p ->
            async {
                val params = mapOf(
                    "watch_region" to region,
                    "with_watch_providers" to p.id.toString(),
                    "sort_by" to "popularity.desc",
                )
                val movies = async { api.discover("movie", params).results.mapNotNull { it.toSummary(MediaType.MOVIE) } }
                val shows = async { api.discover("tv", params).results.mapNotNull { it.toSummary(MediaType.TV) } }
                Shelf("Top on ${p.name}", interleave(shows.await(), movies.await()))
            }
        }
        val t = trending.await()
        val trendingRow = traktTrending.await()
        HomeData(
            hero = (trendingRow?.items ?: t).firstOrNull { it.backdropPath != null } ?: t.firstOrNull { it.backdropPath != null },
            shelves = buildList {
                add(trendingRow ?: Shelf("Trending today", t))
                add(Shelf("New on streaming", newOnStreaming.await()))
                serviceShelves.forEach { add(it.await()) }
                add(traktWatched.await() ?: Shelf("Popular this week", trendingWeek.await()))
                traktAnticipated.await()?.let(::add)
                adultShelves.forEach { add(it.await()) }
            }.filter { it.items.isNotEmpty() },
        )
    }

    suspend fun genres(type: MediaType): List<Genre> =
        api.genres(type.path).genres.map { Genre(it.id, it.name) }

    /** One page of Browse results. */
    suspend fun browse(type: MediaType, region: String, filter: BrowseFilter, page: Int): BrowsePage {
        val dateField = if (type == MediaType.MOVIE) "primary_release_date" else "first_air_date"
        val params = buildMap {
            put("page", page.toString())
            put("include_adult", filter.includeAdult.toString())
            filter.genreId?.let { put("with_genres", it.toString()) }
            filter.category?.let { putAll(it.params(type, adult = filter.includeAdult)) }
            filter.age?.let { bucket ->
                val f = AgeRating.discoverFilter(region, type, bucket)
                put("certification_country", f.country)
                put("certification", f.certifications)
            }
            filter.providerId?.let {
                put("with_watch_providers", it.toString())
                put("watch_region", region)
            }
            when (filter.sort) {
                BrowseSort.POPULAR -> put("sort_by", "popularity.desc")
                BrowseSort.TOP_RATED -> { put("sort_by", "vote_average.desc"); put("vote_count.gte", "200") }
                BrowseSort.NEWEST -> { put("sort_by", "$dateField.desc"); put("$dateField.lte", LocalDate.now().toString()) }
            }
        }
        val res = api.discover(type.path, params)
        return BrowsePage(res.results.mapNotNull { it.toSummary(type) }, page < res.totalPages)
    }

    /**
     * Releases in [month] for an Upcoming tab (queries in [ReleaseQueries]). Pages run in parallel;
     * a single failed page is skipped, but if every page fails the error is shown.
     */
    suspend fun releases(month: YearMonth, region: String, tab: UpcomingTab): List<TitleSummary> = coroutineScope {
        val calls = ReleaseQueries.forTab(tab.query, month, region).flatMap { q ->
            (1..q.pages).map { page ->
                async {
                    runCatching {
                        api.discover(q.type.path, q.params + ("page" to page.toString())).results.mapNotNull { it.toSummary(q.type) }
                    }
                }
            }
        }
        val results = calls.map { it.await() }
        if (results.all { it.isFailure }) throw results.first().exceptionOrNull()!!
        results.flatMap { it.getOrDefault(emptyList()) }
    }

    /**
     * Upcoming → On OTT with each title tagged by service (big five):
     *  - the existing On OTT queries (untagged; these decide what "All" shows),
     *  - per service: new series on its networks, titles already streaming on it in [region],
     *  - for OTT movie premieres: the service named in TMDB's digital-release note.
     * Lookups run 6 at a time and are cached by the proxy; a failed lookup is skipped.
     */
    suspend fun ottReleases(month: YearMonth, region: String): OttReleases = coroutineScope {
        val base = async { releases(month, region, UpcomingTab.OTT) }
        val gate = Semaphore(6)
        fun window(type: MediaType): Map<String, String> {
            val field = if (type == MediaType.MOVIE) "primary_release_date" else "first_air_date"
            return mapOf(
                "$field.gte" to month.atDay(1).toString(), "$field.lte" to month.atEndOfMonth().toString(),
                "sort_by" to "popularity.desc", "include_adult" to "false",
            )
        }
        suspend fun pages(type: MediaType, params: Map<String, String>, count: Int = 2): List<TitleSummary> =
            (1..count).flatMap { p ->
                gate.withPermit {
                    runCatching { api.discover(type.path, params + ("page" to p.toString())).results.mapNotNull { it.toSummary(type) } }
                        .getOrDefault(emptyList())
                }
            }
        val tagged = OttService.entries.flatMap { s ->
            val streaming = mapOf("with_watch_providers" to s.providerId.toString(), "watch_region" to region)
            listOf(
                async { s to pages(MediaType.TV, window(MediaType.TV) + ("with_networks" to s.networkFilter)) },
                async { s to pages(MediaType.MOVIE, window(MediaType.MOVIE) + streaming, 1) },
                async { s to pages(MediaType.TV, window(MediaType.TV) + streaming, 1) },
            )
        }
        val baseTitles = base.await()
        val noteTags = baseTitles.filter { it.type == MediaType.MOVIE }.take(80).map { m ->
            async {
                m.key to gate.withPermit {
                    runCatching {
                        val notes = api.details("movie", m.id, "release_dates").releaseDates?.results.orEmpty()
                            .flatMap { c -> c.releaseDates.map { it.type to it.note } }
                        OttService.fromReleaseNotes(notes)
                    }.getOrDefault(emptySet())
                }
            }
        }.map { it.await() }.toMap()
        OttReleases.merge(baseTitles, tagged.map { it.await() }, noteTags)
    }

    /** Logos for the big five in [region] (from TMDB's provider list; cached 7 days). */
    suspend fun ottServiceLogos(region: String): Map<OttService, String?> = runCatching {
        val byId = api.regionProviders("movie", region).results.associate { it.id to it.logoPath }
        OttService.entries.associateWith { byId[it.providerId] }
    }.getOrDefault(emptyMap())

    /** 🎲 Surprise me: a pool of up to 5 pages per media type; the caller picks 3 at random. */
    suspend fun surprisePool(r: SurpriseRequest): List<TitleSummary> = coroutineScope {
        SurpriseRules.types(r.kind).flatMap { type ->
            (1..5).map { page ->
                async {
                    runCatching {
                        api.discover(type.path, SurpriseRules.params(r, type) + ("page" to page.toString()))
                            .results.mapNotNull { it.toSummary(type) }
                    }.getOrDefault(emptyList())
                }
            }
        }.flatMap { it.await() }
    }

    /** A franchise with its parts (release order applied by [Franchise.ordered]). */
    suspend fun franchise(id: Int): Franchise {
        val c = api.collection(id)
        return Franchise(
            c.id, c.name, c.overview.orEmpty(), c.posterPath, c.backdropPath,
            c.parts.mapNotNull { it.toSummary(MediaType.MOVIE) },
        )
    }

    suspend fun searchFranchises(query: String): List<FranchiseRef> =
        api.searchCollections(query).results.map { FranchiseRef(it.id, it.name) }

    private fun <T> interleave(a: List<T>, b: List<T>): List<T> = buildList {
        for (i in 0 until maxOf(a.size, b.size)) {
            a.getOrNull(i)?.let(::add); b.getOrNull(i)?.let(::add)
        }
    }

    companion object {
        private val EXCLUDE = listOf("Store", "JustWatch", "Kids", "Channel", "with Ads", "Amazon Video")
    }
}

data class SearchResults(val titles: List<TitleSummary>, val people: List<PersonSummary>)

data class HomeData(val hero: TitleSummary?, val shelves: List<Shelf>)

data class Genre(val id: Int, val name: String)
enum class BrowseSort(val label: String) { POPULAR("Popular"), TOP_RATED("Top rated"), NEWEST("Newest") }
data class BrowseFilter(
    val genreId: Int? = null,
    /** Extra category (rom-com, spy…); used instead of a genre. */
    val category: Category? = null,
    /** Settings → "Show 18+ titles". */
    val includeAdult: Boolean = false,
    val age: AgeBucket? = null,
    val providerId: Int? = null,
    val sort: BrowseSort = BrowseSort.POPULAR,
)
data class BrowsePage(val items: List<TitleSummary>, val hasMore: Boolean)
enum class UpcomingTab(val label: String, val query: ReleaseTab) {
    MOVIES("Movies", ReleaseTab.MOVIES), SERIES("Series", ReleaseTab.SERIES), OTT("On OTT", ReleaseTab.OTT)
}
