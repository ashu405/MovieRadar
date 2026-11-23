package com.movieradar.app.data.local

import com.movieradar.app.domain.MediaType
import com.movieradar.app.domain.TitleSummary
import com.movieradar.app.domain.key
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LibraryRepository @Inject constructor(private val dao: LibraryDao) {

    val watchlist: Flow<List<TitleSummary>> = dao.watchlist().map { rows ->
        rows.mapNotNull { r ->
            val type = MediaType.fromPath(r.type) ?: return@mapNotNull null
            TitleSummary(r.tmdbId, type, r.title, r.year, r.posterPath, null, "", 0.0, emptyList())
        }
    }

    /** ✅ Watched, newest first. */
    val watched: Flow<List<TitleSummary>> = dao.watched().map { rows ->
        rows.mapNotNull { r ->
            val type = MediaType.fromPath(r.type) ?: return@mapNotNull null
            TitleSummary(r.tmdbId, type, r.title, r.year, r.posterPath, null, "", 0.0, emptyList())
        }
    }

    /** Keys ("movie-1") of every watched title, for the ✓ poster badge. */
    val watchedKeys: Flow<Set<String>> = dao.watchedKeys().map { it.toSet() }

    fun isWatched(t: TitleSummary): Flow<Boolean> = dao.isWatched(t.key)

    /** Marking watched moves the title off the Watchlist; unmarking does not put it back. */
    suspend fun setWatched(t: TitleSummary, watched: Boolean) {
        if (watched) dao.markWatched(WatchedEntity(t.key, t.id, t.type.path, t.title, t.posterPath, t.year, System.currentTimeMillis()))
        else dao.removeWatched(t.key)
    }

    val continueWatching: Flow<List<ProgressEntity>> = dao.continueWatching()

    fun isSaved(t: TitleSummary): Flow<Boolean> = dao.isSaved(t.key)

    suspend fun setSaved(t: TitleSummary, saved: Boolean) {
        if (saved) {
            dao.save(WatchlistEntity(t.key, t.id, t.type.path, t.title, t.posterPath, t.year, System.currentTimeMillis()))
        } else {
            dao.remove(t.key)
        }
    }

    suspend fun resumePositionMs(identifier: String): Long = dao.progress(identifier)?.positionMs ?: 0L

    /**
     * Fire-and-forget on an app-wide scope, so a save issued while the player screen is closing
     * still completes. Finishing the film (≥ 95 %) removes it from Continue watching.
     */
    fun recordProgress(identifier: String, title: String, positionMs: Long, durationMs: Long) {
        appScope.launch {
            if (durationMs > 0 && positionMs >= durationMs * 0.95) dao.removeProgress(identifier)
            else dao.saveProgress(
                ProgressEntity(identifier, title, positionMs.coerceAtLeast(0), durationMs.coerceAtLeast(0), System.currentTimeMillis()),
            )
        }
    }

    suspend fun clearProgress(identifier: String) = dao.removeProgress(identifier)

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
}
