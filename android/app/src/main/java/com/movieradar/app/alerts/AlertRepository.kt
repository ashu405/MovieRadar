package com.movieradar.app.alerts

import com.movieradar.app.data.TitleRepository
import com.movieradar.app.data.local.AlertEntity
import com.movieradar.app.data.local.LibraryDao
import com.movieradar.app.domain.AlertRules
import com.movieradar.app.domain.MediaType
import com.movieradar.app.domain.Provider
import com.movieradar.app.domain.TitleSummary
import com.movieradar.app.domain.key
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/** One alert to show: [title] newly streaming on [providers]. */
data class NewArrival(val title: TitleSummary, val providers: List<Provider>)

@Singleton
class AlertRepository @Inject constructor(
    private val dao: LibraryDao,
    private val titles: TitleRepository,
) {
    val alerts: Flow<List<TitleSummary>> = dao.alerts().map { rows -> rows.mapNotNull { it.toSummary() } }

    fun hasAlert(t: TitleSummary): Flow<Boolean> = dao.hasAlert(t.key)

    /**
     * Turn 🔔 on: remember the services it's already on, so only later arrivals alert.
     * If the lookup fails, start with none known (the first check then reports what it finds).
     */
    suspend fun enable(t: TitleSummary, region: String) {
        val known = runCatching { AlertRules.streamingProviders(titles.whereToWatch(t.type, t.id, region)).map { it.id } }
            .getOrDefault(emptyList())
        dao.saveAlert(AlertEntity(t.key, t.id, t.type.path, t.title, t.posterPath, AlertRules.encode(known), System.currentTimeMillis()))
    }

    suspend fun disable(t: TitleSummary) = dao.removeAlert(t.key)

    /**
     * Daily check. Returns titles that gained a streaming service since last time and records the
     * new services as known (so each one alerts once). Titles whose lookup fails are retried next run.
     */
    suspend fun check(region: String): List<NewArrival> = dao.alertsOnce().mapNotNull { a ->
        val t = a.toSummary() ?: return@mapNotNull null
        val now = runCatching { AlertRules.streamingProviders(titles.whereToWatch(t.type, t.id, region)) }.getOrNull()
            ?: return@mapNotNull null
        val known = AlertRules.decode(a.knownProviderIds)
        val fresh = AlertRules.newProviders(known, now)
        if (fresh.isEmpty()) return@mapNotNull null
        dao.updateKnown(a.key, AlertRules.encode(known + fresh.map { it.id }))
        NewArrival(t, fresh)
    }

    private fun AlertEntity.toSummary(): TitleSummary? {
        val type = MediaType.fromPath(type) ?: return null
        return TitleSummary(tmdbId, type, title, null, posterPath, null, "", 0.0, emptyList())
    }
}
