package com.movieradar.app.data.archive

import com.movieradar.app.domain.ArchiveFile
import com.movieradar.app.domain.ArchiveItem
import com.movieradar.app.domain.ArchiveRules
import com.movieradar.app.domain.PlayableItem
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ArchiveRepository @Inject constructor(private val api: ArchiveApi) {

    private val cacheMutex = Mutex()
    private val matchCache = HashMap<String, ArchiveItem?>()
    private var classicsCache: List<ArchiveItem>? = null

    /** A legal Internet Archive copy of a TMDB movie, or null if there's no confident match. */
    suspend fun findFor(title: String, year: Int?): ArchiveItem? {
        if (year == null || title.isBlank()) return null
        val cacheKey = "$title|$year"
        cacheMutex.withLock { if (matchCache.containsKey(cacheKey)) return matchCache[cacheKey] }
        val words = ArchiveRules.normalize(title).split(' ').filter { it.length > 1 && it !in QUERY_STOP_WORDS }.take(6)
        if (words.isEmpty()) return null
        val q = "mediatype:movies AND title:(${words.joinToString(" AND ")}) AND $LEGAL_CLAUSE"
        val match = parseDocs(api.search(q, rows = 25))
            .firstOrNull { ArchiveRules.matches(title, year, it.title, it.year) }
        cacheMutex.withLock { matchCache[cacheKey] = match }
        return match
    }

    /** Most-watched public-domain feature films, for the Home row. */
    suspend fun classics(): List<ArchiveItem> = cacheMutex.withLock { classicsCache } ?: run {
        val q = "mediatype:movies AND collection:(feature_films OR silent_films OR film_noir) " +
            "AND -collection:feature_films_unsorted AND year:[1900 TO 1975]"
        parseDocs(api.search(q, rows = 30)).also { list -> cacheMutex.withLock { classicsCache = list } }
    }

    /** Files and qualities for the player. Re-checks the legal filter on the full metadata. */
    suspend fun playable(identifier: String): PlayableItem {
        val root = api.metadata(identifier)
        val meta = root["metadata"] as? JsonObject ?: error("Item not found")
        val collections = meta["collection"].strings()
        require(ArchiveRules.isLegal(collections, meta["licenseurl"].firstString())) {
            "This item isn't marked public domain, so it can't be played in the app."
        }
        val files = (root["files"] as? JsonArray).orEmpty().mapNotNull { el ->
            val o = el as? JsonObject ?: return@mapNotNull null
            ArchiveFile(
                name = o["name"].firstString() ?: return@mapNotNull null,
                format = o["format"].firstString(),
                height = o["height"].firstString()?.toIntOrNull()?.takeIf { it > 0 },
                sizeBytes = o["size"].firstString()?.toLongOrNull(),
            )
        }
        val item = ArchiveItem(
            identifier = identifier,
            title = meta["title"].firstString() ?: identifier,
            year = meta["year"].firstString()?.take(4)?.toIntOrNull()
                ?: meta["date"].firstString()?.take(4)?.toIntOrNull(),
        )
        return PlayableItem(item, ArchiveRules.videoSources(identifier, files), ArchiveRules.subtitles(identifier, files))
    }

    private fun parseDocs(root: JsonObject): List<ArchiveItem> {
        val docs = root["response"]?.jsonObject?.get("docs")?.jsonArray.orEmpty()
        return docs.mapNotNull { el ->
            val d = el as? JsonObject ?: return@mapNotNull null
            if (!ArchiveRules.isLegal(d["collection"].strings(), d["licenseurl"].firstString())) return@mapNotNull null
            ArchiveItem(
                identifier = d["identifier"].firstString() ?: return@mapNotNull null,
                title = d["title"].firstString() ?: return@mapNotNull null,
                year = d["year"].firstString()?.take(4)?.toIntOrNull(),
            )
        }
    }

    private fun JsonElement?.strings(): List<String> = when (this) {
        is JsonArray -> mapNotNull { (it as? JsonPrimitive)?.contentOrNull }
        is JsonPrimitive -> listOfNotNull(contentOrNull)
        else -> emptyList()
    }

    private fun JsonElement?.firstString(): String? = strings().firstOrNull()?.takeIf { it.isNotBlank() }

    companion object {
        private val QUERY_STOP_WORDS = setOf("and", "or", "not", "to")
        private val LEGAL_CLAUSE =
            "(collection:(${ArchiveRules.CURATED.joinToString(" OR ")}) OR licenseurl:*publicdomain* OR licenseurl:*creativecommons*)"
    }
}
